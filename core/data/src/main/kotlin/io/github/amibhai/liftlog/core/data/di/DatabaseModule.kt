package io.github.amibhai.liftlog.core.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.LiftLogDatabaseFactory
import io.github.amibhai.liftlog.core.database.dao.BodyDao
import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import io.github.amibhai.liftlog.core.database.dao.HistoryDao
import io.github.amibhai.liftlog.core.database.dao.ProgramDao
import io.github.amibhai.liftlog.core.database.dao.ProgressDao
import io.github.amibhai.liftlog.core.database.dao.RoutineDao
import io.github.amibhai.liftlog.core.database.dao.WorkoutDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LiftLogDatabase = LiftLogDatabaseFactory.create(context)

    @Provides
    fun provideExerciseDao(database: LiftLogDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWorkoutDao(database: LiftLogDatabase): WorkoutDao = database.workoutDao()

    @Provides
    fun provideRoutineDao(database: LiftLogDatabase): RoutineDao = database.routineDao()

    @Provides
    fun provideProgramDao(database: LiftLogDatabase): ProgramDao = database.programDao()

    @Provides
    fun provideHistoryDao(database: LiftLogDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideProgressDao(database: LiftLogDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideBodyDao(database: LiftLogDatabase): BodyDao = database.bodyDao()
}
