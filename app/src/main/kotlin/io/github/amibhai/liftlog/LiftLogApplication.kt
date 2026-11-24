package io.github.amibhai.liftlog

import android.app.Application
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import io.github.amibhai.liftlog.core.data.backup.BackupScheduler
import io.github.amibhai.liftlog.core.data.progress.RecordMaintainer
import io.github.amibhai.liftlog.feature.widget.WidgetUpdater
import io.github.amibhai.liftlog.feature.workout.timer.RestTimerCoordinator
import javax.inject.Inject

@HiltAndroidApp
class LiftLogApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {

    @Inject
    lateinit var restTimer: RestTimerCoordinator

    @Inject
    lateinit var records: RecordMaintainer

    @Inject
    lateinit var backups: BackupScheduler

    @Inject
    lateinit var widgets: WidgetUpdater

    /** WorkManager asks for it when it is first used; nothing of it is needed for the first screen. */
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        // The rest timer's alarm and notifications follow the stored workout from the start (SPEC §6).
        restTimer.start(this)
        records.start()
        backups.start()
        widgets.start()
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
