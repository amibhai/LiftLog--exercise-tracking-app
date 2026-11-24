package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseId

/** Changes to exercises. Built-in exercises can be edited and hidden, but never deleted (SPEC §3). */
interface ExerciseEditor {
    suspend fun save(draft: ExerciseDraft): ExerciseId

    /** Only custom exercises; their past sets stay in the history. */
    suspend fun deleteCustom(id: ExerciseId)

    suspend fun setHidden(id: ExerciseId, hidden: Boolean)

    suspend fun setFavorite(id: ExerciseId, favorite: Boolean)

    suspend fun setStickyNote(id: ExerciseId, note: String?)

    suspend fun setDefaults(id: ExerciseId, defaults: ExerciseDefaults)
}
