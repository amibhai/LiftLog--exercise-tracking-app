package io.github.amibhai.liftlog.core.domain.suggest

import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseUsage
import io.github.amibhai.liftlog.core.domain.model.Muscle

/** What the exercise picker shows before anything is typed (SPEC §4): never the whole library. */
data class PickerSuggestions(
    val frequent: List<ExerciseId> = emptyList(),
    val goesWith: List<ExerciseId> = emptyList(),
    val recent: List<ExerciseId> = emptyList(),
    val favorites: List<ExerciseId> = emptyList(),
    /** Popular compound lifts, for someone without any history. */
    val starters: List<ExerciseId> = emptyList(),
) {
    companion object {
        private const val FREQUENT = 8
        private const val GOES_WITH = 5
        private const val RECENT = 5
        private const val STARTERS = 12

        /** [current] are the exercises already in the workout or routine; they are not suggested again. */
        fun build(
            catalog: List<CatalogExercise>,
            usages: List<ExerciseUsage>,
            current: Set<ExerciseId>,
            now: Long,
        ): PickerSuggestions {
            val available = catalog.map { it.id }.toSet() - current
            val frequent = top(Frecency.scores(usages, now), available, FREQUENT)
            val goesWith = top(GoesWith.scores(current, usages, now), available, GOES_WITH)
            val recent = usages.sortedByDescending { it.at }.map { it.exerciseId }.distinct()
                .filter { it in available && it !in frequent && it !in goesWith }
                .take(RECENT)
            val shown = (frequent + goesWith + recent).toSet()
            return PickerSuggestions(
                frequent = frequent,
                goesWith = goesWith,
                recent = recent,
                favorites = catalog.filter { it.isFavorite && it.id in available && it.id !in shown }
                    .sortedBy { it.name.lowercase() }
                    .map { it.id },
                starters = if (usages.isEmpty()) starters(catalog, available) else emptyList(),
            )
        }

        private fun top(scores: Map<ExerciseId, Double>, available: Set<ExerciseId>, count: Int): List<ExerciseId> =
            scores.entries.filter { it.key in available }.sortedByDescending { it.value }.take(count).map { it.key }

        private fun starters(catalog: List<CatalogExercise>, available: Set<ExerciseId>): List<ExerciseId> = catalog
            .filter { (it.popularityRank ?: Int.MAX_VALUE) <= STARTERS && it.id in available }
            .sortedBy { it.popularityRank }
            .map { it.id }
    }
}

/** A filtered part of the library: what I do most first, then the well-known ones, then by name. */
fun browse(
    catalog: List<CatalogExercise>,
    muscle: Muscle?,
    equipment: Equipment?,
    frecency: Map<ExerciseId, Double>,
): List<CatalogExercise> = catalog
    .filter { (muscle == null || muscle in it.primaryMuscles) && (equipment == null || it.equipment == equipment) }
    .sortedWith(mostUsedFirst(frecency))

/** The order of equal matches everywhere in the picker. */
internal fun mostUsedFirst(frecency: Map<ExerciseId, Double>): Comparator<CatalogExercise> =
    compareByDescending<CatalogExercise> { frecency[it.id] ?: 0.0 }
        .thenBy { it.popularityRank ?: Int.MAX_VALUE }
        .thenBy { it.name.lowercase() }
