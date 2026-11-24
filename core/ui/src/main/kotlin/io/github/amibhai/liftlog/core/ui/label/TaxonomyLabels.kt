package io.github.amibhai.liftlog.core.ui.label

import androidx.annotation.StringRes
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Mechanic
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.R

/** Localized names for the taxonomy enums; the enums themselves carry no display text. */
@StringRes
fun Muscle.labelRes(): Int = when (this) {
    Muscle.CHEST -> R.string.ui_muscle_chest
    Muscle.FRONT_DELTS -> R.string.ui_muscle_front_delts
    Muscle.SIDE_DELTS -> R.string.ui_muscle_side_delts
    Muscle.REAR_DELTS -> R.string.ui_muscle_rear_delts
    Muscle.BICEPS -> R.string.ui_muscle_biceps
    Muscle.TRICEPS -> R.string.ui_muscle_triceps
    Muscle.FOREARMS -> R.string.ui_muscle_forearms
    Muscle.LATS -> R.string.ui_muscle_lats
    Muscle.UPPER_BACK -> R.string.ui_muscle_upper_back
    Muscle.LOWER_BACK -> R.string.ui_muscle_lower_back
    Muscle.ABS -> R.string.ui_muscle_abs
    Muscle.OBLIQUES -> R.string.ui_muscle_obliques
    Muscle.GLUTES -> R.string.ui_muscle_glutes
    Muscle.QUADS -> R.string.ui_muscle_quads
    Muscle.HAMSTRINGS -> R.string.ui_muscle_hamstrings
    Muscle.ADDUCTORS -> R.string.ui_muscle_adductors
    Muscle.ABDUCTORS -> R.string.ui_muscle_abductors
    Muscle.CALVES -> R.string.ui_muscle_calves
    Muscle.NECK -> R.string.ui_muscle_neck
}

@StringRes
fun Equipment.labelRes(): Int = when (this) {
    Equipment.BARBELL -> R.string.ui_equipment_barbell
    Equipment.DUMBBELL -> R.string.ui_equipment_dumbbell
    Equipment.KETTLEBELL -> R.string.ui_equipment_kettlebell
    Equipment.MACHINE -> R.string.ui_equipment_machine
    Equipment.CABLE -> R.string.ui_equipment_cable
    Equipment.SMITH -> R.string.ui_equipment_smith
    Equipment.BODYWEIGHT -> R.string.ui_equipment_bodyweight
    Equipment.BAND -> R.string.ui_equipment_band
    Equipment.EZ_BAR -> R.string.ui_equipment_ez_bar
    Equipment.TRAP_BAR -> R.string.ui_equipment_trap_bar
    Equipment.PLATE -> R.string.ui_equipment_plate
    Equipment.OTHER -> R.string.ui_equipment_other
}

@StringRes
fun ExerciseType.labelRes(): Int = when (this) {
    ExerciseType.WEIGHT_REPS -> R.string.ui_type_weight_reps
    ExerciseType.BODYWEIGHT_ADDED -> R.string.ui_type_bodyweight_added
    ExerciseType.BODYWEIGHT_ASSISTED -> R.string.ui_type_bodyweight_assisted
    ExerciseType.REPS_ONLY -> R.string.ui_type_reps_only
    ExerciseType.DURATION -> R.string.ui_type_duration
    ExerciseType.DISTANCE_DURATION -> R.string.ui_type_distance_duration
    ExerciseType.WEIGHT_DISTANCE -> R.string.ui_type_weight_distance
}

@StringRes
fun ExerciseCategory.labelRes(): Int = when (this) {
    ExerciseCategory.STRENGTH -> R.string.ui_category_strength
    ExerciseCategory.OLYMPIC -> R.string.ui_category_olympic
    ExerciseCategory.PLYOMETRIC -> R.string.ui_category_plyometric
    ExerciseCategory.STRONGMAN -> R.string.ui_category_strongman
    ExerciseCategory.CARDIO -> R.string.ui_category_cardio
    ExerciseCategory.MOBILITY -> R.string.ui_category_mobility
}

@StringRes
fun Mechanic.labelRes(): Int = when (this) {
    Mechanic.COMPOUND -> R.string.ui_mechanic_compound
    Mechanic.ISOLATION -> R.string.ui_mechanic_isolation
}

@StringRes
fun WeightUnit.labelRes(): Int = when (this) {
    WeightUnit.KG -> R.string.ui_unit_kg
    WeightUnit.LB -> R.string.ui_unit_lb
}

@StringRes
fun SetField.labelRes(): Int = when (this) {
    SetField.WEIGHT -> R.string.ui_field_weight
    SetField.REPS -> R.string.ui_field_reps
    SetField.DURATION -> R.string.ui_field_duration
    SetField.DISTANCE -> R.string.ui_field_distance
}
