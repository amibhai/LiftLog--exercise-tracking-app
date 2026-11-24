package io.github.amibhai.liftlog.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
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

/**
 * Reads and replaces all of the user's data (SPEC §9). Rows are backed up as they are, except for
 * deleted workouts and measurements. Records are left out, because they are worked out again.
 */
@Dao
interface BackupDao {

    /** Custom exercises, those with personal settings, and everything that other rows refer to. */
    @Query(
        """
        SELECT * FROM exercise WHERE is_custom = 1 OR user_modified_at IS NOT NULL OR is_favorite = 1
            OR is_archived = 1 OR notes IS NOT NULL OR default_rest_s IS NOT NULL OR weight_step_kg IS NOT NULL
            OR weight_step_lb IS NOT NULL OR bar_weight_kg IS NOT NULL
            OR id IN (SELECT exercise_id FROM workout_exercise) OR id IN (SELECT exercise_id FROM routine_exercise)
        """,
    )
    suspend fun exercises(): List<ExerciseEntity>

    /** Of the exercises whose muscles can differ from the library. */
    @Query(
        "SELECT m.* FROM exercise_muscle m JOIN exercise e ON e.id = m.exercise_id " +
            "WHERE e.is_custom = 1 OR e.user_modified_at IS NOT NULL",
    )
    suspend fun exerciseMuscles(): List<ExerciseMuscleEntity>

    @Query("SELECT * FROM routine")
    suspend fun routines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_exercise")
    suspend fun routineExercises(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM program")
    suspend fun programs(): List<ProgramEntity>

    @Query("SELECT * FROM program_day")
    suspend fun programDays(): List<ProgramDayEntity>

    @Query("SELECT * FROM workout WHERE deleted_at IS NULL")
    suspend fun workouts(): List<WorkoutEntity>

    @Query(
        "SELECT we.* FROM workout_exercise we JOIN workout w ON w.id = we.workout_id " +
            "WHERE we.deleted_at IS NULL AND w.deleted_at IS NULL",
    )
    suspend fun workoutExercises(): List<WorkoutExerciseEntity>

    @Query(
        "SELECT s.* FROM workout_set s JOIN workout_exercise we ON we.id = s.workout_exercise_id " +
            "JOIN workout w ON w.id = we.workout_id " +
            "WHERE s.deleted_at IS NULL AND we.deleted_at IS NULL AND w.deleted_at IS NULL",
    )
    suspend fun workoutSets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM body_metric WHERE deleted_at IS NULL")
    suspend fun bodyMetrics(): List<BodyMetricEntity>

    @Query("DELETE FROM personal_record")
    suspend fun clearRecords()

    /** Its exercises and sets go with it. */
    @Query("DELETE FROM workout")
    suspend fun clearWorkouts()

    @Query("DELETE FROM program")
    suspend fun clearPrograms()

    @Query("DELETE FROM routine")
    suspend fun clearRoutines()

    @Query("DELETE FROM body_metric")
    suspend fun clearBodyMetrics()

    @Query("DELETE FROM exercise WHERE is_custom = 1")
    suspend fun clearCustomExercises()

    @Query(
        """
        UPDATE exercise SET is_favorite = 0, is_archived = 0, notes = NULL, default_rest_s = NULL,
            weight_step_kg = NULL, weight_step_lb = NULL, bar_weight_kg = NULL
        """,
    )
    suspend fun clearPersonalSettings()

    @Query("SELECT id FROM exercise")
    suspend fun exerciseIds(): List<String>

    @Query("DELETE FROM exercise_muscle WHERE exercise_id IN (:exerciseIds)")
    suspend fun deleteMuscles(exerciseIds: List<String>)

    @Query(
        """
        UPDATE exercise SET is_favorite = :isFavorite, is_archived = :isArchived, notes = :notes,
            default_rest_s = :restSeconds, weight_step_kg = :stepKg, weight_step_lb = :stepLb, bar_weight_kg = :barKg
        WHERE id = :id
        """,
    )
    suspend fun setPersonalSettings(
        id: String,
        isFavorite: Boolean,
        isArchived: Boolean,
        notes: String?,
        restSeconds: Int?,
        stepKg: Double?,
        stepLb: Double?,
        barKg: Double?,
    )

    @Upsert
    suspend fun upsertExercises(rows: List<ExerciseEntity>)

    @Upsert
    suspend fun upsertMuscles(rows: List<ExerciseMuscleEntity>)

    @Upsert
    suspend fun upsertRoutines(rows: List<RoutineEntity>)

    @Upsert
    suspend fun upsertRoutineExercises(rows: List<RoutineExerciseEntity>)

    @Upsert
    suspend fun upsertPrograms(rows: List<ProgramEntity>)

    @Upsert
    suspend fun upsertProgramDays(rows: List<ProgramDayEntity>)

    @Upsert
    suspend fun upsertWorkouts(rows: List<WorkoutEntity>)

    @Upsert
    suspend fun upsertWorkoutExercises(rows: List<WorkoutExerciseEntity>)

    @Upsert
    suspend fun upsertWorkoutSets(rows: List<WorkoutSetEntity>)

    @Upsert
    suspend fun upsertBodyMetrics(rows: List<BodyMetricEntity>)
}
