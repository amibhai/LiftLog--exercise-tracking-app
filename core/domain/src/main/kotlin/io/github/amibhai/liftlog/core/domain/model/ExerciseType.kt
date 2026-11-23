package io.github.amibhai.liftlog.core.domain.model

/** An input a set row can show. */
enum class SetField { WEIGHT, REPS, DURATION, DISTANCE }

/** How the weight field of a set is interpreted for an [ExerciseType]. */
enum class WeightMeaning {
    /** The exercise has no weight field. */
    NONE,

    /** The external load lifted. */
    LOAD,

    /** Added on top of body weight (weighted pull-ups, dips). */
    ADDED,

    /** Assistance subtracted from body weight (assisted pull-up machine). */
    ASSISTANCE,
}

/** Decides which fields a set row shows (SPEC §3). Names are stored in the database. */
enum class ExerciseType(val fields: List<SetField>, val weightMeaning: WeightMeaning) {
    WEIGHT_REPS(listOf(SetField.WEIGHT, SetField.REPS), WeightMeaning.LOAD),
    BODYWEIGHT_ADDED(listOf(SetField.WEIGHT, SetField.REPS), WeightMeaning.ADDED),
    BODYWEIGHT_ASSISTED(listOf(SetField.WEIGHT, SetField.REPS), WeightMeaning.ASSISTANCE),
    REPS_ONLY(listOf(SetField.REPS), WeightMeaning.NONE),
    DURATION(listOf(SetField.DURATION), WeightMeaning.NONE),
    DISTANCE_DURATION(listOf(SetField.DISTANCE, SetField.DURATION), WeightMeaning.NONE),
    WEIGHT_DISTANCE(listOf(SetField.WEIGHT, SetField.DISTANCE), WeightMeaning.LOAD),
}
