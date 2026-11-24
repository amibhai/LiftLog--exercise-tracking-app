package io.github.amibhai.liftlog.core.domain.importer

import io.github.amibhai.liftlog.core.domain.csv.CsvTable
import io.github.amibhai.liftlog.core.domain.csv.parseCsv
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.units.DistanceUnit
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import java.time.LocalDateTime
import java.util.Locale

/** Reads a CSV export of Strong or Hevy; null when it is neither. */
fun parseLog(text: String): ImportedLog? {
    val table = parseCsv(text)
    val columns = table.columns.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
    return when {
        columns.containsAll(listOf("exercise_title", "start_time")) -> HevyLog(table).read()
        columns.containsAll(listOf("exercise name", "set order", "date")) -> StrongLog(table).read()
        else -> null
    }
}

/** A row of an export; columns are looked up by any of their names, whatever the case. */
internal class LogRow(row: Map<String, String>) {
    private val values = row.entries.associate { (column, value) -> column.lowercase(Locale.ROOT) to value.trim() }

    fun text(vararg columns: String): String? = columns.firstNotNullOfOrNull { column ->
        values[column]?.ifEmpty { null }
    }

    /** Zero counts as nothing: exports fill in zeros where there is no value. */
    fun number(vararg columns: String): Double? = text(*columns)?.let(::decimal)?.takeIf { it > 0 }

    fun whole(vararg columns: String): Int? = number(*columns)?.toInt()?.takeIf { it > 0 }

    /** "27.5" and "27,5" are both in use. */
    private fun decimal(text: String): Double? = (if ('.' in text) text else text.replace(',', '.')).toDoubleOrNull()
}

/** What both exports have in common: one row per set, in order. */
internal abstract class LogReader(private val source: LogSource, private val table: CsvTable) {

    /** The workout a row belongs to; rows with equal keys belong together. */
    protected data class WorkoutKey(val name: String, val startedAt: LocalDateTime)

    private class KeyedRow(val key: WorkoutKey, val exercise: String, val row: LogRow)

    protected abstract val unit: WeightUnit?

    protected abstract fun key(row: LogRow): WorkoutKey?

    protected abstract fun exerciseName(row: LogRow): String?

    protected abstract fun workout(
        key: WorkoutKey,
        rows: List<LogRow>,
        exercises: List<ImportedExercise>,
    ): ImportedWorkout

    protected abstract fun superset(row: LogRow): String?

    protected abstract fun notes(rows: List<LogRow>): String?

    /** Null for rows that are no sets, such as rest timers. */
    protected abstract fun set(row: LogRow): ImportedSet?

    fun read(): ImportedLog {
        val rows = table.rows.map { LogRow(it) }
        val readable = rows.mapNotNull { row ->
            val key = key(row)
            val exercise = exerciseName(row)
            if (key == null || exercise == null) null else KeyedRow(key, exercise, row)
        }
        val workouts = readable.groupBy { it.key }.map { (key, own) ->
            workout(key, own.map { it.row }, exercises(own))
        }
        return ImportedLog(
            source = source,
            unit = unit,
            workouts = workouts.filter { it.exercises.isNotEmpty() }.sortedBy { it.startedAt },
            unreadableRows = rows.size - readable.size,
        )
    }

    /** Neighbouring rows with the same exercise are its sets. */
    private fun exercises(rows: List<KeyedRow>): List<ImportedExercise> {
        val groups = mutableListOf<MutableList<KeyedRow>>()
        for (row in rows) {
            val last = groups.lastOrNull()
            if (last != null && last.first().exercise == row.exercise) last += row else groups += mutableListOf(row)
        }
        return groups.map { group ->
            val own = group.map { it.row }
            ImportedExercise(group.first().exercise, superset(own.first()), notes(own), own.mapNotNull(::set))
        }.filter { it.sets.isNotEmpty() }
    }
}

private class HevyLog(private val table: CsvTable) : LogReader(LogSource.HEVY, table) {

