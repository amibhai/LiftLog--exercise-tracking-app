package io.github.amibhai.liftlog.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.LibrarySource
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.RoutineSummary
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.domain.progression.PlannedSession
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.search.SearchIndex
import io.github.amibhai.liftlog.core.domain.workout.NOW
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import io.github.amibhai.liftlog.core.domain.workout.exercise
import io.github.amibhai.liftlog.core.domain.workout.info
import io.github.amibhai.liftlog.core.domain.workout.logged
import io.github.amibhai.liftlog.core.domain.workout.set
import io.github.amibhai.liftlog.core.domain.workout.values
import io.github.amibhai.liftlog.core.domain.workout.workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class UseCasesTest {

    private var next = 0
    private val ids = IdGenerator { "id${next++}" }
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    private val workouts = InMemoryWorkouts()
    private val routines = InMemoryRoutines()
    private val history = FixedHistory(
        mapOf("bench" to ExerciseSession(WorkoutId("old"), NOW - 1, listOf(logged(80.0, 8)))),
    )
    private val library = FixedLibrary(listOf(info("bench"), info("row"), info("curl")))

    private val progress = FixedProgress()
    private val advisor = ProgressionAdvisor(progress, FixedSettings())
    private val start = StartWorkout(workouts, routines, history, advisor, WorkoutFactory(ids), clock)
    private val add = AddExercises(workouts, routines, library, history, ids, clock)

    @Test
    fun `starting creates one workout and then keeps returning it`() = runTest {
        val first = start.empty()
        val second = start.empty()

        assertThat(second).isEqualTo(first)
        assertThat(workouts.active()!!.startedAt).isEqualTo(NOW)
    }

    @Test
    fun `a routine starts with the numbers of last time`() = runTest {
        routines.create(
            Routine(
                RoutineId("r"),
                "Push",
                exercises = listOf(RoutineExercise(RoutineExerciseId("s"), info("bench"), targetSets = 1)),
            ),
        )

        start.fromRoutine(RoutineId("r"))

        assertThat(workouts.active()!!.name).isEqualTo("Push")
        assertThat(workouts.active()!!.exercises.single().sets.single().target).isEqualTo(values(80.0, 8))
        assertThat(start.fromRoutine(RoutineId("missing"))).isNull()
    }

    @Test
    fun `a routine starts with what its progression suggests`() = runTest {
        val slot = RoutineExercise(RoutineExerciseId("s"), info("bench"), targetSets = 1, repMin = 5, repMax = 8)
        routines.create(Routine(RoutineId("r"), "Push", exercises = listOf(slot)))
        progress.sessions = mapOf(
            ExerciseId("bench") to listOf(PlannedSession(NOW - 1, listOf(logged(80.0, 8)), slot = slot.id)),
        )

        start.fromRoutine(RoutineId("r"))

        assertThat(workouts.active()!!.exercises.single().sets.single().target).isEqualTo(values(82.5, 5))
    }

    @Test
    fun `an earlier workout can be repeated`() = runTest {
        val old = workout(exercise("bench", set("1", target = values(70.0, 8), done = true)))
            .copy(id = WorkoutId("old"), isInProgress = false, endedAt = NOW)
        workouts.create(old)

        val id = start.repeat(WorkoutId("old"))

        assertThat(id).isNotEqualTo(WorkoutId("old"))
        assertThat(workouts.active()!!.exercises.single().sets.single().target).isEqualTo(values(80.0, 8))
        assertThat(start.repeat(WorkoutId("missing"))).isNull()
    }

    @Test
    fun `picked exercises are added in the order they were picked, pre-filled`() = runTest {
        val id = start.empty()

        add.toWorkout(id, listOf(ExerciseId("row"), ExerciseId("bench")), asSuperset = true)

        val added = workouts.active()!!.exercises
        assertThat(added.map { it.exercise.name }).containsExactly("row", "bench").inOrder()
        assertThat(added.map { it.supersetGroup }).containsExactly(1, 1)
        assertThat(added.last().sets.single().target).isEqualTo(values(80.0, 8))
    }

    @Test
    fun `a single exercise is no superset`() = runTest {
        val id = start.empty()

        add.toWorkout(id, listOf(ExerciseId("row")), asSuperset = true)

        assertThat(workouts.active()!!.exercises.single().supersetGroup).isNull()
    }

    @Test
    fun `replacing swaps the exercise in place`() = runTest {
        val id = start.empty()
        add.toWorkout(id, listOf(ExerciseId("row"), ExerciseId("bench")), asSuperset = false)
        val slot = workouts.active()!!.exercises.first().id

        add.replaceInWorkout(id, slot, ExerciseId("curl"))
        add.replaceInWorkout(id, WorkoutExerciseId("missing"), ExerciseId("curl"))

        assertThat(workouts.active()!!.exercises.map { it.exercise.name }).containsExactly("curl", "bench").inOrder()
    }

    @Test
    fun `routines get exercises with default targets`() = runTest {
        routines.create(Routine(RoutineId("r"), "Push"))

        add.toRoutine(RoutineId("r"), listOf(ExerciseId("bench"), ExerciseId("row")), asSuperset = false)
        add.replaceInRoutine(RoutineId("r"), routines.get(RoutineId("r"))!!.exercises.last().id, ExerciseId("curl"))

        val exercises = routines.get(RoutineId("r"))!!.exercises
        assertThat(exercises.map { it.exercise.name }).containsExactly("bench", "curl").inOrder()
        assertThat(exercises.map { it.targetSets }).containsExactly(3, 3)
        assertThat(exercises.map { it.repMin to it.repMax }).containsExactly(8 to 12, 8 to 12)
    }

    @Test
    fun `close spellings are only added when there are few matches`() = runTest {
        val search = SearchExercises(library)
        val index = SearchIndex(library.catalog)
        library.matches = setOf(ExerciseId("bench"))

        assertThat(search("bench", index, emptyMap()).map { it.name }).containsExactly("bench")

        library.matches = emptySet()
        assertThat(search("bnech", index, emptyMap()).map { it.name }).containsExactly("bench")
    }

    private class InMemoryWorkouts : WorkoutRepository {
        private val all = MutableStateFlow(emptyMap<WorkoutId, Workout>())

        suspend fun active() = observeActive().first()

        override fun observeActive(): Flow<Workout?> = all.map { it.values.firstOrNull(Workout::isInProgress) }

        override fun observe(id: WorkoutId): Flow<Workout?> = all.map { it[id] }

        override fun observeFinished(limit: Int): Flow<List<Workout>> = all.map {
            it.values.filterNot(
                Workout::isInProgress,
            )
        }

        override fun observeFinishedCount(): Flow<Int> = observeFinished(Int.MAX_VALUE).map { it.size }

        override suspend fun create(workout: Workout) {
            all.value += workout.id to workout
        }

        override suspend fun edit(id: WorkoutId, change: (Workout) -> Workout): Workout? =
            all.value[id]?.let(change)?.also { create(it) }

        override suspend fun delete(id: WorkoutId) {
            all.value -= id
        }

        override suspend fun restore(id: WorkoutId) = Unit
    }

    private class InMemoryRoutines : RoutineRepository {
        private val all = MutableStateFlow(emptyMap<RoutineId, Routine>())

        override fun observeAll(): Flow<List<RoutineSummary>> = all.map { map ->
            map.values.map {
                RoutineSummary(
                    it,
                    null,
                )
            }
        }

        override fun observe(id: RoutineId): Flow<Routine?> = all.map { it[id] }

        override suspend fun get(id: RoutineId): Routine? = all.value[id]

        override suspend fun create(routine: Routine) {
            all.value += routine.id to routine
        }

        override suspend fun edit(id: RoutineId, change: (Routine) -> Routine): Routine? =
            all.value[id]?.let(change)?.also { create(it) }

        override suspend fun delete(id: RoutineId) {
            all.value -= id
        }

        override suspend fun restore(id: RoutineId) = Unit

        override suspend fun reorder(ids: List<RoutineId>) = Unit
    }

    private class FixedProgress : ProgressRepository {
        var sessions: Map<ExerciseId, List<PlannedSession>> = emptyMap()

        override suspend fun plannedSessions(
            before: Long,
            ids: Set<ExerciseId>,
        ): Map<ExerciseId, List<PlannedSession>> = sessions.filterKeys { it in ids }

        override fun observeSessions(id: ExerciseId): Flow<List<ExerciseSession>> = flowOf(emptyList())

        override suspend fun sessionsBefore(
            before: Long,
            ids: Set<ExerciseId>,
        ): Map<ExerciseId, List<ExerciseSession>> = emptyMap()

        override fun observeWorkoutStats(): Flow<List<WorkoutStats>> = flowOf(emptyList())

        override fun observeMuscleWork(from: Long, to: Long): Flow<List<MuscleWork>> = flowOf(emptyList())

        override fun observeMuscleWork(workout: WorkoutId): Flow<List<MuscleWork>> = flowOf(emptyList())

        override fun observeRecentRecords(limit: Int): Flow<List<RecordEntry>> = flowOf(emptyList())

        override fun observeRecordsOf(workout: WorkoutId): Flow<List<RecordEntry>> = flowOf(emptyList())

        override suspend fun rebuildRecords() = Unit
    }

    private class FixedSettings : SettingsRepository {
        override val settings: Flow<UserSettings> = flowOf(UserSettings())

        override suspend fun update(transform: (UserSettings) -> UserSettings) = Unit
    }

    private class FixedHistory(sessions: Map<String, ExerciseSession>) : HistoryRepository {
        private val latest = sessions.mapKeys { ExerciseId(it.key) }

        override suspend fun usages(since: Long): List<ExerciseUsage> = emptyList()

        override suspend fun latestSessions(before: Long): Map<ExerciseId, ExerciseSession> = latest

        override suspend fun latestSessions(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, ExerciseSession> =
            latest.filterKeys { it in ids }

        override fun observeSessions(id: ExerciseId, limit: Int): Flow<List<ExerciseSession>> = flowOf(
            listOfNotNull(latest[id]),
        )
    }

    private class FixedLibrary(private val all: List<ExerciseInfo>) : ExerciseRepository {
        var matches: Set<ExerciseId> = emptySet()

        val catalog = all.map {
            CatalogExercise(
                it.id,
                it.name,
                type = ExerciseType.WEIGHT_REPS,
                equipment = Equipment.BARBELL,
            )
        }

        override fun observeStats(): Flow<LibraryStats> = flowOf(LibraryStats(all.size, 0))

        override fun observeCatalog(): Flow<List<CatalogExercise>> = flowOf(catalog)

        override suspend fun matching(query: String): Set<ExerciseId> = matches

        override fun observeDetail(id: ExerciseId): Flow<ExerciseDetail?> = flowOf(null)

        override suspend fun infos(ids: List<ExerciseId>): List<ExerciseInfo> =
            ids.mapNotNull { id -> all.firstOrNull { it.id == id } }

        override fun observeSources(): Flow<List<LibrarySource>> = flowOf(emptyList())
    }
}
