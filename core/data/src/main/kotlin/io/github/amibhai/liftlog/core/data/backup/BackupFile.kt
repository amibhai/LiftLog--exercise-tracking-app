package io.github.amibhai.liftlog.core.data.backup

import io.github.amibhai.liftlog.core.data.settings.SettingsJson
import io.github.amibhai.liftlog.core.database.entity.BodyMetricEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.database.entity.ProgramDayEntity
import io.github.amibhai.liftlog.core.database.entity.ProgramEntity
import io.github.amibhai.liftlog.core.database.entity.RoutineEntity
import io.github.amibhai.liftlog.core.database.entity.RoutineExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.WorkoutSetEntity
import io.github.amibhai.liftlog.core.domain.backup.BackupSummary
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

/**
 * A backup: the rows of the database as they are (SPEC §9). Property names of the entities are part of
 * the format. A new property needs a default value, so that older backups can still be read; renaming
 * one, or giving one another meaning, needs a new [VERSION] and a conversion.
 */
@Serializable
internal data class BackupFile(
    val format: String,
    val version: Int,
    val createdAt: Long,
    val settings: SettingsJson,
    val exercises: List<ExerciseEntity>,
    val exerciseMuscles: List<ExerciseMuscleEntity>,
    val routines: List<RoutineEntity>,
    val routineExercises: List<RoutineExerciseEntity>,
    val programs: List<ProgramEntity>,
    val programDays: List<ProgramDayEntity>,
    val workouts: List<WorkoutEntity>,
    val workoutExercises: List<WorkoutExerciseEntity>,
    val workoutSets: List<WorkoutSetEntity>,
    val bodyMetrics: List<BodyMetricEntity>,
) {
    fun summary() = BackupSummary(
        createdAt = createdAt,
        workouts = workouts.count { !it.isInProgress },
        routines = routines.count { it.deletedAt == null },
        programs = programs.count { it.deletedAt == null },
        customExercises = exercises.count { it.isCustom && it.deletedAt == null },
        measurements = bodyMetrics.size,
    )

    companion object {
        const val FORMAT = "liftlog-backup"
        const val VERSION = 1
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal object BackupJson {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun write(file: BackupFile, output: OutputStream) = json.encodeToStream(file, output)

    /** Null when this is no LiftLog backup, or one of a newer version. */
    fun read(input: InputStream): BackupFile? = try {
        json.decodeFromStream<BackupFile>(input)
            .takeIf { it.format == BackupFile.FORMAT && it.version <= BackupFile.VERSION }
    } catch (_: SerializationException) {
        null
    }
}
