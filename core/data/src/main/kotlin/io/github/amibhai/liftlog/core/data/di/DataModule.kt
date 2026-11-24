package io.github.amibhai.liftlog.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.data.backup.BackupSettingsJson
import io.github.amibhai.liftlog.core.data.backup.BackupSettingsSerializer
import io.github.amibhai.liftlog.core.data.settings.SettingsJson
import io.github.amibhai.liftlog.core.data.settings.SettingsSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /** File name inside files/datastore/; referenced by the Auto Backup rules in the app module. */
    private const val SETTINGS_FILE = "settings.json"
    private const val BACKUP_FILE = "backup.json"

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
    @Singleton
    internal fun provideBackupDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        @IoDispatcher io: CoroutineDispatcher,
    ): DataStore<BackupSettingsJson> = DataStoreFactory.create(
        serializer = BackupSettingsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { BackupSettingsJson() },
        scope = CoroutineScope(scope.coroutineContext + io),
        produceFile = { context.dataStoreFile(BACKUP_FILE) },
    )

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
