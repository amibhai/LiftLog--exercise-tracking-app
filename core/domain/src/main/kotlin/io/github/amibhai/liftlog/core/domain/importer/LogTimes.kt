package io.github.amibhai.liftlog.core.domain.importer

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L

private val TimeFormats: List<DateTimeFormatter> = listOf(
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd HH:mm",
    "yyyy-MM-dd'T'HH:mm:ss",
    "d MMM yyyy, HH:mm",
    "d MMM yyyy, h:mm a",
    "MMM d, yyyy, HH:mm",
    "MMM d, yyyy, h:mm a",
).map { DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it).toFormatter(Locale.ENGLISH) }

private val UnitParts = Regex("(\\d+)\\s*([hms])", RegexOption.IGNORE_CASE)

/** "2024-11-02 18:22:13", "28 Mar 2025, 17:29", or only a day, which counts from noon. */
internal fun parseLogTime(text: String): LocalDateTime? =
    TimeFormats.firstNotNullOfOrNull { format -> orNull { LocalDateTime.parse(text, format) } }
        ?: orNull { LocalDate.parse(text).atTime(LocalTime.NOON) }

/** "1h 5m", "52s", "42:15", "1:02:03" or a number of seconds. */
internal fun parseLogDuration(text: String): Long? {
    val parts = UnitParts.findAll(text).toList()
    return when {
        parts.isNotEmpty() -> parts.sumOf { part ->
            val (amount, unit) = part.destructured
            amount.toLong() * when (unit.lowercase(Locale.ROOT)) {
                "h" -> SECONDS_PER_HOUR
                "m" -> SECONDS_PER_MINUTE
                else -> 1L
            }
        }

        ':' in text -> text.split(':').map { it.toLongOrNull() }
            .fold(0L as Long?) { total, part ->
                if (total == null || part == null) null else total * SECONDS_PER_MINUTE + part
            }

        else -> text.toLongOrNull()
    }
}

private fun <T> orNull(parse: () -> T): T? = try {
    parse()
} catch (_: DateTimeException) {
    null
}
