package io.github.amibhai.liftlog.core.domain.model

/** A data source of the built-in exercise library, listed on the About screen. */
data class LibrarySource(
    val name: String,
    val url: String,
    val license: String,
    val licenseUrl: String?,
    val revision: String?,
    val notice: String?,
    val exerciseCount: Int,
)

data class LibraryStats(val totalExercises: Int, val customExercises: Int)
