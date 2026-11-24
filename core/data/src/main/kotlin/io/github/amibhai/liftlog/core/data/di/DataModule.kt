package io.github.amibhai.liftlog.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.data.exercise.RoomExerciseRepository
import io.github.amibhai.liftlog.core.data.settings.DataStoreSettingsRepository
import io.github.amibhai.liftlog.core.data.settings.SettingsJson
import io.github.amibhai.liftlog.core.data.settings.SettingsSerializer
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.LiftLogDatabaseFactory
import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.id.RandomUuidGenerator
import io.github.amibhai.liftlog.core.domain.repository.ExerciseRepository
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindExerciseRepository(impl: RoomExerciseRepository): ExerciseRepository

    @Binds
    internal abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {
        /** File name inside files/datastore/; referenced by the Auto Backup rules in the app module. */
        private const val SETTINGS_FILE = "settings.json"

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): LiftLogDatabase =
            LiftLogDatabaseFactory.create(context)

        @Provides
        fun provideExerciseDao(database: LiftLogDatabase): ExerciseDao = database.exerciseDao()

        @Provides
        @Singleton
        internal fun provideSettingsDataStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
            @IoDispatcher io: CoroutineDispatcher,
        ): DataStore<SettingsJson> = DataStoreFactory.create(
            serializer = SettingsSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { SettingsJson() },
            scope = CoroutineScope(scope.coroutineContext + io),
            produceFile = { context.dataStoreFile(SETTINGS_FILE) },
        )

        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        fun provideClock(): Clock = Clock.systemUTC()

        @Provides
        fun provideIdGenerator(): IdGenerator = RandomUuidGenerator()
    }
}
