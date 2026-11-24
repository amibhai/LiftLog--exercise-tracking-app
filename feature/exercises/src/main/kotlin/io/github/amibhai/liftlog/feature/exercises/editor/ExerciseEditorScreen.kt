package io.github.amibhai.liftlog.feature.exercises.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.ui.component.OptionsDialog
import io.github.amibhai.liftlog.core.ui.component.SwitchRow
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.R
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun ExerciseEditorRoute(
    onClose: () -> Unit,
    onSave: (ExerciseId) -> Unit,
    viewModel: ExerciseEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by rememberUpdatedState(onSave)
    LaunchedEffect(state.savedId) { state.savedId?.let { saved(it) } }
    if (!state.isReady) return
    val actions = remember(viewModel, onClose) {
        ExerciseEditorActions(
            onChange = viewModel::change,
            onTogglePrimary = viewModel::togglePrimary,
            onToggleSecondary = viewModel::toggleSecondary,
            onSave = viewModel::save,
            onClose = onClose,
        )
    }
    ExerciseEditorScreen(state = state, actions = actions)
}

@Composable
internal fun ExerciseEditorScreen(
    state: ExerciseEditorUiState,
    actions: ExerciseEditorActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title =
                        if (state.isNew) R.string.exercises_editor_new_title else R.string.exercises_editor_edit_title
                    Text(stringResource(title))
                },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(painterResource(LiftLogIcons.Close), stringResource(UiR.string.ui_close))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            Form(
                draft = state.draft,
                actions = actions,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            )
            Button(
                onClick = actions.onSave,
                enabled = state.draft.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LiftLogDimens.screenGutter)
                    .heightIn(min = LiftLogDimens.primaryAction),
            ) { Text(stringResource(UiR.string.ui_save)) }
        }
    }
}

@Composable
private fun Form(draft: ExerciseDraft, actions: ExerciseEditorActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
        FormText(
            initial = draft.name,
            label = stringResource(R.string.exercises_editor_name),
            onChange = { name -> actions.onChange { it.copy(name = name) } },
            lineLimits = TextFieldLineLimits.SingleLine,
        )
        Choice(
            title = stringResource(R.string.exercises_editor_type),
            options = ExerciseType.entries,
            selected = draft.type,
            label = { stringResource(it.labelRes()) },
            onSelect = { type -> actions.onChange { it.copy(type = type) } },
        )
        Choice(
            title = stringResource(R.string.exercises_editor_equipment),
            options = Equipment.entries,
            selected = draft.equipment,
            label = { stringResource(it.labelRes()) },
            onSelect = { equipment -> actions.onChange { it.copy(equipment = equipment) } },
        )
        MuscleChips(
            title = stringResource(R.string.exercises_editor_primary),
            selected = draft.primaryMuscles,
            onToggle = actions.onTogglePrimary,
        )
        MuscleChips(
            title = stringResource(R.string.exercises_editor_secondary),
            selected = draft.secondaryMuscles,
            onToggle = actions.onToggleSecondary,
        )
        SwitchRow(
            title = stringResource(R.string.exercises_editor_unilateral),
            checked = draft.isUnilateral,
            onCheckedChange = { on -> actions.onChange { it.copy(isUnilateral = on) } },
            supportingText = stringResource(R.string.exercises_editor_unilateral_body),
        )
        FormText(
            initial = draft.instructions.joinToString("\n"),
            label = stringResource(R.string.exercises_editor_instructions),
            onChange = { text -> actions.onChange { it.copy(instructions = text.lines()) } },
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3),
        )
    }
}

@Composable
private fun FormText(
    initial: String,
    label: String,
    onChange: (String) -> Unit,
    lineLimits: TextFieldLineLimits,
    modifier: Modifier = Modifier,
) {
    val field = rememberTextFieldState(initial)
    val change by rememberUpdatedState(onChange)
    LaunchedEffect(field) { snapshotFlow { field.text.toString() }.collect { change(it) } }
    OutlinedTextField(
        state = field,
        modifier = modifier.fillMaxWidth().padding(horizontal = LiftLogDimens.screenGutter),
        label = { Text(label) },
        lineLimits = lineLimits,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun <T> Choice(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    ListItem(
        onClick = { isOpen = true },
        modifier = modifier,
        supportingContent = { Text(label(selected)) },
        trailingContent = { Icon(painterResource(LiftLogIcons.ExpandMore), contentDescription = null) },
    ) { Text(title) }
    if (isOpen) {
        OptionsDialog(
            title = title,
            options = options,
            selected = selected,
            label = label,
            onSelect = {
                isOpen = false
                onSelect(it)
            },
            onDismiss = { isOpen = false },
        )
    }
}

@Composable
private fun MuscleChips(
    title: String,
    selected: List<Muscle>,
    onToggle: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = LiftLogDimens.screenGutter)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = LiftLogDimens.spacingXs).semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
            for (muscle in Muscle.entries) {
                FilterChip(
                    selected = muscle in selected,
                    onClick = { onToggle(muscle) },
                    label = { Text(stringResource(muscle.labelRes())) },
                )
            }
        }
    }
}