    override val unit: WeightUnit? = when {
        "weight_kg" in table.columns -> WeightUnit.KG
        "weight_lbs" in table.columns -> WeightUnit.LB
        else -> null
    }

    override fun key(row: LogRow): WorkoutKey? =
        row.text("start_time")?.let(::parseLogTime)?.let { WorkoutKey(row.text("title").orEmpty(), it) }

    override fun exerciseName(row: LogRow): String? = row.text("exercise_title")

    override fun workout(key: WorkoutKey, rows: List<LogRow>, exercises: List<ImportedExercise>) = ImportedWorkout(
        name = key.name,
        startedAt = key.startedAt,
        endedAt = rows.first().text("end_time")?.let(::parseLogTime),
        notes = rows.first().text("description"),
        exercises = exercises,
    )

    override fun superset(row: LogRow): String? = row.text("superset_id")

    override fun notes(rows: List<LogRow>): String? = rows.firstNotNullOfOrNull { it.text("exercise_notes") }

    override fun set(row: LogRow) = ImportedSet(
        type = when (row.text("set_type")?.lowercase(Locale.ROOT)) {
            "warmup" -> SetType.WARMUP
            "dropset", "drop_set" -> SetType.DROP
            "failure" -> SetType.FAILURE
            else -> SetType.WORKING
        },
        weight = row.number("weight_kg", "weight_lbs"),
        reps = row.whole("reps"),
        distance = row.number("distance_km", "distance_miles"),
        distanceUnit = if (row.text("distance_miles") != null) DistanceUnit.MILE else DistanceUnit.KILOMETER,
        seconds = row.whole("duration_seconds"),
        rpe = row.number("rpe"),
    )
}

private class StrongLog(private val table: CsvTable) : LogReader(LogSource.STRONG, table) {

    /** Only older exports say it. */
    override val unit: WeightUnit?
        get() = table.rows.firstNotNullOfOrNull { LogRow(it).text("weight unit") }?.let { text ->
            when {
                text.startsWith("kg", ignoreCase = true) -> WeightUnit.KG
                text.startsWith("lb", ignoreCase = true) -> WeightUnit.LB
                else -> null
            }
        }

    override fun key(row: LogRow): WorkoutKey? =
        row.text("date")?.let(::parseLogTime)?.let { WorkoutKey(row.text("workout name").orEmpty(), it) }

    override fun exerciseName(row: LogRow): String? = row.text("exercise name")

    override fun workout(key: WorkoutKey, rows: List<LogRow>, exercises: List<ImportedExercise>) = ImportedWorkout(
        name = key.name,
        startedAt = key.startedAt,
        endedAt = rows.first().text("duration", "workout duration")
            ?.let(::parseLogDuration)
            ?.let { key.startedAt.plusSeconds(it) },
        notes = rows.firstNotNullOfOrNull { it.text("workout notes") },
        exercises = exercises,
    )

    override fun superset(row: LogRow): String? = null

    override fun notes(rows: List<LogRow>): String? = rows.firstNotNullOfOrNull { it.text("notes") }

    override fun set(row: LogRow): ImportedSet? {
        val type = when (row.text("set order")?.uppercase(Locale.ROOT)) {
            "W" -> SetType.WARMUP
            "D" -> SetType.DROP
            "F" -> SetType.FAILURE
            "NOTE", "REST TIMER" -> return null
            else -> SetType.WORKING
        }
        return ImportedSet(
            type = type,
            weight = row.number("weight"),
            reps = row.whole("reps"),
            distance = row.number("distance"),
            distanceUnit = row.text("distance unit")?.let(::distanceUnit),
            seconds = row.whole("seconds"),
            rpe = row.number("rpe"),
        )
    }

    private fun distanceUnit(text: String): DistanceUnit? = when (text.lowercase(Locale.ROOT)) {
        "km" -> DistanceUnit.KILOMETER
        "m", "meters" -> DistanceUnit.METER
        "mi", "miles" -> DistanceUnit.MILE
        "yd", "yards" -> DistanceUnit.YARD
        else -> null
    }
}
