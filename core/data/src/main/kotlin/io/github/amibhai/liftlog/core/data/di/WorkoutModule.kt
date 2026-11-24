package io.github.amibhai.liftlog.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.usecase.AddExercises
import io.github.amibhai.liftlog.core.domain.usecase.ProgressionAdvisor
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import java.time.Clock

/** The use cases of core:domain for starting and filling workouts and routines. */
@Module
@InstallIn(SingletonComponent::class)
object WorkoutModule {

    @Provides
    fun provideWorkoutFactory(ids: IdGenerator) = WorkoutFactory(ids)

    @Provides
    fun provideStartWorkout(
        workouts: WorkoutRepository,
        routines: RoutineRepository,
        programs: ProgramRepository,
        history: HistoryRepository,
        advisor: ProgressionAdvisor,
        factory: WorkoutFactory,
        clock: Clock,
    ) = StartWorkout(workouts, routines, programs, history, advisor, factory, clock)

    @Provides
    fun provideProgressionAdvisor(progress: ProgressRepository, settings: SettingsRepository) =
        ProgressionAdvisor(progress, settings)

    @Provides
    fun provideAddExercises(
        workouts: WorkoutRepository,
        routines: RoutineRepository,
        exercises: ExerciseRepository,
        history: HistoryRepository,
        ids: IdGenerator,
        clock: Clock,
    ) = AddExercises(workouts, routines, exercises, history, ids, clock)
}
