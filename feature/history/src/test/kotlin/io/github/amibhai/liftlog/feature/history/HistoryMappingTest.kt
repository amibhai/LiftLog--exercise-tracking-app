package io.github.amibhai.liftlog.feature.history

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import org.junit.Test
import java.text.NumberFormat
import java.time.YearMonth
import java.time.ZoneOffset

class HistoryMappingTest {

    private val lastMonth = NOW - 30 * 24 * HOUR

    @Test
    fun workoutsAreGroupedByMonthNewestFirst() {
        val items = historyItems(
            listOf(finished("a", NOW - HOUR), finished("b", NOW - 2 * HOUR), finished("c", lastMonth)),
            UserSettings(),
            ZoneOffset.UTC,
        )

        assertThat(items.map { it.key }).containsExactly("month-2026-11", "a", "b", "month-2026-10", "c").inOrder()
        assertThat((items.first() as HistoryItem.Month).month).isEqualTo(YearMonth.of(2026, 11))
    }

    @Test
    fun aCardSumsUpTheWorkout() {
        val workout = finished(
            "a",
            NOW - HOUR,
            exercise("1", bench, done("s1", 80.0, 8), done("s2", 85.0, 5)),
            exercise("2", squat, done("s3", 100.0, 5)),
        )

        val entry = historyItems(listOf(workout), UserSettings(), ZoneOffset.UTC)[1] as HistoryItem.Entry

        assertThat(entry.durationSeconds).isEqualTo(3_600)
        assertThat(entry.sets).isEqualTo(3)
        assertThat(entry.volume).isEqualTo(NumberFormat.getIntegerInstance().format(1_565) + " kg")
        assertThat(entry.exercises).containsExactly(
            ExerciseLine("Bench Press", 2, "85 kg × 5"),
            ExerciseLine("Squat", 1, "100 kg × 5"),
        ).inOrder()
        assertThat(entry.moreExercises).isEqualTo(0)
    }

    @Test
    fun aLongWorkoutListsFourExercisesAndCountsTheRest() {
        val exercises = (1..6).map { exercise("$it", bench, done("s$it", 50.0, 10)) }
        val workout = finished("a", NOW - HOUR, *exercises.toTypedArray())

        val entry = historyItems(listOf(workout), UserSettings(), ZoneOffset.UTC)[1] as HistoryItem.Entry

        assertThat(entry.exercises).hasSize(4)
        assertThat(entry.moreExercises).isEqualTo(2)
    }
}
