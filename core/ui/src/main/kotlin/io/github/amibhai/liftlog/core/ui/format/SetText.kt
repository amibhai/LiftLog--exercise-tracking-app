package io.github.amibhai.liftlog.core.ui.format

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WeightMeaning
import io.github.amibhai.liftlog.core.domain.units.DistanceUnit
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.util.Locale

/** Sets as text in the active unit. Unit symbols are international, so they are not translated. */
@Immutable
data class SetText(val unit: WeightUnit, private val locale: Locale = Locale.getDefault()) {

    /** "82.5 kg × 8", "+10 kg × 6", "1:30", "5.25 km · 28:30". */
    fun summary(type: ExerciseType, values: SetValues): String = join(type, values, withUnits = true)

    /** Without units, for the narrow Previous column: "82.5 × 8". */
    fun compact(type: ExerciseType, values: SetValues): String = join(type, values, withUnits = false)

    /** One field as a set row shows it; empty when there is no value. */
    fun field(type: ExerciseType, values: SetValues, field: SetField): String = when (field) {
        SetField.WEIGHT -> values.weight?.let { sign(type) + LiftLogFormat.weight(it, unit, locale) }

        SetField.REPS -> values.reps?.toString()

        SetField.DURATION -> values.durationSeconds?.let(LiftLogFormat::duration)

        SetField.DISTANCE -> values.distanceMeters?.let {
            LiftLogFormat.decimal(DistanceUnit.of(type, unit).display(it), locale)
        }
    }.orEmpty()

    fun unitOf(type: ExerciseType, field: SetField): String? = when (field) {
        SetField.WEIGHT -> unit.symbol
        SetField.DISTANCE -> DistanceUnit.of(type, unit).symbol
        SetField.REPS, SetField.DURATION -> null
    }

    fun weight(weight: Weight): String = LiftLogFormat.weight(weight, unit, locale) + " " + unit.symbol

    private fun join(type: ExerciseType, values: SetValues, withUnits: Boolean): String {
        val parts = type.fields.mapNotNull { field ->
            val text = field(type, values, field).ifEmpty { return@mapNotNull null }
            val symbol = unitOf(type, field).takeIf { withUnits }
            field to if (symbol == null) text else "$text $symbol"
        }
        val separator = if (type == ExerciseType.DISTANCE_DURATION) " · " else " × "
        val text = parts.joinToString(separator) { it.second }
        // A lone rep count still reads as reps.
        return if (parts.singleOrNull()?.first == SetField.REPS) "× $text" else text
    }

    private fun sign(type: ExerciseType): String = when (type.weightMeaning) {
        WeightMeaning.ADDED -> "+"
        WeightMeaning.ASSISTANCE -> "−"
        WeightMeaning.LOAD, WeightMeaning.NONE -> ""
    }
}

val WeightUnit.symbol: String
    get() = when (this) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }

val DistanceUnit.symbol: String
    get() = when (this) {
        DistanceUnit.METER -> "m"
        DistanceUnit.KILOMETER -> "km"
        DistanceUnit.YARD -> "yd"
        DistanceUnit.MILE -> "mi"
    }
