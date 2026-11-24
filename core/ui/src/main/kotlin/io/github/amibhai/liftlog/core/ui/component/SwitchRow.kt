package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * A setting that is on or off; tapping anywhere on the row toggles it. The switch shows the state, so
 * the row does not use Material's highlight for checked list items.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    val colors = ListItemDefaults.colors()
    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Switch },
        enabled = enabled,
        supportingContent = supportingText?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = colors.copy(
            selectedContainerColor = colors.containerColor,
            selectedContentColor = colors.contentColor,
            selectedSupportingContentColor = colors.supportingContentColor,
        ),
    ) {
        Text(title)
    }
}
