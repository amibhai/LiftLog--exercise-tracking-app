package io.github.amibhai.liftlog.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.domain.concurrent.AppDispatchers
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.id.RandomUuidGenerator
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.HistoryRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.usecase.AddExercises
import io.github.amibhai.liftlog.core.domain.usecase.SearchExercises
import io.github.amibhai.liftlog.core.domain.usecase.StartWorkout
import io.github.amibhai.liftlog.core.domain.workout.WorkoutFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Singleton

/** The use cases of core:domain, which knows nothing about dependency injection. */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    fun provideDispatchers() = AppDispatchers(default = Dispatchers.Default)

    @Provides
    @Singleton
    fun provideSequentialWork(@ApplicationScope scope: CoroutineScope) = SequentialWork(scope)

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideIdGenerator(): IdGenerator = RandomUuidGenerator()

    @Provides
    fun provideWorkoutFactory(ids: IdGenerator) = WorkoutFactory(ids)

    @Provides
    fun provideStartWorkout(
        workouts: WorkoutRepository,
        routines: RoutineRepository,
        history: HistoryRepository,
        factory: WorkoutFactory,
        clock: Clock,
    ) = StartWorkout(workouts, routines, history, factory, clock)

    @Provides
    fun provideAddExercises(
        workouts: WorkoutRepository,
        routines: RoutineRepository,
        exercises: ExerciseRepository,
        history: HistoryRepository,
        ids: IdGenerator,
        clock: Clock,
    ) = AddExercises(workouts, routines, exercises, history, ids, clock)

    @Provides
    fun provideSearchExercises(exercises: ExerciseRepository) = SearchExercises(exercises)
}
