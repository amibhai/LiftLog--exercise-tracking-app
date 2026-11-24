package io.github.amibhai.liftlog.core.domain.search

import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.Muscle

/**
 * Hidden search terms of custom and edited exercises, so that "db chest" finds them like built-in ones.
 * Mirrors EQUIPMENT_TERMS and MUSCLE_TERMS in tools/exercise-pipeline/pipeline/aliases.py.
 */
object SearchTerms {

    fun of(equipment: Equipment, primaryMuscles: List<Muscle>): List<String> =
        (equipment.terms() + primaryMuscles.flatMap { it.terms() }).distinct().sorted()

    private fun Equipment.terms(): List<String> = when (this) {
        Equipment.BARBELL -> listOf("bb", "barbell")
        Equipment.DUMBBELL -> listOf("db", "dumbbell", "dumbbells")
        Equipment.KETTLEBELL -> listOf("kb", "kettlebell")
        Equipment.MACHINE -> listOf("machine")
        Equipment.CABLE -> listOf("cable")
        Equipment.SMITH -> listOf("smith", "smith machine")
        Equipment.BODYWEIGHT -> listOf("bodyweight", "bw")
        Equipment.BAND -> listOf("band", "banded")
        Equipment.EZ_BAR -> listOf("ez", "ez bar", "ezbar", "ez curl bar")
        Equipment.TRAP_BAR -> listOf("trap bar", "hex bar")
        Equipment.PLATE -> listOf("plate")
        Equipment.OTHER -> emptyList()
    }

    private fun Muscle.terms(): List<String> = when (this) {
        Muscle.CHEST -> listOf("chest", "pecs")
        Muscle.FRONT_DELTS -> listOf("shoulders", "delts", "front delts")
        Muscle.SIDE_DELTS -> listOf("shoulders", "delts", "side delts")
        Muscle.REAR_DELTS -> listOf("shoulders", "delts", "rear delts")
        Muscle.BICEPS -> listOf("biceps", "bicep")
        Muscle.TRICEPS -> listOf("triceps", "tricep")
        Muscle.FOREARMS -> listOf("forearms", "grip")
        Muscle.LATS -> listOf("lats", "back")
        Muscle.UPPER_BACK -> listOf("upper back", "traps", "back")
        Muscle.LOWER_BACK -> listOf("lower back", "back")
        Muscle.ABS -> listOf("abs", "core")
        Muscle.OBLIQUES -> listOf("obliques", "core")
        Muscle.GLUTES -> listOf("glutes")
        Muscle.QUADS -> listOf("quads", "legs")
        Muscle.HAMSTRINGS -> listOf("hamstrings", "hams", "legs")
        Muscle.ADDUCTORS -> listOf("adductors", "inner thigh")
        Muscle.ABDUCTORS -> listOf("abductors", "outer thigh")
        Muscle.CALVES -> listOf("calves", "calf")
        Muscle.NECK -> listOf("neck")
    }
}
