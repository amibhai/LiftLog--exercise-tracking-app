package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons

/** A search field that opens the keyboard when it is tapped, not before (SPEC §4). */
@Composable
fun SearchField(state: TextFieldState, hint: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(hint) },
        leadingIcon = { Icon(painterResource(LiftLogIcons.Search), contentDescription = null) },
        trailingIcon = {
            if (state.text.isNotEmpty()) {
                IconButton(onClick = { state.clearText() }) {
                    Icon(
                        painter = painterResource(LiftLogIcons.Close),
                        contentDescription = stringResource(R.string.ui_clear_search),
                    )
                }
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}
