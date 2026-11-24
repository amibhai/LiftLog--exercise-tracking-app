package io.github.amibhai.liftlog.core.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amibhai.liftlog.core.data.di.ApplicationScope
import io.github.amibhai.liftlog.core.domain.backup.BackupFrequency
import io.github.amibhai.liftlog.core.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK = "automatic-backup"
private const val DAYS_PER_WEEK = 7L

/** Keeps the automatic backups as the settings say (SPEC §9); started once, with the app. */
@Singleton
class BackupScheduler @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val backups: BackupRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            backups.settings
                .map { settings -> settings.frequency.takeIf { settings.isAutomatic } }
                .distinctUntilChanged()
                .collect(::schedule)
        }
    }

    private fun schedule(frequency: BackupFrequency?) {
        val manager = WorkManager.getInstance(context)
        val days = when (frequency) {
            BackupFrequency.DAILY -> 1L
            BackupFrequency.WEEKLY -> DAYS_PER_WEEK
            BackupFrequency.OFF, null -> null
        }
        if (days == null) {
            manager.cancelUniqueWork(WORK)
        } else {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(days, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
