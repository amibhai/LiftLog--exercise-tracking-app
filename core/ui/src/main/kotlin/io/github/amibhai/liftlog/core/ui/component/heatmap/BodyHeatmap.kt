package io.github.amibhai.liftlog.core.ui.component.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.theme.LiftLogColors
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/** A muscle with any work at all is clearly visible. */
private const val LEAST_LEVEL = 0.25f
private const val OUTLINE_WIDTH = 0.6f

/**
 * Both sides of the body, every muscle shaded by how far it is worked: 0 for nothing, 1 for enough
 * (SPEC §8). TalkBack reads [description] instead of the picture.
 */
@Composable
fun BodyHeatmap(levels: Map<Muscle, Float>, description: String, modifier: Modifier = Modifier) {
    val body = remember { parseOutlines(BODY_PARTS) }
    val front = remember { parseOutlines(FRONT_MUSCLES) }
    val back = remember { parseOutlines(BACK_MUSCLES) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingXl, Alignment.CenterHorizontally),
    ) {
        Figure(body, front, levels, stringResource(R.string.ui_heatmap_front), Modifier.weight(1f))
        Figure(body, back, levels, stringResource(R.string.ui_heatmap_back), Modifier.weight(1f))
    }
}

@Composable
private fun Figure(
    body: List<BodyOutline>,
    muscles: List<BodyOutline>,
    levels: Map<Muscle, Float>,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val idle = LiftLogColors.idle
    val silhouette = LiftLogColors.silhouette
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .aspectRatio(FIGURE_WIDTH / FIGURE_HEIGHT)
                .drawWithCache {
                    val scale = size.width / FIGURE_WIDTH
                    onDrawBehind {
                        scale(scale, scale, pivot = Offset.Zero) {
                            for (part in body) drawPath(part.path, silhouette)
                            for (outline in muscles) {
                                val level = outline.muscle?.let { levels[it] } ?: 0f
                                val fill = if (level <= 0f) {
                                    idle
                                } else {
                                    lerp(idle, colors.primary, level.coerceIn(LEAST_LEVEL, 1f))
                                }
                                drawPath(outline.path, fill)
                                drawPath(outline.path, colors.surface, style = Stroke(OUTLINE_WIDTH))
                            }
                        }
                    }
                },
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}
