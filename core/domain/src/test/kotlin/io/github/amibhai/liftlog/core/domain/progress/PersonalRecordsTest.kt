package io.github.amibhai.liftlog.core.domain.progress

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.RecordType
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight
import org.junit.jupiter.api.Test

class PersonalRecordsTest {

    private val bench = ExerciseId("bench")
    private val epley = E1rmFormula.EPLEY

    @Test
    fun `the first session only sets the baseline`() {
        val timeline = timeline(session(1, lift(60.0, 8), lift(60.0, 8)))

        assertThat(timeline).isEmpty()
    }

    @Test
    fun `a heavier set breaks the weight record and the estimated one rep max`() {
        val timeline = timeline(session(1, lift(60.0, 5)), session(2, lift(62.5, 5, id = "b")))

        assertThat(timeline.map { it.type }).containsExactly(
            RecordType.HEAVIEST_WEIGHT,
            RecordType.BEST_E1RM,
            RecordType.BEST_SET_VOLUME,
            RecordType.BEST_SESSION_VOLUME,
        ).inOrder()
        val weight = timeline.first()
        assertThat(weight.value).isEqualTo(62.5)
        assertThat(weight.reps).isEqualTo(5)
        assertThat(weight.setId).isEqualTo(WorkoutSetId("b"))
        assertThat(weight.workoutId).isEqualTo(WorkoutId("w2"))
    }

    @Test
    fun `the same numbers again break nothing`() {
        assertThat(timeline(session(1, lift(60.0, 5)), session(2, lift(60.0, 5)))).isEmpty()
    }

    @Test
    fun `most reps at a weight are tracked for every weight on its own`() {
        val timeline = timeline(
            session(1, lift(60.0, 5), lift(50.0, 8)),
            session(2, lift(60.0, 7), lift(50.0, 8)),
            session(3, lift(70.0, 3)),
        )

        val reps = timeline.filter { it.type == RecordType.REPS_AT_WEIGHT }
        assertThat(reps.map { it.value to it.weightKg }).containsExactly(7.0 to 60.0)
    }

    @Test
    fun `only the best set of a session breaks a record`() {
        val timeline = timeline(session(1, lift(60.0, 5)), session(2, lift(62.5, 5), lift(65.0, 5), lift(62.5, 5)))

        assertThat(timeline.filter { it.type == RecordType.HEAVIEST_WEIGHT }.map { it.value }).containsExactly(65.0)
    }

    @Test
    fun `warm-ups never set or break records`() {
        val warmUp = lift(100.0, 5, type = SetType.WARMUP)

        assertThat(timeline(session(1, lift(60.0, 5), warmUp), session(2, lift(65.0, 5))).first().value).isEqualTo(65.0)
        assertThat(recordsBrokenBy(ExerciseType.WEIGHT_REPS, warmUp, baseline(), epley)).isEmpty()
    }

    @Test
    fun `a session record is broken by the work of the whole session`() {
        val timeline = timeline(session(1, lift(60.0, 5)), session(2, lift(50.0, 5), lift(50.0, 5)))

        val volume = timeline.single { it.type == RecordType.BEST_SESSION_VOLUME }
        assertThat(volume.value).isEqualTo(500.0)
        assertThat(volume.setId).isNull()
        assertThat(volume.weightKg).isNull()
    }

    @Test
    fun `less assistance is the better record`() {
        val timeline = recordTimeline(
            bench,
            ExerciseType.BODYWEIGHT_ASSISTED,
            listOf(session(1, lift(30.0, 8)), session(2, lift(25.0, 8)), session(3, lift(35.0, 8))),
            epley,
        )

        assertThat(timeline.map { it.type to it.value }).containsExactly(RecordType.LEAST_ASSISTANCE to 25.0)
    }

    @Test
    fun `timed and distance sets have their own records`() {
        val longer = SetValues(distanceMeters = 5_500.0, durationSeconds = 1_800)
        val timeline = recordTimeline(
            bench,
            ExerciseType.DISTANCE_DURATION,
            listOf(
                ExerciseSession(
                    WorkoutId("w1"),
                    1,
                    listOf(set(SetValues(distanceMeters = 5_000.0, durationSeconds = 1_500))),
                ),
                ExerciseSession(WorkoutId("w2"), 2, listOf(set(longer))),
            ),
            epley,
        )

        assertThat(timeline.map { it.type to it.value }).containsExactly(
            RecordType.LONGEST_DISTANCE to 5_500.0,
            RecordType.LONGEST_DURATION to 1_800.0,
        )
    }

    @Test
    fun `a set in progress shows which records it breaks`() {
        val bests = baseline()

        assertThat(recordsBrokenBy(ExerciseType.WEIGHT_REPS, lift(62.5, 5), bests, epley)).containsExactly(
            RecordType.HEAVIEST_WEIGHT,
            RecordType.BEST_E1RM,
            RecordType.BEST_SET_VOLUME,
        )
        assertThat(recordsBrokenBy(ExerciseType.WEIGHT_REPS, lift(60.0, 6), bests, epley)).containsExactly(
            RecordType.BEST_E1RM,
            RecordType.REPS_AT_WEIGHT,
            RecordType.BEST_SET_VOLUME,
        )
    }

    @Test
    fun `an unknown exercise has nothing to break`() {
        assertThat(recordsBrokenBy(ExerciseType.WEIGHT_REPS, lift(60.0, 5), Bests(), epley)).isEmpty()
    }

    private fun baseline() = bestsOf(ExerciseType.WEIGHT_REPS, listOf(session(1, lift(60.0, 5))), epley)

    private fun timeline(vararg sessions: ExerciseSession) =
        recordTimeline(bench, ExerciseType.WEIGHT_REPS, sessions.toList(), epley)

    private fun session(number: Int, vararg sets: LoggedSet) =
        ExerciseSession(WorkoutId("w$number"), at = number.toLong(), sets = sets.toList())

    private fun lift(kg: Double, reps: Int, id: String = "s", type: SetType = SetType.WORKING) =
        set(SetValues(weight = Weight(kg), reps = reps), id, type)

    private fun set(values: SetValues, id: String = "s", type: SetType = SetType.WORKING) =
        LoggedSet(type, values, id = WorkoutSetId(id))
}
