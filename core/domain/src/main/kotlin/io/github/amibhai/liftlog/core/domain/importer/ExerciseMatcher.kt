package io.github.amibhai.liftlog.core.domain.importer

import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.search.SearchText

/** The exercise a name of another app stands for; not [isSure] when somebody should have a look. */
data class ExerciseMatch(val exercise: CatalogExercise, val isSure: Boolean)

/**
 * Finds the exercises of the library behind names like "Bench Press (Barbell)" (SPEC §9): by the words
 * they have in common, by the equipment, which other apps put in parentheses, and by what was logged.
 */
class ExerciseMatcher(exercises: List<CatalogExercise>) {

    private class Candidate(val exercise: CatalogExercise) {
        val names: List<Set<String>> = (listOf(exercise.name) + exercise.aliases).map(::wordsOf)
        val equipment: Set<String> = exercise.equipment.words
        val isWellKnown: Boolean = exercise.popularityRank != null
    }

    private class Scored(val candidate: Candidate, val score: Double) {
        /** A well-known lift is meant more often than its rare namesakes. */
        val rank: Double get() = if (candidate.isWellKnown) score + WELL_KNOWN else score
        val isSure: Boolean get() = score >= if (candidate.isWellKnown) SURE_IF_WELL_KNOWN else SURE
    }

    private val candidates = exercises.map(::Candidate)

    /** An exercise that cannot hold what was [loggedAs] is no match: its sets would be lost. */
    fun match(name: String, loggedAs: ExerciseType? = null): ExerciseMatch? {
        val words = wordsOf(name)
        val equipment = equipmentIn(name)
        val best = candidates
            .filter { loggedAs == null || it.exercise.type.fields.containsAll(loggedAs.fields) }
            .map { Scored(it, score(words, equipment, it)) }
            .filter { it.score >= POSSIBLE }
            .maxWithOrNull(
                compareBy<Scored> { it.rank }
                    // Of equally good ones the better known, and at last the shorter name.
                    .thenByDescending { it.candidate.exercise.popularityRank ?: Int.MAX_VALUE }
                    .thenByDescending { it.candidate.exercise.name.length },
            )
        return best?.let { ExerciseMatch(it.candidate.exercise, it.isSure) }
    }

    private fun score(words: Set<String>, equipment: Equipment?, candidate: Candidate): Double {
        val type = candidate.exercise.type
        val meant = if (type == ExerciseType.BODYWEIGHT_ADDED) words - "weighted" else words
        val likeness = candidate.names.maxOf { name ->
            // The equipment may be in the name or not; it counts only when it was asked for.
            val all = name + candidate.equipment.intersect(meant)
            2.0 * all.intersect(meant).size / (all.size + meant.size)
        }
        val isAssisted = type == ExerciseType.BODYWEIGHT_ASSISTED
        return likeness +
            (if (equipment != null && equipment != candidate.exercise.equipment) OTHER_EQUIPMENT else 0.0) +
            (if (isAssisted != ("assisted" in words)) OTHER_KIND else 0.0)
    }

    private companion object {
        const val SURE = 0.9
        const val SURE_IF_WELL_KNOWN = 0.8
        const val POSSIBLE = 0.5
        const val WELL_KNOWN = 0.2
        const val OTHER_EQUIPMENT = -0.2
        const val OTHER_KIND = -0.3
    }
}

private const val SHORTEST_PLURAL = 3
private val Ignored = setOf("the", "a", "with", "on", "and", "of", "to", "in", "for")
private val Parentheses = Regex("\\(([^)]*)\\)")

/** Other spellings of the same words. */
private val Spellings = mapOf(
    "db" to "dumbbell",
    "bb" to "barbell",
    "kb" to "kettlebell",
    "pullup" to "pull up",
    "chinup" to "chin up",
    "pushup" to "push up",
    "situp" to "sit up",
    "pulldown" to "pull down",
    "pushdown" to "push down",
    "pullover" to "pull over",
    "flye" to "fly",
    "flie" to "fly",
    "running" to "run",
    "jogging" to "jog",
    "walking" to "walk",
    "cycling" to "cycle",
    "biking" to "bike",
)

private fun wordsOf(name: String): Set<String> = SearchText.tokens(name)
    .filter { it !in Ignored }
    .map(::singular)
    .flatMap { (Spellings[it] ?: it).split(' ') }
    .toSet()

/** "squats" is "squat", but "abs" and "press" stay. Both sides are treated alike, so it need not be good grammar. */
private fun singular(word: String): String =
    if (word.length > SHORTEST_PLURAL && word.endsWith('s') && !word.endsWith("ss")) word.dropLast(1) else word

/** The equipment in parentheses: "(Smith Machine)". */
internal fun equipmentIn(name: String): Equipment? {
    val words = Parentheses.findAll(name).flatMap { SearchText.tokens(it.groupValues[1]) }.toSet()
    return when {
        words.isEmpty() -> null
        "smith" in words -> Equipment.SMITH
        "ez" in words -> Equipment.EZ_BAR
        "trap" in words || "hex" in words -> Equipment.TRAP_BAR
        else -> SimpleEquipment.firstNotNullOfOrNull { (word, equipment) -> equipment.takeIf { word in words } }
    }
}

private val SimpleEquipment = listOf(
    "barbell" to Equipment.BARBELL,
    "dumbbell" to Equipment.DUMBBELL,
    "kettlebell" to Equipment.KETTLEBELL,
    "cable" to Equipment.CABLE,
    "machine" to Equipment.MACHINE,
    "band" to Equipment.BAND,
    "plate" to Equipment.PLATE,
    "bodyweight" to Equipment.BODYWEIGHT,
)

private val Equipment.words: Set<String>
    get() = when (this) {
        Equipment.SMITH -> setOf("smith", "machine")
        Equipment.EZ_BAR -> setOf("ez", "bar")
        Equipment.TRAP_BAR -> setOf("trap", "bar", "hex")
        else -> SimpleEquipment.filter { it.second == this }.map { it.first }.toSet()
    }
