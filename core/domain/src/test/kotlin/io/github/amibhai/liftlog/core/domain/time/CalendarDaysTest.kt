package io.github.amibhai.liftlog.core.domain.time

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarDaysTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `days are counted by the calendar, not by 24 hours`() {
        val now = at(2026, 11, 24, 0, 30)

        assertThat(daysAgo(at(2026, 11, 24, 0, 5), now, zone)).isEqualTo(0)
        assertThat(daysAgo(at(2026, 11, 23, 23, 55), now, zone)).isEqualTo(1)
        assertThat(daysAgo(at(2026, 11, 21, 12, 0), now, zone)).isEqualTo(3)
        assertThat(daysAgo(at(2026, 11, 25, 9, 0), now, zone)).isEqualTo(0)
    }

    @Test
    fun `a workout is named after the time of day it started`() {
        assertThat(partOfDay(at(2026, 11, 24, 6, 0), zone)).isEqualTo(PartOfDay.MORNING)
        assertThat(partOfDay(at(2026, 11, 24, 12, 0), zone)).isEqualTo(PartOfDay.AFTERNOON)
        assertThat(partOfDay(at(2026, 11, 24, 17, 30), zone)).isEqualTo(PartOfDay.EVENING)
        assertThat(partOfDay(at(2026, 11, 24, 22, 0), zone)).isEqualTo(PartOfDay.NIGHT)
        assertThat(partOfDay(at(2026, 11, 24, 3, 0), zone)).isEqualTo(PartOfDay.NIGHT)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
}
