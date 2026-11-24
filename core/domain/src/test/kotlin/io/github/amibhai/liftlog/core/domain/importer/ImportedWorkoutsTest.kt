package io.github.amibhai.liftlog.core.domain.importer

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.workout.info
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ImportedWorkoutsTest {

    private val start = LocalDateTime.of(2025, 1, 1, 10, 0)
    private val startMillis = start.toInstant(ZoneOffset.UTC).toEpochMilli()
    private val squat = info("Squat")
    private val plank = info("Plank", ExerciseType.DURATION)
    private val exercises = mapOf("Squat (Barbell)" to squat, "Plank" to plank, "Row" to info("Row"))

    @Test
    fun `sets become completed sets in the unit of the file`() {
        val log = log(
            exercise(
                "Squat (Barbell)",
                ImportedSet(SetType.WARMUP, weight = 135.0, reps = 5),
                ImportedSet(weight = 225.0, reps = 5, rpe = 8.5),
            ),
        )

        val workout = log.toWorkouts(exercises, WeightUnit.LB, ZoneOffset.UTC).single()

        assertThat(workout.isInProgress).isFalse()
        assertThat(workout.startedAt).isEqualTo(startMillis)
        val sets = workout.exercises.single().sets
        assertThat(workout.exercises.single().exercise).isEqualTo(squat)
        assertThat(sets.map { it.entered.weight?.displayText(WeightUnit.LB) }).containsExactly("135", "225").inOrder()
        assertThat(sets.all { it.isCompleted }).isTrue()
        assertThat(sets.map { it.type }).containsExactly(SetType.WARMUP, SetType.WORKING).inOrder()
        assertThat(sets.last().rpe).isEqualTo(8.5)
        assertThat(sets.first().completedAt!!).isGreaterThan(startMillis)
        assertThat(sets.last().completedAt!!).isLessThan(workout.endedAt!!)
    }

    @Test
    fun `importing the same file again gives the same workouts`() {
        val log = log(exercise("Row", ImportedSet(weight = 60.0, reps = 8)))

        val first = log.toWorkouts(exercises, WeightUnit.KG, ZoneOffset.UTC)
        val second = log.toWorkouts(exercises, WeightUnit.KG, ZoneOffset.UTC)

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `exercises without a match and sets without numbers are left out`() {
        val log = log(
            exercise("Unknown", ImportedSet(weight = 10.0, reps = 10)),
            exercise("Plank", ImportedSet(reps = 10), ImportedSet(seconds = 60)),
            exercise("Row", ImportedSet(weight = 60.0)),
        )

        val workout = log.toWorkouts(exercises, WeightUnit.KG, ZoneOffset.UTC).single()

        assertThat(workout.exercises.map { it.exercise.name }).containsExactly("Plank")
        assertThat(workout.exercises.single().sets.single().entered.durationSeconds).isEqualTo(60)
        assertThat(log.toWorkouts(emptyMap(), WeightUnit.KG, ZoneOffset.UTC)).isEmpty()
    }

    @Test
    fun `a lift without a weight was done without one`() {
        val log = log(exercise("Squat (Barbell)", ImportedSet(reps = 20)))

        val set = log.toWorkouts(exercises, WeightUnit.KG, ZoneOffset.UTC).single().exercises.single().sets.single()

        assertThat(set.entered.weight).isEqualTo(Weight.ZERO)
    }

    @Test
    fun `neighbours in a superset stay one`() {
        val log = log(
            exercise("Row", ImportedSet(weight = 60.0, reps = 8), superset = "a"),
            exercise("Squat (Barbell)", ImportedSet(weight = 100.0, reps = 8), superset = "a"),
            exercise("Plank", ImportedSet(seconds = 60), superset = "b"),
        )

        val workout = log.toWorkouts(exercises, WeightUnit.KG, ZoneOffset.UTC).single()

        assertThat(workout.exercises.map { it.supersetGroup }).containsExactly(1, 1, null).inOrder()
    }

    @Test
    fun `distances are guessed when the file does not say the unit`() {
        val run = mapOf("Running" to info("Running", ExerciseType.DISTANCE_DURATION))
        val meters = log(exercise("Running", ImportedSet(distance = 5_000.0, seconds = 1_500)))
        val miles = log(exercise("Running", ImportedSet(distance = 3.0, seconds = 1_500)))

        assertThat(distance(meters, run, WeightUnit.KG)).isEqualTo(5_000.0)
        assertThat(distance(miles, run, WeightUnit.KG)).isEqualTo(3_000.0)
        assertThat(distance(miles, run, WeightUnit.LB)).isWithin(0.01).of(4_828.03)
    }

    @Test
    fun `a custom exercise is drafted from its sets`() {
        val log = log(
            exercise("Sled Push (Machine)", ImportedSet(weight = 100.0, distance = 20.0)),
            exercise("Jumping Jacks", ImportedSet(reps = 50)),
            exercise("Wall Sit", ImportedSet(seconds = 60)),
            exercise("Odd Press", ImportedSet(weight = 20.0, reps = 10)),
        )

        assertThat(log.draftFor("Sled Push (Machine)").type).isEqualTo(ExerciseType.WEIGHT_DISTANCE)
        assertThat(log.draftFor("Sled Push (Machine)").equipment).isEqualTo(Equipment.MACHINE)
        assertThat(log.draftFor("Jumping Jacks").type).isEqualTo(ExerciseType.REPS_ONLY)
        assertThat(log.draftFor("Jumping Jacks").equipment).isEqualTo(Equipment.BODYWEIGHT)
        assertThat(log.draftFor("Wall Sit").type).isEqualTo(ExerciseType.DURATION)
        assertThat(log.draftFor("Odd Press").type).isEqualTo(ExerciseType.WEIGHT_REPS)
    }

    @Test
    fun `names are matched by their words and their equipment`() {
        val matcher = ExerciseMatcher(
            listOf(
                catalog("Barbell Bench Press", Equipment.BARBELL, rank = 1),
                catalog("Dumbbell Bench Press", Equipment.DUMBBELL, rank = 2),
                catalog("Wide-Grip Lat Pulldown", Equipment.CABLE, aliases = listOf("Lat Pulldown")),
                catalog("Pull-Up", Equipment.BODYWEIGHT, type = ExerciseType.BODYWEIGHT_ADDED),
                catalog("Band Assisted Pull-Up", Equipment.BAND, type = ExerciseType.BODYWEIGHT_ASSISTED),
            ),
        )

        assertThat(matcher.match("Bench Press (Barbell)")?.exercise?.name).isEqualTo("Barbell Bench Press")
        assertThat(matcher.match("Bench Press (Barbell)")?.isSure).isTrue()
        assertThat(matcher.match("DB Bench Press")?.exercise?.name).isEqualTo("Dumbbell Bench Press")
        assertThat(matcher.match("Bench Press")?.exercise?.name).isEqualTo("Barbell Bench Press")
        assertThat(matcher.match("Press")?.isSure).isFalse()
        assertThat(matcher.match("Lat Pulldown (Cable)")?.isSure).isTrue()
        assertThat(matcher.match("Pullups")?.exercise?.name).isEqualTo("Pull-Up")
        assertThat(matcher.match("Pull Up (Weighted)")?.exercise?.name).isEqualTo("Pull-Up")
        assertThat(matcher.match("Pull Up (Assisted)")?.exercise?.name).isEqualTo("Band Assisted Pull-Up")
        assertThat(matcher.match("Underwater Basket Weaving")).isNull()
    }

    private fun distance(log: ImportedLog, exercises: Map<String, ExerciseInfo>, unit: WeightUnit) =
        log.toWorkouts(exercises, unit, ZoneOffset.UTC).single().exercises.single().sets.single().entered.distanceMeters

    private fun log(vararg exercises: ImportedExercise) = ImportedLog(
        source = LogSource.STRONG,
        unit = null,
        workouts = listOf(ImportedWorkout("A", start, start.plusHours(1), notes = null, exercises.toList())),
        unreadableRows = 0,
    )

    private fun exercise(name: String, vararg sets: ImportedSet, superset: String? = null) =
        ImportedExercise(name, superset, notes = null, sets.toList())

    private fun catalog(
        name: String,
        equipment: Equipment,
        aliases: List<String> = emptyList(),
        type: ExerciseType = ExerciseType.WEIGHT_REPS,
        rank: Int? = null,
    ) = CatalogExercise(
        id = ExerciseId(name),
        name = name,
        aliases = aliases,
        type = type,
        equipment = equipment,
        popularityRank = rank,
    )
}
