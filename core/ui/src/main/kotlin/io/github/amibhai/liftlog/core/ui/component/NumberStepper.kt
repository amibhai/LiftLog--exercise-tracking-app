package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons

/** A whole number between − and +, which TalkBack calls [fewer] and [more]; [text] shows the number. */
@Composable
fun NumberStepper(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    fewer: String,
    more: String,
    modifier: Modifier = Modifier,
    text: String = value.toString(),
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Icon(painterResource(LiftLogIcons.Remove), fewer)
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Icon(painterResource(LiftLogIcons.Add), more)
        }
    }
}
