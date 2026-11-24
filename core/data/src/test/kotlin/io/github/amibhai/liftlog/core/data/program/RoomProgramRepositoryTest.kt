package io.github.amibhai.liftlog.core.data.program

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.data.NOW
import io.github.amibhai.liftlog.core.data.TestClock
import io.github.amibhai.liftlog.core.data.TestDatabase
import io.github.amibhai.liftlog.core.data.routine.RoomRoutineRepository
import io.github.amibhai.liftlog.core.data.workout.RoomWorkoutRepository
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.program.nextDay
import io.github.amibhai.liftlog.core.domain.program.removeDay
import io.github.amibhai.liftlog.core.domain.program.reorderDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomProgramRepositoryTest {

    @get:Rule
    val db = TestDatabase()

    private val clock = TestClock()
    private val ppl = Program(
        ProgramId("ppl"),
        "PPL",
        templateKey = "PUSH_PULL_LEGS",
        days = listOf(day("push"), day("pull"), day("legs")),
    )

    @Test
    fun aProgramIsStoredWithItsDaysInOrder() = runTest {
        create()

        assertThat(programs().observe(ppl.id).first()).isEqualTo(ppl)
        assertThat(programs().observeActive().first()).isNull()
    }

    @Test
    fun onlyOneProgramIsFollowed() = runTest {
        create()
        programs().create(Program(ProgramId("other"), "Other"))

        programs().activate(ppl.id)
        programs().activate(ProgramId("other"))
        assertThat(programs().observeActive().first()?.program?.name).isEqualTo("Other")
        assertThat(programs().observeAll().first().map { it.isActive }).containsExactly(true, false).inOrder()

        programs().activate(null)
        assertThat(programs().observeActive().first()).isNull()
    }

    @Test
    fun theNextDayFollowsTheOneDoneLast() = runTest {
        create()
        programs().activate(ppl.id)
        assertThat(programs().observeActive().first()?.nextDay?.name).isEqualTo("push")

        workouts().create(workout("w1", day = "push", at = NOW - 2))
        workouts().create(workout("w2", day = "legs", at = NOW - 1))
        assertThat(programs().observeActive().first()?.lastDay).isEqualTo(ProgramDayId("legs"))
        assertThat(programs().observeActive().first()?.nextDay?.name).isEqualTo("push")

        workouts().delete(WorkoutId("w2"))
        assertThat(programs().observeActive().first()?.nextDay?.name).isEqualTo("pull")
    }

    @Test
    fun editsAreStored() = runTest {
        create()

        programs().edit(ppl.id) {
            it.copy(name = "My PPL").removeDay(ProgramDayId("pull")).reorderDays(listOf(ProgramDayId("legs")))
        }

        val edited = programs().observe(ppl.id).first()!!
        assertThat(edited.name).isEqualTo("My PPL")
        assertThat(edited.days.map { it.name }).containsExactly("legs", "push").inOrder()
    }

    @Test
    fun aDayLosesItsRoutineWhenThatIsDeletedAndGetsItBackWithUndo() = runTest {
        create()

        routines().delete(RoutineId("pull"))
        assertThat(programs().observe(ppl.id).first()!!.days[1].routineId).isNull()
        programs().edit(ppl.id) { it.copy(name = "Renamed meanwhile") }

        routines().restore(RoutineId("pull"))
        assertThat(programs().observe(ppl.id).first()!!.days[1].routineId).isEqualTo(RoutineId("pull"))
    }

    @Test
    fun aDeletedProgramIsNoLongerFollowedAndItsRoutinesStay() = runTest {
        create()
        programs().activate(ppl.id)

        programs().delete(ppl.id)

        assertThat(programs().observeActive().first()).isNull()
        assertThat(programs().observeAll().first()).isEmpty()
        assertThat(routines().get(RoutineId("push"))).isNotNull()
    }

    private suspend fun create() {
        for (day in ppl.days) routines().create(Routine(day.routineId!!, day.name))
        programs().create(ppl)
    }

    private fun programs() = RoomProgramRepository(db.database, db.database.programDao(), clock)

    private fun routines() =
        RoomRoutineRepository(db.database, db.database.routineDao(), db.database.historyDao(), clock)

    private fun workouts() = RoomWorkoutRepository(db.database, db.database.workoutDao(), clock, db.recordKeeper(clock))

    private fun day(name: String) = ProgramDay(ProgramDayId(name), name, RoutineId(name))

    private fun workout(id: String, day: String, at: Long) = Workout(
        id = WorkoutId(id),
        startedAt = at,
        endedAt = at,
        isInProgress = false,
        programDayId = ProgramDayId(day),
    )
}
