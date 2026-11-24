package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.runtime.Immutable
import io.github.amibhai.liftlog.core.domain.model.SetField
import io.github.amibhai.liftlog.core.domain.model.SetType

/**
 * Everything one set row shows, already formatted. Immutable, so a row only recomposes when its
 * own set changes (SPEC §1: set rows must not recompose unnecessarily).
 */
@Immutable
data class SetRowUiModel(
    val id: String,
    val type: SetType,
    /** 1-based number among working sets; shown in the badge. */
    val workingSetNumber: Int?,
    /** What was done last time for this set, e.g. "80 × 8"; null when there is no history. */
    val previous: String?,
    /** In display order; which fields exist depends on the exercise type. */
    val fields: List<SetFieldUiModel>,
    val isCompleted: Boolean,
)

@Immutable
data class SetFieldUiModel(
    val field: SetField,
    val text: String,
    val unit: String?,
    /** A pre-filled value the user has not touched; completing the set accepts it. */
    val isGhost: Boolean,
    val isFocused: Boolean = false,
    /** A small remark above the value: "+2.5 kg", "RPE 8.5". */
    val note: String? = null,
)

sealed interface SetRowAction {
    val setId: String

    data class EditField(override val setId: String, val field: SetField) : SetRowAction

    data class ToggleComplete(override val setId: String) : SetRowAction

    data class ChangeType(override val setId: String, val type: SetType) : SetRowAction

    data class UsePrevious(override val setId: String) : SetRowAction

    data class Duplicate(override val setId: String) : SetRowAction

    data class Delete(override val setId: String) : SetRowAction
}
