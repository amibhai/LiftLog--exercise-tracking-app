package io.github.amibhai.liftlog.core.domain.images

import kotlinx.coroutines.flow.Flow

/** How many of the exercise pictures are on this phone. */
data class ImageDownload(val done: Int, val total: Int, val isRunning: Boolean)

/** "Download all exercise images for offline use" (SPEC §3). */
interface OfflineImages {
    fun observe(): Flow<ImageDownload>

    /** Goes on in the background, and where it left off. */
    fun download()

    fun cancel()
}
