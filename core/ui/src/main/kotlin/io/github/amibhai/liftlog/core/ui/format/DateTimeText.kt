package io.github.amibhai.liftlog.core.ui.format

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.ZoneId

/** "Mon, Nov 24, 6:05 PM": follows the phone's language and 12/24-hour setting. */
@Composable
fun dateTimeText(millis: Long): String = format(
    millis,
    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or
        DateUtils.FORMAT_ABBREV_ALL,
)

/** "Mon, Nov 24"; the year only when it is not this year. */
@Composable
fun dateText(millis: Long): String =
    format(millis, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)

/** "November 2026". */
@Composable
fun monthText(millis: Long): String =
    format(millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_MONTH_DAY or DateUtils.FORMAT_SHOW_YEAR)

/** "6:05 PM" or "18:05". */
@Composable
fun timeText(millis: Long): String = format(millis, DateUtils.FORMAT_SHOW_TIME)

/** "Nov 24", for the labels of a chart. */
@Composable
fun dayMonthText(date: LocalDate): String {
    val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return format(millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR)
}

@Composable
private fun format(millis: Long, flags: Int): String = DateUtils.formatDateTime(LocalContext.current, millis, flags)
