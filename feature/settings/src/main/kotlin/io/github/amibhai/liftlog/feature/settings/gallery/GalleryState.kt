package io.github.amibhai.liftlog.feature.settings.gallery

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.NumberInputState
import io.github.amibhai.liftlog.core.ui.component.NumberPadKey
import io.github.amibhai.liftlog.core.ui.component.RestTimerUiState
import io.github.amibhai.liftlog.core.ui.component.SetRowAction
import java.math.BigDecimal

/** One demo set. Weight is canonical kg, exactly like the real database (SPEC §2). */
@Immutable
internal data class DemoSet(
    val id: String,
    val type: SetType = SetType.WORKING,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val seconds: Int? = null,
    val isGhost: Boolean = true,
    val isCompleted: Boolean = false,
)

@Immutable
internal data class DemoExercise(
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val sets: List<DemoSet>,
    /** Last time's sets, by index, for the Previous column and "Same as previous". */
    val previous: List<DemoSet>,
)

internal data class FieldRef(val exercise: Int, val set: Int, val field: SetField)

/**
 * State of the component gallery: a throw-away workout for trying set rows, the number pad and
 * the rest timer on a phone. Pure, so the interaction rules are unit-tested.
 */
@Immutable
internal data class GalleryState(
    val unit: WeightUnit = WeightUnit.KG,
    val exercises: List<DemoExercise> = demoExercises(),
    val editing: FieldRef? = null,
    val input: NumberInputState? = null,
    val restTimer: RestTimerUiState? = null,
) {
    fun withUnit(newUnit: WeightUnit) = copy(unit = newUnit, editing = null, input = null)

    fun handle(action: SetRowAction, exercise: Int, set: Int, nowMillis: Long): GalleryState = when (action) {
        is SetRowAction.EditField -> startEditing(FieldRef(exercise, set, action.field))

        is SetRowAction.ToggleComplete -> toggleComplete(exercise, set, nowMillis)

        is SetRowAction.CycleType -> updateSet(exercise, set) { it.copy(type = it.type.next()) }

        // The number pad edits by position, so it closes when rows are added or removed.
        is SetRowAction.Duplicate -> updateSets(exercise) { sets ->
            val copy = sets[set].copy(id = sets.unusedId(sets[set].id), isCompleted = false)
            sets.take(set + 1) + copy + sets.drop(set + 1)
        }.copy(editing = null, input = null)

        is SetRowAction.Delete -> updateSets(exercise) { sets -> sets.filterIndexed { index, _ -> index != set } }
            .copy(editing = null, input = null)
    }

    fun startEditing(ref: FieldRef): GalleryState = copy(
        editing = ref,
        input = NumberInputState.start(displayValue(set(ref), ref.field), maxDecimals(ref.field), MAX_INPUT),
    )

    fun press(key: NumberPadKey): GalleryState {
        val ref = editing
        val current = input
        if (ref == null || current == null) return this
        return when (key) {
            NumberPadKey.Hide -> copy(editing = null, input = null)

            NumberPadKey.Next -> nextField(ref)?.let { startEditing(it) } ?: copy(editing = null, input = null)

            else -> {
                val next = current.reduce(key, step(ref), previousValue(ref))
                copy(input = next).write(ref, next.value)
            }
        }
    }

    fun adjustRest(seconds: Int): GalleryState = copy(
        restTimer = restTimer?.let {
            it.copy(
                endsAtMillis = it.endsAtMillis + seconds * MILLIS_PER_SECOND,
                totalSeconds = (it.totalSeconds + seconds).coerceAtLeast(1),
            )
        },
    )

    fun skipRest(): GalleryState = copy(restTimer = null)

    fun set(ref: FieldRef): DemoSet = exercises[ref.exercise].sets[ref.set]

    /** Value shown for [field] in the current unit; null when empty. */
    fun displayValue(set: DemoSet, field: SetField): BigDecimal? = when (field) {
        SetField.WEIGHT -> set.weightKg?.let { Weight(it).displayValue(unit) }
        SetField.REPS -> set.reps?.toBigDecimal()
        SetField.DURATION -> set.seconds?.toBigDecimal()
        SetField.DISTANCE -> null
    }

    fun step(ref: FieldRef): BigDecimal = when (ref.field) {
        SetField.WEIGHT -> BigDecimal.valueOf(UserSettings().weightStep(exercises[ref.exercise].equipment, unit))
        SetField.REPS -> BigDecimal.ONE
        SetField.DURATION -> DURATION_STEP_SECONDS.toBigDecimal()
        SetField.DISTANCE -> BigDecimal.ONE
    }

    fun previousValue(ref: FieldRef): BigDecimal? =
        exercises[ref.exercise].previous.getOrNull(ref.set)?.let { displayValue(it, ref.field) }

    fun nextField(ref: FieldRef): FieldRef? {
        val fields = exercises[ref.exercise].type.fields
        val index = fields.indexOf(ref.field)
        return when {
            index < fields.lastIndex -> ref.copy(field = fields[index + 1])
            ref.set < exercises[ref.exercise].sets.lastIndex -> FieldRef(ref.exercise, ref.set + 1, fields.first())
            else -> null
        }
    }

    private fun toggleComplete(exercise: Int, set: Int, nowMillis: Long): GalleryState {
        val updated = updateSet(exercise, set) { it.copy(isCompleted = !it.isCompleted, isGhost = false) }
        val completed = updated.exercises[exercise].sets[set].isCompleted
        val rest = RestTimerUiState(nowMillis + REST_SECONDS * MILLIS_PER_SECOND, REST_SECONDS)
        return if (completed) updated.copy(restTimer = rest, editing = null, input = null) else updated
    }

    private fun write(ref: FieldRef, value: BigDecimal?): GalleryState = updateSet(ref.exercise, ref.set) { set ->
        when (ref.field) {
            SetField.WEIGHT -> set.copy(weightKg = value?.let { Weight.of(it, unit).kg }, isGhost = false)
            SetField.REPS -> set.copy(reps = value?.toInt(), isGhost = false)
            SetField.DURATION -> set.copy(seconds = value?.toInt(), isGhost = false)
            SetField.DISTANCE -> set
        }
    }

    private fun updateSet(exercise: Int, set: Int, transform: (DemoSet) -> DemoSet) = updateSets(exercise) { sets ->
        sets.mapIndexed { index, demoSet -> if (index == set) transform(demoSet) else demoSet }
    }

    private fun updateSets(exercise: Int, transform: (List<DemoSet>) -> List<DemoSet>): GalleryState {
        val updated = exercises.mapIndexed { index, demo ->
            if (index == exercise) demo.copy(sets = transform(demo.sets)) else demo
        }
        return copy(exercises = updated)
    }

    private fun maxDecimals(field: SetField) = if (field == SetField.WEIGHT) Weight.DISPLAY_DECIMALS else 0

    companion object {
        const val REST_SECONDS = 120
        private const val MILLIS_PER_SECOND = 1_000L
        private const val DURATION_STEP_SECONDS = 5
        private val MAX_INPUT = BigDecimal("9999")
    }
}

