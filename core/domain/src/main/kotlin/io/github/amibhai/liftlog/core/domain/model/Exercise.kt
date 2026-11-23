package io.github.amibhai.liftlog.core.domain.model

/** What a search result or list row needs. */
data class ExerciseSummary(
    val id: ExerciseId,
    val name: String,
    val type: ExerciseType,
    val equipment: Equipment,
    val primaryMuscle: Muscle?,
    val thumbnailUrl: String?,
    val isCustom: Boolean,
)

/** Everything the exercise detail screen shows. */
data class ExerciseDetail(
    val id: ExerciseId,
    val name: String,
    val aliases: List<String>,
    val type: ExerciseType,
    val equipment: Equipment,
    val category: ExerciseCategory,
    val mechanic: Mechanic?,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val instructions: List<String>,
    val images: List<ExerciseImage>,
    val isUnilateral: Boolean,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val notes: String?,
    val attribution: Attribution?,
)

/** A remote image; loaded lazily and cached on disk (SPEC §3). */
data class ExerciseImage(val url: String, val thumbnailUrl: String?, val license: String?, val author: String?)

/** Who to credit for a built-in exercise's text and data. */
data class Attribution(val sourceName: String, val license: String?, val licenseUrl: String?, val author: String?)
