package io.github.amibhai.liftlog.core.ui.component.heatmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import io.github.amibhai.liftlog.core.domain.model.Muscle

/** The space the figure is drawn in. */
internal const val FIGURE_WIDTH = 100f
internal const val FIGURE_HEIGHT = 230f

/** Marks an outline that lies across the middle and has no mirror image. */
private const val MIDDLE = "*"

/** How far from a corner its rounding begins, as a part of the edge. */
private const val CORNER = 0.35f

/** One area of the figure; [muscle] is null for the parts that are no muscle, such as the head. */
internal class BodyOutline(val muscle: Muscle?, val path: Path)

/** Reads outlines: one per line as "NAME x,y x,y …", where an indented line goes on with the one above. */
internal fun parseOutlines(text: String): List<BodyOutline> = joinContinued(text.lines()).flatMap { line ->
    val tokens = line.split(' ').filter { it.isNotBlank() }
    val name = tokens.first()
    val corners = tokens.drop(1).map { it.toOffset() }
    val muscle = Muscle.entries.firstOrNull { it.name == name.removeSuffix(MIDDLE) }
    val sides = if (name.endsWith(MIDDLE)) listOf(corners) else listOf(corners, corners.map { it.mirrored() })
    sides.map { BodyOutline(muscle, roundedPath(it)) }
}

private fun joinContinued(lines: List<String>): List<String> = lines.filter { it.isNotBlank() }
    .fold(mutableListOf()) { joined: MutableList<String>, line ->
        if (line.startsWith(" ") && joined.isNotEmpty()) joined[joined.lastIndex] += line else joined += line
        joined
    }

private fun String.toOffset(): Offset = split(',').let { (x, y) -> Offset(x.toFloat(), y.toFloat()) }

private fun Offset.mirrored(): Offset = Offset(FIGURE_WIDTH - x, y)

/** The closed shape through [corners] with every corner rounded. */
private fun roundedPath(corners: List<Offset>): Path = Path().apply {
    corners.forEachIndexed { index, corner ->
        val before = corners[(index + corners.size - 1) % corners.size]
        val after = corners[(index + 1) % corners.size]
        val from = lerp(corner, before, CORNER)
        val to = lerp(corner, after, CORNER)
        if (index == 0) moveTo(from.x, from.y) else lineTo(from.x, from.y)
        quadraticTo(corner.x, corner.y, to.x, to.y)
    }
    close()
}
