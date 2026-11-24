package io.github.amibhai.liftlog.core.data.images

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.SingletonImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.amibhai.liftlog.core.data.di.IoDispatcher
import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import io.github.amibhai.liftlog.core.domain.images.ImageDownload
import io.github.amibhai.liftlog.core.domain.images.OfflineImages
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val WORK = "exercise-images"

internal class WorkOfflineImages @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exercises: ExerciseDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OfflineImages {

    override fun observe(): Flow<ImageDownload> = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(WORK)
        .map { infos -> state(infos.firstOrNull()) }
        .flowOn(io)

    override fun download() {
        val request = OneTimeWorkRequestBuilder<ImageDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    private suspend fun state(info: WorkInfo?): ImageDownload {
        val total = info?.progress?.let(ImageDownloadWorker::totalIn) ?: 0
        val isRunning = info != null && !info.state.isFinished
        return if (isRunning && total > 0) {
            ImageDownload(ImageDownloadWorker.doneIn(info.progress), total, isRunning = true)
        } else {
            val urls = exercises.imageUrls()
            val loader = SingletonImageLoader.get(context)
            ImageDownload(urls.count(loader::hasOnDisk), urls.size, isRunning)
        }
    }
}
