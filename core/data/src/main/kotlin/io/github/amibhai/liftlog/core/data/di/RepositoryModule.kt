package io.github.amibhai.liftlog.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.data.body.RoomBodyRepository
import io.github.amibhai.liftlog.core.data.exercise.RoomExerciseEditor
import io.github.amibhai.liftlog.core.data.exercise.RoomExerciseRepository
import io.github.amibhai.liftlog.core.data.progress.RoomProgressRepository
import io.github.amibhai.liftlog.core.data.routine.RoomRoutineRepository
import io.github.amibhai.liftlog.core.data.settings.DataStoreSettingsRepository
import io.github.amibhai.liftlog.core.data.workout.RoomHistoryRepository
import io.github.amibhai.liftlog.core.data.workout.RoomWorkoutRepository
import io.github.amibhai.liftlog.core.domain.repository.BodyRepository
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {

    @Binds
    fun exercises(impl: RoomExerciseRepository): ExerciseRepository

    @Binds
    fun exerciseEditor(impl: RoomExerciseEditor): ExerciseEditor

    @Binds
    fun workouts(impl: RoomWorkoutRepository): WorkoutRepository

    @Binds
    fun history(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    fun routines(impl: RoomRoutineRepository): RoutineRepository

    @Binds
    fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun progress(impl: RoomProgressRepository): ProgressRepository

    @Binds
    fun body(impl: RoomBodyRepository): BodyRepository
}
