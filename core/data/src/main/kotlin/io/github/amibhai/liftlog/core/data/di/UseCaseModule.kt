package io.github.amibhai.liftlog.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.repository.ExerciseEditor
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgramRepository
import io.github.amibhai.liftlog.core.domain.repository.ProgressRepository
import io.github.amibhai.liftlog.core.domain.repository.RoutineRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import io.github.amibhai.liftlog.core.domain.repository.WorkoutRepository
import io.github.amibhai.liftlog.core.domain.usecase.CreateProgram
import io.github.amibhai.liftlog.core.domain.usecase.ImportLog
import io.github.amibhai.liftlog.core.domain.usecase.Insights
import io.github.amibhai.liftlog.core.domain.usecase.SearchExercises
import java.time.Clock

/** The other use cases of core:domain. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideCreateProgram(
        programs: ProgramRepository,
        routines: RoutineRepository,
        exercises: ExerciseRepository,
        settings: SettingsRepository,
        ids: IdGenerator,
        clock: Clock,
    ) = CreateProgram(programs, routines, exercises, settings, ids, clock)

    @Provides
    fun provideInsights(progress: ProgressRepository, settings: SettingsRepository, clock: Clock) =
        Insights(progress, settings, clock)

    @Provides
    fun provideImportLog(workouts: WorkoutRepository, exercises: ExerciseRepository, editor: ExerciseEditor) =
        ImportLog(workouts, exercises, editor)

    @Provides
    fun provideSearchExercises(exercises: ExerciseRepository) = SearchExercises(exercises)
}
