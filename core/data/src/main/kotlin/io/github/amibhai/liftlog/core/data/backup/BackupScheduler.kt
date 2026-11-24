package io.github.amibhai.liftlog.core.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amibhai.liftlog.core.data.di.ApplicationScope
import io.github.amibhai.liftlog.core.domain.backup.BackupSettings
import io.github.amibhai.liftlog.core.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK = "automatic-backup"

/** Keeps the automatic backups as the settings say (SPEC §9); started once, with the app. */
@Singleton
class BackupScheduler @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val backups: BackupRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            backups.settings
                .distinctUntilChangedBy { settings -> settings.frequency.takeIf { settings.isAutomatic } }
                .collect(::schedule)
        }
    }

    private fun schedule(settings: BackupSettings) {
        val manager = WorkManager.getInstance(context)
        val days = settings.frequency.days
        val delay = settings.nextBackupIn(clock.millis())
        if (days == null || delay == null) {
            manager.cancelUniqueWork(WORK)
        } else {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(days, TimeUnit.DAYS)
                // Only for the first run: with UPDATE a schedule that is running already keeps its times.
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
