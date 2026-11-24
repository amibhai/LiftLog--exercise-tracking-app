package io.github.amibhai.liftlog.core.ui.format

import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/** Locale-aware display formatting. Values arrive already rounded by the domain unit helpers. */
object LiftLogFormat {
    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3_600

    fun decimal(value: BigDecimal, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = Weight.DISPLAY_DECIMALS
            minimumFractionDigits = 0
            isGroupingUsed = false
        }.format(value)

    fun weight(weight: Weight, unit: WeightUnit, locale: Locale = Locale.getDefault()): String =
        decimal(weight.displayValue(unit), locale)

    /** "0:45", "1:30", "1:02:03". */
    fun duration(totalSeconds: Int): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val hours = seconds / SECONDS_PER_HOUR
        val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, rest)
        }
    }

    /** A rounded total with thousands separators: "3,240 kg". */
    fun volume(volume: Weight, unit: WeightUnit, locale: Locale = Locale.getDefault()): String {
        val rounded = volume.displayValue(unit).setScale(0, RoundingMode.HALF_UP)
        return NumberFormat.getIntegerInstance(locale).format(rounded) + " " + unit.symbol
    }

    fun decimalSeparator(locale: Locale = Locale.getDefault()): Char =
        DecimalFormatSymbols.getInstance(locale).decimalSeparator
}
