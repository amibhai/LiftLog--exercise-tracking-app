package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.ExerciseHistory
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.progress.MuscleWork
import io.github.amibhai.liftlog.core.domain.progress.WorkoutStats
import io.github.amibhai.liftlog.core.domain.progression.PlannedSession
import kotlinx.coroutines.flow.Flow

/** What finished workouts add up to (SPEC §8). Warm-ups and the workout in progress never count. */
interface ProgressRepository {
    /** Every finished session of one exercise, oldest first. */
    fun observeSessions(id: ExerciseId): Flow<List<ExerciseSession>>

    /** Everything done with a load since [since]: what insights are drawn from. */
    fun observeLoadedExercises(since: Long): Flow<List<ExerciseHistory>>

    /** The sessions of a few exercises before [before]: what records of the current workout are compared with. */
    suspend fun sessionsBefore(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, List<ExerciseSession>>

    /** The same with the routine slot each session was planned by: what progression rules build on. */
    suspend fun plannedSessions(before: Long, ids: Set<ExerciseId>): Map<ExerciseId, List<PlannedSession>>

    /** Every finished workout, summed up, oldest first. */
    fun observeWorkoutStats(): Flow<List<WorkoutStats>>

    /** The counting sets per exercise of the workouts started from [from] until before [to]. */
    fun observeMuscleWork(from: Long, to: Long): Flow<List<MuscleWork>>

    /** The same for a single workout. */
    fun observeMuscleWork(workout: WorkoutId): Flow<List<MuscleWork>>

    /** The latest records broken, newest first. */
    fun observeRecentRecords(limit: Int): Flow<List<RecordEntry>>

    /** The records one workout broke. */
    fun observeRecordsOf(workout: WorkoutId): Flow<List<RecordEntry>>

    /** Works out all records again, for example after an import or with another formula. */
    suspend fun rebuildRecords()
}
