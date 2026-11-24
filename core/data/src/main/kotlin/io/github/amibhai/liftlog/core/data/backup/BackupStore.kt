package io.github.amibhai.liftlog.core.data.backup

import androidx.datastore.core.DataStore
import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import io.github.amibhai.liftlog.core.data.progress.RecordKeeper
import io.github.amibhai.liftlog.core.data.settings.SettingsJson
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.dao.BackupDao
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject

/** Turns all of the user's data into a backup and back. */
internal class BackupStore @Inject constructor(
    private val database: LiftLogDatabase,
    private val dao: BackupDao,
    private val settings: DataStore<SettingsJson>,
    private val records: RecordKeeper,
    private val clock: Clock,
) {
    suspend fun write(output: OutputStream) = BackupJson.write(snapshot(), output)

    fun read(input: InputStream): BackupFile? = BackupJson.read(input)

    /** All or nothing: when something is wrong with the backup, the data stays as it was. */
    suspend fun restore(file: BackupFile) {
        database.withWriteTransaction {
            clear()
            restoreExercises(file)
            dao.upsertRoutines(file.routines)
            dao.upsertRoutineExercises(file.routineExercises)
            dao.upsertPrograms(file.programs)
            dao.upsertProgramDays(file.programDays)
            dao.upsertWorkouts(file.workouts)
            dao.upsertWorkoutExercises(file.workoutExercises)
            dao.upsertWorkoutSets(file.workoutSets)
            dao.upsertBodyMetrics(file.bodyMetrics)
        }
        settings.updateData { file.settings }
        records.rebuildAll()
    }

    private suspend fun snapshot(): BackupFile {
        val currentSettings = settings.data.first()
        return database.withReadTransaction {
            BackupFile(
                format = BackupFile.FORMAT,
                version = BackupFile.VERSION,
                createdAt = clock.millis(),
                settings = currentSettings,
                exercises = dao.exercises(),
                exerciseMuscles = dao.exerciseMuscles(),
                routines = dao.routines(),
                routineExercises = dao.routineExercises(),
                programs = dao.programs(),
                programDays = dao.programDays(),
                workouts = dao.workouts(),
                workoutExercises = dao.workoutExercises(),
                workoutSets = dao.workoutSets(),
                bodyMetrics = dao.bodyMetrics(),
            )
        }
    }

    private suspend fun clear() {
        dao.clearRecords()
        dao.clearWorkouts()
        dao.clearPrograms()
        dao.clearRoutines()
        dao.clearBodyMetrics()
        dao.clearCustomExercises()
        dao.clearPersonalSettings()
    }

    /**
     * Custom and edited exercises come back as they were. Of the others only the personal settings do:
     * the library of this app may be newer than the one in the backup.
     */
    private suspend fun restoreExercises(file: BackupFile) {
        val library = dao.exerciseIds().toSet()
        val (whole, untouched) = file.exercises.partition { it.isOwn || it.id !in library }
        dao.upsertExercises(whole)
        dao.deleteMuscles(whole.filter { it.isOwn }.map { it.id })
        val known = whole.mapTo(mutableSetOf()) { it.id }
        dao.upsertMuscles(file.exerciseMuscles.filter { it.exerciseId in known })
        for (exercise in untouched) {
            dao.setPersonalSettings(
                id = exercise.id,
                isFavorite = exercise.isFavorite,
                isArchived = exercise.isArchived,
                notes = exercise.notes,
                restSeconds = exercise.defaultRestSeconds,
                stepKg = exercise.weightStepKg,
                stepLb = exercise.weightStepLb,
                barKg = exercise.barWeightKg,
            )
        }
    }

    private val ExerciseEntity.isOwn: Boolean get() = isCustom || userModifiedAt != null
}
