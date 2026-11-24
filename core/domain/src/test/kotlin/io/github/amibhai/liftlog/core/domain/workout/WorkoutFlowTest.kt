package io.github.amibhai.liftlog.core.domain.workout

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.RestTimer
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import org.junit.jupiter.api.Test

/** What happens after a set (SPEC §5): where the focus goes next and whether to rest first. */
class WorkoutFlowTest {

    private val settings = UserSettings(defaultRestSeconds = 120)

    @Test
    fun `after a set you rest, then do the next set`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8)), set("2")))

        assertThat(start.complete("1").nextStep("1")).isEqualTo(NextStep(WorkoutSetId("2"), restFirst = true))
    }

    @Test
    fun `after the last set of an exercise the next exercise is up`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8))), exercise("row", set("r1")))

        assertThat(start.complete("1").nextStep("1")).isEqualTo(NextStep(WorkoutSetId("r1"), restFirst = true))
    }

    @Test
    fun `an earlier skipped set is next when nothing follows`() {
        val start = workout(exercise("bench", set("1")), exercise("row", set("r1", target = values(60.0, 8))))

        assertThat(start.complete("r1").nextStep("r1")).isEqualTo(NextStep(WorkoutSetId("1"), restFirst = true))
    }

    @Test
    fun `there is no rest before a drop set`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8)), set("2", SetType.DROP)))

        assertThat(start.complete("1").nextStep("1")).isEqualTo(NextStep(WorkoutSetId("2"), restFirst = false))
    }

    @Test
    fun `there is no rest after the very last set`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8))))

        assertThat(start.complete("1").nextStep("1")).isEqualTo(NextStep(null, restFirst = false))
    }

    @Test
    fun `a superset jumps to the next exercise and rests only after the round`() {
        val start = workout(
            exercise("a", set("a1", target = values(20.0, 10)), set("a2"), group = 1),
            exercise("b", set("b1", target = values(30.0, 10)), set("b2"), group = 1),
        )

        val first = start.complete("a1")
        assertThat(first.nextStep("a1")).isEqualTo(NextStep(WorkoutSetId("b1"), restFirst = false))

        val round = first.complete("b1")
        assertThat(round.nextStep("b1")).isEqualTo(NextStep(WorkoutSetId("a2"), restFirst = true))
    }

    @Test
    fun `a circuit of three visits every exercise before resting`() {
        val start = workout(
            exercise("a", set("a1", target = values(20.0, 10)), group = 1),
            exercise("b", set("b1", target = values(20.0, 10)), group = 1),
            exercise("c", set("c1", target = values(20.0, 10)), group = 1),
            exercise("d", set("d1")),
        )

        val afterB = start.complete("a1").complete("b1")
        assertThat(afterB.nextStep("b1")).isEqualTo(NextStep(WorkoutSetId("c1"), restFirst = false))
        val afterC = afterB.complete("c1")
        assertThat(afterC.nextStep("c1")).isEqualTo(NextStep(WorkoutSetId("d1"), restFirst = true))
    }

    @Test
    fun `uneven supersets continue with the exercise that has sets left`() {
        val start = workout(
            exercise(
                "a",
                set("a1", target = values(20.0, 10), done = true),
                set("a2", target = values(20.0, 10)),
                group = 1,
            ),
            exercise("b", set("b1", target = values(20.0, 10), done = true), group = 1),
            exercise("c", set("c1")),
        )

        assertThat(start.complete("a2").nextStep("a2")).isEqualTo(NextStep(WorkoutSetId("c1"), restFirst = true))
    }

    @Test
    fun `rest length comes from the workout exercise, then the exercise, then the settings`() {
        val plain = exercise("bench")
        val withDefault = plain.copy(exercise = plain.exercise.copy(defaults = ExerciseDefaults(restSeconds = 180)))

        assertThat(plain.restSeconds(settings)).isEqualTo(120)
        assertThat(withDefault.restSeconds(settings)).isEqualTo(180)
        assertThat(withDefault.copy(restSeconds = 60).restSeconds(settings)).isEqualTo(60)
    }

    @Test
    fun `logging a set starts the rest timer as an absolute end time`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8)), set("2")))

        assertThat(start.logSet(WorkoutSetId("1"), NOW, settings).restTimer)
            .isEqualTo(RestTimer(endsAt = NOW + 120_000, totalSeconds = 120))
    }

    @Test
    fun `no rest timer starts when it is switched off or the length is zero`() {
        val start = workout(exercise("bench", set("1", target = values(80.0, 8)), set("2")))

        assertThat(start.logSet(WorkoutSetId("1"), NOW, settings.copy(autoStartRestTimer = false)).restTimer).isNull()
        assertThat(start.logSet(WorkoutSetId("1"), NOW, settings.copy(defaultRestSeconds = 0)).restTimer).isNull()
    }

    @Test
    fun `a running rest timer ends when the next set needs no rest`() {
        val running = RestTimer(endsAt = NOW + 30_000, totalSeconds = 120)
        val start = workout(
            exercise("a", set("a1", target = values(20.0, 10)), group = 1),
            exercise("b", set("b1"), group = 1),
        ).copy(restTimer = running)

        assertThat(start.logSet(WorkoutSetId("a1"), NOW, settings).restTimer).isNull()
    }

    @Test
    fun `adjusting the timer moves its end and never rests a negative time`() {
        val timer = RestTimer(endsAt = NOW + 60_000, totalSeconds = 120)

        assertThat(timer.adjustedBy(15, NOW)).isEqualTo(RestTimer(NOW + 75_000, 135))
        assertThat(timer.adjustedBy(-15, NOW)).isEqualTo(RestTimer(NOW + 45_000, 105))
        assertThat(timer.adjustedBy(-60, NOW)).isNull()
        assertThat(RestTimer(NOW - 5_000, 120).adjustedBy(15, NOW)).isEqualTo(RestTimer(NOW + 15_000, 135))
    }

    @Test
    fun `finishing drops what was not done`() {
        val start = workout(
            exercise("bench", set("1", target = values(80.0, 8), done = true), set("2", target = values(80.0, 8))),
            exercise("row", set("r1")),
        ).copy(restTimer = RestTimer(NOW + 1_000, 120))

        val done = start.finish(NOW)

        assertThat(done.isInProgress).isFalse()
        assertThat(done.endedAt).isEqualTo(NOW)
        assertThat(done.restTimer).isNull()
        assertThat(done.exercises.map { it.id.value }).containsExactly("bench")
        assertThat(done.exercise("bench").sets.map { it.id.value }).containsExactly("1")
    }

    private fun Workout.complete(id: String) = completeSet(WorkoutSetId(id), NOW)

    private fun Workout.nextStep(id: String) = nextStepAfter(WorkoutSetId(id))
}
