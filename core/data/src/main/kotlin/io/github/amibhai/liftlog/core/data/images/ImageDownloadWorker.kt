package io.github.amibhai.liftlog.core.data.images

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val PARALLEL = 6
private const val MAX_ATTEMPTS = 5
private const val DONE = "done"
private const val TOTAL = "total"

/** Puts every exercise picture into the disk cache of the app. What is there already is not loaded again. */
internal class ImageDownloadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun exercises(): ExerciseDao
    }

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val loader = SingletonImageLoader.get(applicationContext)
        val urls = dependencies.exercises().imageUrls()
        val missing = urls.filterNot(loader::hasOnDisk)
        var done = urls.size - missing.size
        setProgress(progress(done, urls.size))
        for (part in missing.chunked(PARALLEL)) {
            val loaded = load(loader, part)
            done += loaded
            setProgress(progress(done, urls.size))
            // Without a connection nothing will work for now.
            if (loaded == 0) break
        }
        return when {
            done == urls.size -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    private suspend fun load(loader: ImageLoader, urls: List<String>): Int = coroutineScope {
        urls.map { url -> async { loader.execute(request(url)) is SuccessResult } }.awaitAll().count { it }
    }

    /** The picture is only stored, not decoded. */
    @OptIn(ExperimentalCoilApi::class)
    private fun request(url: String): ImageRequest = ImageRequest.Builder(applicationContext)
        .data(url)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .decoderFactory(BlackholeDecoder.Factory())
        .build()

    companion object {
        private fun progress(done: Int, total: Int): Data = workDataOf(DONE to done, TOTAL to total)

        fun doneIn(progress: Data): Int = progress.getInt(DONE, 0)

        fun totalIn(progress: Data): Int = progress.getInt(TOTAL, 0)
    }
}

internal fun ImageLoader.hasOnDisk(url: String): Boolean = diskCache?.openSnapshot(url)?.use { true } ?: false
