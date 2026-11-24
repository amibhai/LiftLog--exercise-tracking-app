package io.github.amibhai.liftlog

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LiftLogApplication :
    Application(),
    SingletonImageLoader.Factory {

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
