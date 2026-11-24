package io.github.amibhai.liftlog.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.ui.R

private const val DAYS_PER_WEEK = 7
private const val DAYS_PER_MONTH = 30
private const val DAYS_PER_YEAR = 365
private const val SHOW_WEEKS_FROM = 14
private const val SHOW_MONTHS_FROM = 60

/** "Today", "Yesterday", "3 days ago", "5 weeks ago". */
@Composable
fun relativeDays(days: Int): String = when {
    days <= 0 -> stringResource(R.string.ui_today)
    days == 1 -> stringResource(R.string.ui_yesterday)
    days < SHOW_WEEKS_FROM -> pluralStringResource(R.plurals.ui_days_ago, days, days)
    days < SHOW_MONTHS_FROM -> (days / DAYS_PER_WEEK).let { pluralStringResource(R.plurals.ui_weeks_ago, it, it) }
    days < DAYS_PER_YEAR -> (days / DAYS_PER_MONTH).let { pluralStringResource(R.plurals.ui_months_ago, it, it) }
    else -> (days / DAYS_PER_YEAR).let { pluralStringResource(R.plurals.ui_years_ago, it, it) }
}
