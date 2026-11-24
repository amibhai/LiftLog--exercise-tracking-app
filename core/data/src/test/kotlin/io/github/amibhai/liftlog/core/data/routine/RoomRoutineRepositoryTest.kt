package io.github.amibhai.liftlog.core.data.routine

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.data.NOW
import io.github.amibhai.liftlog.core.data.TestClock
import io.github.amibhai.liftlog.core.data.TestDatabase
import io.github.amibhai.liftlog.core.data.workout.RoomWorkoutRepository
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.workout.moveExercise
import io.github.amibhai.liftlog.core.domain.workout.removeExercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRoutineRepositoryTest {

    @get:Rule
    val db = TestDatabase()

    private val clock = TestClock()

    private val push = Routine(
        id = RoutineId("push"),
        name = "Push",
        notes = "Monday",
        exercises = listOf(
            slot("bench").copy(repMin = 6, repMax = 8, targetRpe = 8.0, restSeconds = 180, supersetGroup = 1),
            slot("dip").copy(targetSets = 4, targetRir = 2, notes = "Lean forward", supersetGroup = 1),
        ),
    )

    @Test
    fun aRoutineIsStoredCompletely() = runTest {
        create()

        assertThat(repository().get(push.id)).isEqualTo(push)
        assertThat(repository().observe(push.id).first()).isEqualTo(push)
    }

    @Test
    fun routinesKeepTheOrderTheyWereGiven() = runTest {
        create()
        repository().create(Routine(RoutineId("pull"), "Pull"))
        assertThat(names()).containsExactly("Push", "Pull").inOrder()

        repository().reorder(listOf(RoutineId("pull"), RoutineId("push")))

        assertThat(names()).containsExactly("Pull", "Push").inOrder()
    }

    @Test
    fun editsAreStored() = runTest {
        create()

        repository().edit(push.id) { it.moveExercise(0, 1).copy(name = "Push day") }
        assertThat(repository().get(push.id)!!.exercises.map { it.id.value }).containsExactly("dip", "bench").inOrder()

        repository().edit(push.id) { it.removeExercise(RoutineExerciseId("dip")) }
        assertThat(repository().get(push.id)!!.exercises.single().supersetGroup).isNull()
        assertThat(names()).containsExactly("Push day")
    }

    @Test
    fun deletingCanBeUndone() = runTest {
        create()

        repository().delete(push.id)
        assertThat(repository().get(push.id)).isNull()

        repository().restore(push.id)
        assertThat(repository().get(push.id)).isEqualTo(push)
    }

    @Test
    fun theListShowsWhenARoutineWasLastDone() = runTest {
        create()
        assertThat(repository().observeAll().first().single().lastPerformedAt).isNull()

        RoomWorkoutRepository(db.database, db.database.workoutDao(), clock, db.recordKeeper(clock)).create(
            Workout(WorkoutId("w"), startedAt = NOW - 5, endedAt = NOW, routineId = push.id, isInProgress = false),
        )

        assertThat(repository().observeAll().first().single().lastPerformedAt).isEqualTo(NOW - 5)
    }

    private fun repository() =
        RoomRoutineRepository(db.database, db.database.routineDao(), db.database.historyDao(), clock)

    private suspend fun create() {
        db.addExercise("bench")
        db.addExercise("dip")
        repository().create(push)
    }

    private suspend fun names() = repository().observeAll().first().map { it.routine.name }

    private fun slot(name: String) = RoutineExercise(
        id = RoutineExerciseId(name),
        exercise = ExerciseInfo(ExerciseId(name), name, ExerciseType.WEIGHT_REPS, Equipment.BARBELL),
    )
}
