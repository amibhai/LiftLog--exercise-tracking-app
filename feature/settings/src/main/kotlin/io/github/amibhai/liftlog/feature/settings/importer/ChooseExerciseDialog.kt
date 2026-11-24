package io.github.amibhai.liftlog.feature.settings.importer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.settings.R
import io.github.amibhai.liftlog.core.ui.R as UiR

/** Picks what a name of another app stands for: an exercise, a new one, or nothing. */
@Composable
internal fun ChooseExerciseDialog(
    name: String,
    found: List<CatalogExercise>,
    actions: ImportActions,
    onDismiss: () -> Unit,
) {
    // Without the equipment in parentheses, which would keep the search from finding much.
    val start = name.substringBefore('(').trim()
    val field = rememberTextFieldState(start, TextRange(start.length))
    val search by rememberUpdatedState(actions.onSearch)
    LaunchedEffect(field) { snapshotFlow { field.text.toString() }.collect { search(it) } }
    val act: (() -> Unit) -> Unit = { action ->
        onDismiss()
        action()
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                OutlinedTextField(
                    state = field,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LiftLogDimens.screenGutter),
                    label = { Text(stringResource(R.string.import_search, name)) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                LazyColumn(Modifier.weight(1f)) {
                    item(key = "create") {
                        ListItem(
                            onClick = { act { actions.onCreate(name) } },
                            supportingContent = { Text(stringResource(R.string.import_create_body)) },
                        ) { Text(stringResource(R.string.import_create)) }
                    }
                    item(key = "skip") {
                        ListItem(
                            onClick = { act { actions.onSkip(name) } },
                            supportingContent = { Text(stringResource(R.string.import_skip_body)) },
                        ) { Text(stringResource(R.string.import_skip)) }
                    }
                    item(key = "divider") { HorizontalDivider() }
                    items(found, key = { it.id.value }) { exercise ->
                        ListItem(
                            onClick = { act { actions.onChoose(name, exercise) } },
                            supportingContent = { Text(stringResource(exercise.equipment.labelRes())) },
                        ) { Text(exercise.name) }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(LiftLogDimens.spacingS),
                ) { Text(stringResource(UiR.string.ui_cancel)) }
            }
        }
    }
}
