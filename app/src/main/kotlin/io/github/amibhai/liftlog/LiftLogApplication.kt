package io.github.amibhai.liftlog

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import io.github.amibhai.liftlog.core.data.progress.RecordMaintainer
import io.github.amibhai.liftlog.feature.workout.timer.RestTimerCoordinator
import javax.inject.Inject

@HiltAndroidApp
class LiftLogApplication :
    Application(),
    SingletonImageLoader.Factory {

    @Inject
    lateinit var restTimer: RestTimerCoordinator

    @Inject
    lateinit var records: RecordMaintainer

    override fun onCreate() {
        super.onCreate()
        // The rest timer's alarm and notifications follow the stored workout from the start (SPEC §6).
        restTimer.start(this)
        records.start()
    }

    /** Exercise pictures load lazily and stay on disk, so each is downloaded once (SPEC §3). */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("exercise_images"))
                .maxSizeBytes(IMAGE_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .build()

    private companion object {
        const val IMAGE_CACHE_BYTES = 256L * 1024 * 1024
    }
}
