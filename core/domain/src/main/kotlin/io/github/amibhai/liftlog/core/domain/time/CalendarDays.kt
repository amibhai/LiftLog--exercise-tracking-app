package io.github.amibhai.liftlog.core.domain.time

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Calendar days between two times: 0 = today, 1 = yesterday. Never negative. */
fun daysAgo(then: Long, now: Long, zone: ZoneId): Int {
    val days = ChronoUnit.DAYS.between(localDate(then, zone), localDate(now, zone))
    return days.coerceAtLeast(0).toInt()
}

/** Names a workout that has no name of its own ("Evening workout"). */
enum class PartOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

private val MORNING_HOURS = 5..11
private val AFTERNOON_HOURS = 12..16
private val EVENING_HOURS = 17..20

fun partOfDay(at: Long, zone: ZoneId): PartOfDay = when (Instant.ofEpochMilli(at).atZone(zone).hour) {
    in MORNING_HOURS -> PartOfDay.MORNING
    in AFTERNOON_HOURS -> PartOfDay.AFTERNOON
    in EVENING_HOURS -> PartOfDay.EVENING
    else -> PartOfDay.NIGHT
}

private fun localDate(at: Long, zone: ZoneId) = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
