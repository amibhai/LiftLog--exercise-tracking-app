package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

private val ThumbnailSize = 48.dp

/** A small exercise picture, or a dumbbell while it loads, when there is none, and offline. Decorative. */
@Composable
fun ExerciseThumbnail(url: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(ThumbnailSize),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(LiftLogIcons.Exercises),
                contentDescription = null,
                modifier = Modifier.size(LiftLogDimens.spacingXl),
            )
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.size(ThumbnailSize),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
