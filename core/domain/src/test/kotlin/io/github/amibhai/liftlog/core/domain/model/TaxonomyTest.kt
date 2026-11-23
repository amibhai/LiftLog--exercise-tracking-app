package io.github.amibhai.liftlog.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * The enum names are a contract with the exercise pipeline (tools/exercise-pipeline/pipeline/taxonomy.py)
 * and with rows already stored on the phone. Renaming a value needs a migration.
 */
class TaxonomyTest {

    @Test
    fun `muscles are the fixed SPEC taxonomy`() {
        assertThat(Muscle.entries.map { it.name }).containsExactly(
            "CHEST", "FRONT_DELTS", "SIDE_DELTS", "REAR_DELTS", "BICEPS", "TRICEPS", "FOREARMS",
            "LATS", "UPPER_BACK", "LOWER_BACK", "ABS", "OBLIQUES", "GLUTES", "QUADS", "HAMSTRINGS",
            "ADDUCTORS", "ABDUCTORS", "CALVES", "NECK",
        ).inOrder()
    }

    @Test
    fun `equipment is the fixed SPEC taxonomy`() {
        assertThat(Equipment.entries.map { it.name }).containsExactly(
            "BARBELL", "DUMBBELL", "KETTLEBELL", "MACHINE", "CABLE", "SMITH", "BODYWEIGHT", "BAND",
            "EZ_BAR", "TRAP_BAR", "PLATE", "OTHER",
        ).inOrder()
    }

    @Test
    fun `categories, mechanics and set types are stable`() {
        assertThat(ExerciseCategory.entries.map { it.name })
            .containsExactly("STRENGTH", "OLYMPIC", "PLYOMETRIC", "STRONGMAN", "CARDIO", "MOBILITY").inOrder()
        assertThat(Mechanic.entries.map { it.name }).containsExactly("COMPOUND", "ISOLATION").inOrder()
        assertThat(SetType.entries.map { it.name }).containsExactly("WARMUP", "WORKING", "DROP", "FAILURE").inOrder()
    }

    @Test
    fun `each exercise type decides which fields a set row shows`() {
        assertThat(ExerciseType.entries.associateWith { it.fields }).containsExactly(
            ExerciseType.WEIGHT_REPS, listOf(SetField.WEIGHT, SetField.REPS),
            ExerciseType.BODYWEIGHT_ADDED, listOf(SetField.WEIGHT, SetField.REPS),
            ExerciseType.BODYWEIGHT_ASSISTED, listOf(SetField.WEIGHT, SetField.REPS),
            ExerciseType.REPS_ONLY, listOf(SetField.REPS),
            ExerciseType.DURATION, listOf(SetField.DURATION),
            ExerciseType.DISTANCE_DURATION, listOf(SetField.DISTANCE, SetField.DURATION),
            ExerciseType.WEIGHT_DISTANCE, listOf(SetField.WEIGHT, SetField.DISTANCE),
        ).inOrder()
    }

    @Test
    fun `assisted bodyweight subtracts weight and added bodyweight adds it`() {
        assertThat(ExerciseType.BODYWEIGHT_ASSISTED.weightMeaning).isEqualTo(WeightMeaning.ASSISTANCE)
        assertThat(ExerciseType.BODYWEIGHT_ADDED.weightMeaning).isEqualTo(WeightMeaning.ADDED)
        assertThat(ExerciseType.WEIGHT_REPS.weightMeaning).isEqualTo(WeightMeaning.LOAD)
        assertThat(ExerciseType.REPS_ONLY.weightMeaning).isEqualTo(WeightMeaning.NONE)
    }

    @Test
    fun `only warm-up sets are excluded from records and volume`() {
        assertThat(SetType.entries.filterNot { it.countsTowardRecords }).containsExactly(SetType.WARMUP)
    }
}
