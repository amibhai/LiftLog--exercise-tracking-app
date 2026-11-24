package io.github.amibhai.liftlog.core.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.RecordType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.units.DistanceUnit
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.R
import kotlin.math.roundToInt

/** Names of the kinds of personal record. */
@StringRes
fun RecordType.labelRes(): Int = when (this) {
    RecordType.HEAVIEST_WEIGHT -> R.string.ui_record_heaviest
    RecordType.BEST_E1RM -> R.string.ui_record_e1rm
    RecordType.REPS_AT_WEIGHT -> R.string.ui_record_reps_at_weight
    RecordType.BEST_SET_VOLUME -> R.string.ui_record_set_volume
    RecordType.BEST_SESSION_VOLUME -> R.string.ui_record_session_volume
    RecordType.MOST_REPS -> R.string.ui_record_most_reps
    RecordType.LEAST_ASSISTANCE -> R.string.ui_record_least_assistance
    RecordType.LONGEST_DURATION -> R.string.ui_record_longest_time
    RecordType.LONGEST_DISTANCE -> R.string.ui_record_longest_distance
}

/** The value of a record: "102.5 kg", "60 kg × 8", "12 reps", "1:30", "5.25 km". */
@Composable
fun recordValueText(
    type: RecordType,
    value: Double,
    weightKg: Double?,
    exerciseType: ExerciseType,
    unit: WeightUnit,
): String = when (type) {
    RecordType.HEAVIEST_WEIGHT, RecordType.LEAST_ASSISTANCE -> SetText(unit).weight(Weight(value))

    RecordType.BEST_E1RM -> LiftLogFormat.estimate(value, unit)

    RecordType.REPS_AT_WEIGHT -> SetText(unit).summary(
        ExerciseType.WEIGHT_REPS,
        SetValues(weight = weightKg?.let(::Weight), reps = value.roundToInt()),
    )

    RecordType.BEST_SET_VOLUME, RecordType.BEST_SESSION_VOLUME -> LiftLogFormat.volume(Weight(value), unit)

    RecordType.MOST_REPS -> value.roundToInt().let { pluralStringResource(R.plurals.ui_reps, it, it) }

    RecordType.LONGEST_DURATION -> LiftLogFormat.duration(value.roundToInt())

    RecordType.LONGEST_DISTANCE -> {
        val distance = DistanceUnit.of(exerciseType, unit)
        LiftLogFormat.decimal(distance.display(value)) + " " + distance.symbol
    }
}
