package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/** Start padding inside Material's text button. */
private val TextButtonInset = LiftLogDimens.spacingM

/** A low-emphasis action, shifted so that its label lines up with the text above it. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.offset(x = -TextButtonInset)) { Text(text) }
}
