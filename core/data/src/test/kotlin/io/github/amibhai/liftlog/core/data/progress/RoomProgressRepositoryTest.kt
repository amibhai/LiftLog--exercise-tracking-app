package io.github.amibhai.liftlog.core.data.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.data.NOW
import io.github.amibhai.liftlog.core.data.TestClock
import io.github.amibhai.liftlog.core.data.TestDatabase
import io.github.amibhai.liftlog.core.data.routine.RoomRoutineRepository
import io.github.amibhai.liftlog.core.data.workout.RoomWorkoutRepository
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.ProgressionRuleType
import io.github.amibhai.liftlog.core.domain.model.RecordType
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.progress.E1rmFormula
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.progression.SessionPlan
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomProgressRepositoryTest {

    @get:Rule
    val db = TestDatabase()

    private val clock = TestClock()
    private val settings = FakeSettingsRepository()
    private val bench = ExerciseId("bench")
    private val info = ExerciseInfo(bench, "Bench Press", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
    private val first = workout("w1", at = NOW, set("a", 60.0, 5), set("b", 60.0, 5))
    private val second = workout("w2", at = NOW + DAY, set("c", 62.5, 5))

    @Test
    fun everyImprovementIsARecord() = runTest {
        start()

        val records = progress().observeRecentRecords(10).first()

        assertThat(records.map { it.record.type }).containsExactly(
            RecordType.HEAVIEST_WEIGHT,
            RecordType.BEST_E1RM,
            RecordType.BEST_SET_VOLUME,
        )
        val heaviest = records.single { it.record.type == RecordType.HEAVIEST_WEIGHT }
        assertThat(heaviest.exerciseName).isEqualTo("Bench Press")
        assertThat(heaviest.record.value).isEqualTo(62.5)
        assertThat(heaviest.record.workoutId).isEqualTo(WorkoutId("w2"))
        assertThat(heaviest.record.setId).isEqualTo(WorkoutSetId("c"))
        assertThat(progress().observeRecordsOf(WorkoutId("w1")).first()).isEmpty()
    }

    @Test
    fun sessionsKnowTheRoutineSlotThatPlannedThem() = runTest {
        start()
        val slot = RoutineExercise(RoutineExerciseId("slot"), info, repMin = 5, repMax = 8)
        RoomRoutineRepository(db.database, db.database.routineDao(), db.database.historyDao(), clock)
            .create(Routine(RoutineId("r"), "Push", exercises = listOf(slot)))
        workouts().edit(WorkoutId("w2")) { workout ->
            workout.copy(exercises = workout.exercises.map { it.copy(routineExerciseId = slot.id) })
        }

        val sessions = progress().plannedSessions(before = NOW + 2 * DAY, ids = setOf(bench)).getValue(bench)

        assertThat(sessions.map { it.at }).containsExactly(NOW, NOW + DAY).inOrder()
        assertThat(sessions.first().slot).isNull()
        assertThat(sessions.first().plan).isNull()
        assertThat(sessions.last().slot).isEqualTo(slot.id)
        assertThat(sessions.last().plan).isEqualTo(SessionPlan(5, 8, ProgressionRuleType.DOUBLE_PROGRESSION))
        assertThat(progress().plannedSessions(before = NOW + DAY, ids = setOf(bench)).getValue(bench)).hasSize(1)
    }

    @Test
    fun loadedExercisesComeWithTheirRecentSessions() = runTest {
        start()
        db.addExercise("plank", type = ExerciseType.DURATION)

        val histories = progress().observeLoadedExercises(since = NOW).first()
        assertThat(histories.single().name).isEqualTo("Bench Press")
        assertThat(histories.single().sessions.map { it.at }).containsExactly(NOW, NOW + DAY).inOrder()
        assertThat(progress().observeLoadedExercises(since = NOW + 1).first().single().sessions).hasSize(1)
    }

    @Test
    fun changingAFinishedWorkoutChangesItsRecords() = runTest {
        start()

        workouts().edit(WorkoutId("w2")) { workout ->
            workout.copy(
                exercises = workout.exercises.map { exercise ->
                    exercise.copy(sets = listOf(set("c", 60.0, 5)))
                },
            )
        }

        assertThat(progress().observeRecentRecords(10).first()).isEmpty()
    }

    @Test
    fun deletingAWorkoutTakesItsRecordsAndUndoBringsThemBack() = runTest {
        start()

        workouts().delete(WorkoutId("w2"))
        assertThat(progress().observeRecentRecords(10).first()).isEmpty()

        workouts().restore(WorkoutId("w2"))
        assertThat(progress().observeRecentRecords(10).first()).hasSize(3)
    }

    @Test
    fun theFormulaChangesTheRecordsItGives() = runTest {
        settings.update { it.copy(e1rmFormula = E1rmFormula.BRZYCKI) }
        start()

        val e1rm = progress().observeRecentRecords(10).first().single { it.record.type == RecordType.BEST_E1RM }
        assertThat(e1rm.record.value).isWithin(1e-9).of(62.5 * 36 / (37 - 5))
    }

    @Test
    fun importedWorkoutsAreAddedOnceAndCountForRecords() = runTest {
        db.addExercise("bench", name = "Bench Press")

        assertThat(workouts().addFinished(listOf(first, second))).isEqualTo(2)
        assertThat(progress().observeRecentRecords(10).first()).hasSize(3)

        workouts().delete(WorkoutId("w1"))
        assertThat(workouts().addFinished(listOf(first, second))).isEqualTo(0)
        assertThat(workouts().observeFinishedCount().first()).isEqualTo(1)
    }

    @Test
    fun statsSumUpEveryFinishedWorkout() = runTest {
        start()
        val warm = set("w", 20.0, 10).copy(type = SetType.WARMUP)
        workouts().create(workout("w3", NOW + 2 * DAY, warm, set("d", 100.0, 5)))
        workouts().create(Workout(WorkoutId("active"), startedAt = NOW + 3 * DAY))

        val stats = progress().observeWorkoutStats().first()

        assertThat(stats.map { it.id.value }).containsExactly("w1", "w2", "w3").inOrder()
        assertThat(stats.map { it.volumeKg }).containsExactly(600.0, 312.5, 500.0).inOrder()
        assertThat(stats.map { it.sets }).containsExactly(2, 1, 2).inOrder()
        assertThat(stats.first().seconds).isEqualTo(3_600)
    }

    @Test
    fun muscleWorkCountsWorkingSetsForTheirMuscles() = runTest {
        start()
        db.database.exerciseDao().upsertMuscles(
            listOf(muscle(Muscle.CHEST, isPrimary = true), muscle(Muscle.TRICEPS, isPrimary = false)),
        )

        val work = progress().observeMuscleWork(from = NOW, to = NOW + 2 * DAY).first().single()

        assertThat(work).isEqualTo(MuscleWork(bench, listOf(Muscle.CHEST), listOf(Muscle.TRICEPS), sets = 3))
        assertThat(progress().observeMuscleWork(WorkoutId("w2")).first().single().sets).isEqualTo(1)
        assertThat(progress().observeMuscleWork(from = NOW + 2 * DAY, to = NOW + 3 * DAY).first()).isEmpty()
    }

    @Test
    fun theSessionsOfAnExerciseComeOldestFirstWithTheirSetIds() = runTest {
        start()

        val sessions = progress().observeSessions(bench).first()
        assertThat(sessions.map { it.workoutId.value }).containsExactly("w1", "w2").inOrder()
        assertThat(sessions.first().sets.map { it.id?.value }).containsExactly("a", "b").inOrder()
        assertThat(progress().sessionsBefore(NOW + DAY, setOf(bench)).getValue(bench)).hasSize(1)
        assertThat(progress().sessionsBefore(NOW + 2 * DAY, emptySet())).isEmpty()
    }

    @Test
    fun recordsAreRebuiltFromScratch() = runTest {
        start()
        db.database.progressDao().deleteRecords(listOf("bench"))
        assertThat(progress().observeRecentRecords(10).first()).isEmpty()

        progress().rebuildRecords()

        assertThat(progress().observeRecentRecords(10).first()).hasSize(3)
    }

    private suspend fun start() {
        db.addExercise("bench", name = "Bench Press")
        workouts().create(first)
        workouts().create(second)
    }

    private fun workouts() =
        RoomWorkoutRepository(db.database, db.database.workoutDao(), clock, db.recordKeeper(clock, settings))

    private fun progress() = RoomProgressRepository(db.database.progressDao(), db.recordKeeper(clock, settings))

    private fun set(id: String, kg: Double, reps: Int) = WorkoutSet(
        id = WorkoutSetId(id),
        entered = SetValues(weight = Weight(kg), reps = reps),
        isCompleted = true,
        completedAt = NOW,
    )

    private fun workout(id: String, at: Long, vararg sets: WorkoutSet) = Workout(
        id = WorkoutId(id),
        startedAt = at,
        endedAt = at + HOUR,
        isInProgress = false,
        exercises = listOf(
            WorkoutExercise(
                id = WorkoutExerciseId("$id-bench"),
                exercise = info,
                sets = sets.toList(),
            ),
        ),
    )

    private fun muscle(muscle: Muscle, isPrimary: Boolean) = ExerciseMuscleEntity(
        id = muscle.name,
        exerciseId = "bench",
        muscle = muscle,
        isPrimary = isPrimary,
        createdAt = NOW,
        updatedAt = NOW,
        deletedAt = null,
    )

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR
    }
}
