package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val PIECES = 60
private const val DURATION_MS = 2_200
private const val COLORS = 3

/** Speeds and gravity are in canvas sizes per burst. */
private const val MIN_SPEED = 0.5f
private const val MAX_SPEED = 1.3f
private const val GRAVITY = 1.7f
private const val SIDEWAYS = 0.6f

/** How far from straight up a piece may fly, both ways together, in radians. */
private const val SPREAD = 2.4f
private const val STRAIGHT_UP = (PI / 2).toFloat()
private const val MAX_SPIN = 900f

/** Where the burst starts, down from the top. */
private const val ORIGIN = 0.4f
private const val PIECE_SIZE = 0.012f
private const val FADE_FROM = 0.6f

/**
 * One burst of confetti over the content (SPEC §13). It is decoration: touches go through it, TalkBack
 * skips it, and it ends at once when animations are turned off.
 */
@Composable
fun Celebration(onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val end by rememberUpdatedState(onEnd)
    val time = remember { Animatable(0f) }
    val pieces = remember { List(PIECES) { ConfettiPiece.random(Random(it)) } }
    LaunchedEffect(Unit) {
        time.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        end()
    }
    val colors = MaterialTheme.colorScheme.let { listOf(it.primary, it.tertiary, it.secondary) }
    Canvas(modifier.clearAndSetSemantics {}) {
        val now = time.value
        val side = size.width * PIECE_SIZE
        val alpha = ((1f - now) / (1f - FADE_FROM)).coerceIn(0f, 1f)
        for (piece in pieces) {
            val center = piece.position(now, size)
            rotate(piece.spin * now, pivot = center) {
                drawRect(
                    color = colors[piece.color],
                    topLeft = Offset(center.x - side, center.y - side / 2),
                    size = Size(side * 2, side),
                    alpha = alpha,
                )
            }
        }
    }
}

/** A piece that flies up, then falls. */
private class ConfettiPiece(val angle: Float, val speed: Float, val spin: Float, val color: Int) {

    fun position(time: Float, canvas: Size) = Offset(
        x = canvas.width * (0.5f + SIDEWAYS * speed * cos(angle) * time),
        y = canvas.height * (ORIGIN - speed * sin(angle) * time + GRAVITY * time * time),
    )

    companion object {
        fun random(random: Random) = ConfettiPiece(
            angle = STRAIGHT_UP + (random.nextFloat() - 0.5f) * SPREAD,
            speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED),
            spin = (random.nextFloat() - 0.5f) * 2 * MAX_SPIN,
            color = random.nextInt(COLORS),
        )
    }
}
