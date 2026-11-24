package io.github.amibhai.liftlog.feature.routines.programs.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.program.TemplateDayName
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.component.LiftLogCard
import io.github.amibhai.liftlog.core.ui.format.LiftLogFormat
import io.github.amibhai.liftlog.core.ui.format.symbol
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.routines.R
import io.github.amibhai.liftlog.feature.routines.programs.descriptionRes
import io.github.amibhai.liftlog.feature.routines.programs.nameRes
import java.math.BigDecimal
import io.github.amibhai.liftlog.core.ui.R as UiR

private val MaxWeight = BigDecimal(2_000)

@Composable
internal fun TemplateRoute(
    onBack: () -> Unit,
    onCreate: (ProgramId) -> Unit,
    viewModel: TemplateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onCreate)
    val ready = state as? TemplateUiState.Ready ?: return
    LaunchedEffect(ready.created) { ready.created?.let(open) }
    TemplateScreen(state = ready, onBack = onBack, onCreate = viewModel::create)
}

@Composable
internal fun TemplateScreen(
    state: TemplateUiState.Ready,
    onBack: () -> Unit,
    onCreate: (String, Map<TemplateDayName, String>, Map<ExerciseId, Weight>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(state.key.nameRes())
    val dayNames = state.days.map { it.name to stringResource(it.name.nameRes()) }.toMap()
    val maxes = rememberSaveable(saver = MaxesSaver) {
        state.lifts.map { lift ->
            TextFieldState(lift.suggestion?.let { LiftLogFormat.weight(it, state.unit) }.orEmpty())
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                },
            )
        },
        bottomBar = {
            UseButton(
                enabled = !state.isCreating,
                onClick = { onCreate(name, dayNames, trainingMaxes(state, maxes)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(LiftLogDimens.screenGutter),
            verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        ) {
            item(key = "description") {
                Text(stringResource(state.key.descriptionRes()), style = MaterialTheme.typography.bodyLarge)
            }
            if (state.lifts.isNotEmpty()) {
                item(key = "maxes") { TrainingMaxes(state, maxes) }
            }
            items(state.days, key = { it.name.name }) { day -> DayCard(dayNames.getValue(day.name), day) }
        }
    }
}

/** What was typed, kept through rotation. */
private val MaxesSaver: Saver<List<TextFieldState>, Any> = listSaver(
    save = { states -> states.map { it.text.toString() } },
    restore = { texts -> texts.map { TextFieldState(it) } },
)

private fun trainingMaxes(state: TemplateUiState.Ready, fields: List<TextFieldState>): Map<ExerciseId, Weight> =
    state.lifts.zip(fields).mapNotNull { (lift, field) ->
        val value = field.text.toString().replace(LiftLogFormat.decimalSeparator(), '.').toBigDecimalOrNull()
        value?.takeIf { it.signum() > 0 && it < MaxWeight }?.let { lift.id to Weight.of(it, state.unit) }
    }.toMap()

@Composable
private fun UseButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(LiftLogDimens.screenGutter)
                .fillMaxWidth()
                .heightIn(min = LiftLogDimens.primaryAction),
            enabled = enabled,
        ) { Text(stringResource(R.string.programs_use)) }
    }
}

@Composable
private fun TrainingMaxes(state: TemplateUiState.Ready, fields: List<TextFieldState>, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.programs_maxes),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.programs_maxes_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for ((lift, field) in state.lifts.zip(fields)) MaxField(lift, field, state.unit)
    }
}

@Composable
private fun MaxField(lift: LiftUiModel, field: TextFieldState, unit: WeightUnit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        state = field,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.programs_max_label, lift.name, unit.symbol)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun DayCard(name: String, day: TemplateDayUiModel, modifier: Modifier = Modifier) {
    LiftLogCard(modifier = modifier) {
        Text(
            text = name,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
            for (exercise in day.exercises) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
                ) {
                    Text(exercise.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = remember(exercise) { planText(exercise) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "3 × 8–12" or "5 × 5". */
private fun planText(exercise: TemplateExerciseUiModel): String {
    val reps = exercise.reps
    val range = when {
        reps == null -> ""
        reps.first == reps.last -> " × ${reps.first}"
        else -> " × ${reps.first}–${reps.last}"
    }
    return exercise.sets.toString() + range
}