private fun List<DemoSet>.unusedId(base: String): String =
    generateSequence(1) { it + 1 }.map { "$base-$it" }.first { id -> none { it.id == id } }

private fun SetType.next(): SetType = when (this) {
    SetType.WORKING -> SetType.WARMUP
    SetType.WARMUP -> SetType.DROP
    SetType.DROP -> SetType.FAILURE
    SetType.FAILURE -> SetType.WORKING
}

/** Demo data only; real exercises come from the library. */
internal fun demoExercises(): List<DemoExercise> = listOf(
    DemoExercise(
        name = "Barbell Bench Press",
        type = ExerciseType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        sets = listOf(
            DemoSet("bench-w", SetType.WARMUP, weightKg = 40.0, reps = 10, isGhost = false, isCompleted = true),
            DemoSet("bench-1", weightKg = 80.0, reps = 8),
            DemoSet("bench-2", weightKg = 80.0, reps = 8),
            DemoSet("bench-3", weightKg = 80.0, reps = 8),
        ),
        previous = listOf(
            DemoSet("bench-prev-w", SetType.WARMUP, weightKg = 40.0, reps = 10),
            DemoSet("bench-prev-1", weightKg = 77.5, reps = 8),
            DemoSet("bench-prev-2", weightKg = 77.5, reps = 8),
            DemoSet("bench-prev-3", weightKg = 77.5, reps = 7),
        ),
    ),
    DemoExercise(
        name = "Pull-Up",
        type = ExerciseType.BODYWEIGHT_ADDED,
        equipment = Equipment.BODYWEIGHT,
        sets = listOf(DemoSet("pullup-1", weightKg = 10.0, reps = 6), DemoSet("pullup-2", weightKg = 10.0, reps = 6)),
        previous = listOf(
            DemoSet("pullup-prev-1", weightKg = 7.5, reps = 6),
            DemoSet("pullup-prev-2", weightKg = 7.5, reps = 5),
        ),
    ),
    DemoExercise(
        name = "Plank",
        type = ExerciseType.DURATION,
        equipment = Equipment.BODYWEIGHT,
        sets = listOf(DemoSet("plank-1", seconds = 60), DemoSet("plank-2", seconds = 60)),
        previous = listOf(DemoSet("plank-prev-1", seconds = 45), DemoSet("plank-prev-2", seconds = 45)),
    ),
)
