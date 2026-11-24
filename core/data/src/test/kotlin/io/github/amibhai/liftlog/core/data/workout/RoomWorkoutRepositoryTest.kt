package io.github.amibhai.liftlog.core.data.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.data.NOW
import io.github.amibhai.liftlog.core.data.TestClock
import io.github.amibhai.liftlog.core.data.TestDatabase
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.RestTimer
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.workout.completeSet
import io.github.amibhai.liftlog.core.domain.workout.finish
import io.github.amibhai.liftlog.core.domain.workout.insertExercise
import io.github.amibhai.liftlog.core.domain.workout.insertSet
import io.github.amibhai.liftlog.core.domain.workout.moveExercise
import io.github.amibhai.liftlog.core.domain.workout.removeExercise
import io.github.amibhai.liftlog.core.domain.workout.removeSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomWorkoutRepositoryTest {

    @get:Rule
    val db = TestDatabase()

    private val clock = TestClock()
    private val id = WorkoutId("w")

    private val workout = Workout(
        id = id,
        name = "Push",
        startedAt = NOW,
        notes = "Slept well",
        restTimer = RestTimer(NOW + 90_000, 120),
        exercises = listOf(
            exercise("bench", supersetGroup = 1, sets = listOf(set("b1", 80.0, 8), set("b2", 80.0, 8))),
            exercise(
                "dip",
                supersetGroup = 1,
                sets = listOf(set("d1", 10.0, 6).copy(rpe = 8.5, type = SetType.WARMUP)),
            ),
        ),
    )

    @Test
    fun aWorkoutIsStoredCompletely() = runTest {
        create()

        assertThat(repository().observe(id).first()).isEqualTo(workout)
        assertThat(repository().observeActive().first()).isEqualTo(workout)
    }

    @Test
    fun timedAndDistanceSetsKeepTheirValuesAndTargets() = runTest {
        db.addExercise("run", type = ExerciseType.DISTANCE_DURATION)
        val run = exercise("run", type = ExerciseType.DISTANCE_DURATION).copy(
            sets = listOf(
                WorkoutSet(
                    WorkoutSetId("r1"),
                    entered = SetValues(durationSeconds = 1_500, distanceMeters = 5_000.0),
                    target = SetValues(durationSeconds = 1_600, distanceMeters = 5_250.5),
                ),
            ),
        )
        val cardio = Workout(id = id, startedAt = NOW, exercises = listOf(run))

        repository().create(cardio)

        assertThat(repository().observe(id).first()).isEqualTo(cardio)
    }

    @Test
    fun theWorkoutInProgressSurvivesANewProcess() = runTest {
        create()
        repository().edit(id) { it.completeSet(WorkoutSetId("b1"), NOW + 5) }

        // A new repository has no memory of the old one; everything comes from the database.
        val resumed = repository().observeActive().first()!!

        assertThat(resumed.exercises.first().sets.first().isCompleted).isTrue()
        assertThat(resumed.restTimer).isEqualTo(RestTimer(NOW + 90_000, 120))
    }

    @Test
    fun anEditWritesOnlyTheRowsThatChanged() = runTest {
        create()
        clock.now = NOW + 1_000

        repository().edit(id) { it.completeSet(WorkoutSetId("b1"), clock.now) }

        val stored = db.database.workoutDao().load("w")!!
        assertThat(stored.workout.updatedAt).isEqualTo(NOW)
        assertThat(stored.exercises.map { it.workoutExercise.updatedAt }).containsExactly(NOW, NOW)
        val updated = stored.exercises.flatMap { it.sets }.associate { it.id to it.updatedAt }
        assertThat(updated).containsExactly("b1", NOW + 1_000, "b2", NOW, "d1", NOW)
        assertThat(stored.exercises.flatMap { it.sets }.first { it.id == "b1" }.createdAt).isEqualTo(NOW)
    }

    @Test
    fun removedSetsAreSoftDeletedAndCanComeBack() = runTest {
        create()
        val removed = workout.exercises[0].sets[0]

        repository().edit(id) { it.removeSet(removed.id) }
        assertThat(repository().observe(id).first()!!.exercises[0].sets.map { it.id.value }).containsExactly("b2")
        assertThat(db.database.workoutDao().load("w")!!.exercises.flatMap { it.sets }).hasSize(3)

        repository().edit(id) { it.insertSet(WorkoutExerciseId("bench-slot"), 0, removed) }
        assertThat(repository().observe(id).first()).isEqualTo(workout)
    }

    @Test
    fun removedExercisesComeBackWithTheirSets() = runTest {
        create()
        val removed = workout.exercises[0]

        repository().edit(id) { it.removeExercise(removed.id) }
        assertThat(repository().observe(id).first()!!.exercises.map { it.id.value }).containsExactly("dip-slot")

        repository().edit(id) { it.insertExercise(0, removed, workout) }
        assertThat(repository().observe(id).first()).isEqualTo(workout)
    }

    @Test
    fun theOrderIsStored() = runTest {
        create()

        repository().edit(id) { it.moveExercise(0, 1) }

        assertThat(repository().observe(id).first()!!.exercises.map { it.id.value })
            .containsExactly("dip-slot", "bench-slot").inOrder()
    }

    @Test
    fun aFinishedWorkoutMovesToTheHistory() = runTest {
        create()
        repository().edit(id) { it.completeSet(WorkoutSetId("b1"), NOW).finish(NOW + 60_000) }

        assertThat(repository().observeActive().first()).isNull()
        val finished = repository().observeFinished(10).first().single()
        assertThat(finished.endedAt).isEqualTo(NOW + 60_000)
        assertThat(finished.exercises.single().sets.single().id.value).isEqualTo("b1")
        assertThat(RoomHistoryRepository(db.database.historyDao()).latestSessions(NOW + 1).keys)
            .containsExactly(ExerciseId("bench"))
    }

    @Test
    fun deletingCanBeUndone() = runTest {
        create()

        repository().delete(id)
        assertThat(repository().observe(id).first()).isNull()
        assertThat(repository().edit(id) { it.copy(name = "x") }).isNull()

        repository().restore(id)
        assertThat(repository().observe(id).first()).isEqualTo(workout)
    }

    private fun repository() = RoomWorkoutRepository(
        db.database,
        db.database.workoutDao(),
        clock,
        db.recordKeeper(clock),
    )

    private suspend fun create() {
        db.addExercise("bench")
        db.addExercise("dip")
        repository().create(workout)
    }

    private fun exercise(
        name: String,
        supersetGroup: Int? = null,
        type: ExerciseType = ExerciseType.WEIGHT_REPS,
        sets: List<WorkoutSet> = emptyList(),
    ) = WorkoutExercise(
        id = WorkoutExerciseId("$name-slot"),
        exercise = ExerciseInfo(ExerciseId(name), name, type, Equipment.BARBELL),
        supersetGroup = supersetGroup,
        notes = "Note on $name",
        restSeconds = 90,
        sets = sets,
    )

    private fun set(id: String, kg: Double, reps: Int) =
        WorkoutSet(WorkoutSetId(id), target = SetValues(Weight(kg), reps))
}
