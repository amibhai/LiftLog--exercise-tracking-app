package io.github.amibhai.liftlog.core.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.domain.repository.BackupRepository

private const val MAX_ATTEMPTS = 3

/** Writes an automatic backup. WorkManager creates it, so it asks Hilt for what it needs. */
internal class BackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun backups(): BackupRepository
    }

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        return when {
            dependencies.backups().backUpNow() -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }
}
