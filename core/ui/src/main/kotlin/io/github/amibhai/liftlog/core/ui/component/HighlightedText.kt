package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/** [text] with the parts that match a search emphasized (SPEC §4). */
@Composable
fun HighlightedText(text: String, ranges: List<IntRange>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val annotated = remember(text, ranges, color) {
        buildAnnotatedString {
            append(text)
            for (range in ranges) {
                val end = minOf(range.last + 1, text.length)
                addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), range.first, end)
            }
        }
    }
    Text(text = annotated, modifier = modifier)
}
