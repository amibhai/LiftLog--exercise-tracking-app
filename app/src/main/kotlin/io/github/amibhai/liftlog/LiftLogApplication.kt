package io.github.amibhai.liftlog

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import io.github.amibhai.liftlog.core.data.backup.BackupScheduler
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

    @Inject
    lateinit var backups: BackupScheduler

    override fun onCreate() {
        super.onCreate()
        // The rest timer's alarm and notifications follow the stored workout from the start (SPEC §6).
        restTimer.start(this)
        records.start()
        backups.start()
    }

    /**
     * Exercise pictures load lazily and stay on disk, so each is downloaded once (SPEC §3). Not in the cache
     * folder, which the system may empty: they can be downloaded for the gym without a signal.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.noBackupFilesDir.resolve("exercise_images"))
                .maxSizeBytes(IMAGE_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .build()

    private companion object {
        /** All of them are about 300 MB. */
        const val IMAGE_CACHE_BYTES = 512L * 1024 * 1024
    }
}
