package io.github.amibhai.liftlog.core.domain.model

/** A library entry kept in memory for searching, browsing and suggestions (SPEC §4). */
data class CatalogExercise(
    val id: ExerciseId,
    val name: String,
    val aliases: List<String> = emptyList(),
    /** Normalized abbreviations and group words ("ohp", "chest"). */
    val searchTerms: List<String> = emptyList(),
    val type: ExerciseType,
    val equipment: Equipment,
    val primaryMuscles: List<Muscle> = emptyList(),
    val thumbnailUrl: String? = null,
    /** 1 = most popular; null for unranked. */
    val popularityRank: Int? = null,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
)
