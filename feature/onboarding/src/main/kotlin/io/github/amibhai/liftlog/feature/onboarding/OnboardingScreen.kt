package io.github.amibhai.liftlog.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.plates.commonBars
import io.github.amibhai.liftlog.core.domain.plates.commonPlates
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.format.SetText
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/** The first run; [onFinish] says whether to choose a program next. */
@Composable
fun OnboardingRoute(
    onFinish: (openPrograms: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onFinish) {
        val finish = { openPrograms: Boolean ->
            viewModel.finish()
            onFinish(openPrograms)
        }
        OnboardingActions(
            onUnitChange = viewModel::setUnit,
            onBarChange = viewModel::setBarWeight,
            onTogglePlate = viewModel::togglePlate,
            onNext = viewModel::next,
            onBack = viewModel::back,
            onChooseProgram = { finish(true) },
            onSkip = { finish(false) },
        )
    }
    state.settings?.let { OnboardingScreen(state.page, it, actions, modifier) }
}

@Composable
internal fun OnboardingScreen(
    page: OnboardingPage,
    settings: UserSettings,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = page != OnboardingPage.entries.first(), onBack = actions.onBack)
    Scaffold(
        modifier = modifier,
        topBar = { TopRow(page, onSkip = actions.onSkip) },
        bottomBar = { Buttons(page, actions) },
    ) { padding ->
        AnimatedContent(
            targetState = page,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "page",
        ) { shown ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LiftLogDimens.screenGutter),
                verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
            ) {
                when (shown) {
                    OnboardingPage.UNIT -> UnitPage(settings.weightUnit, actions.onUnitChange)
                    OnboardingPage.PLATES -> PlatesPage(settings, actions)
                    OnboardingPage.PROGRAM -> Intro(R.string.onboarding_program_title, R.string.onboarding_program_body)
                }
            }
        }
    }
}

@Composable
private fun TopRow(page: OnboardingPage, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingS)
            .heightIn(min = LiftLogDimens.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.onboarding_step, page.ordinal + 1, OnboardingPage.entries.size),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The last page has a button for it.
        if (page != OnboardingPage.entries.last()) {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
        }
    }
}

@Composable
private fun Buttons(page: OnboardingPage, actions: OnboardingActions, modifier: Modifier = Modifier) {
    val button = Modifier
        .fillMaxWidth()
        .heightIn(min = LiftLogDimens.primaryAction)
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(LiftLogDimens.screenGutter),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        if (page == OnboardingPage.PROGRAM) {
            Button(onClick = actions.onChooseProgram, modifier = button) {
                Text(stringResource(R.string.onboarding_choose_program))
            }
            FilledTonalButton(onClick = actions.onSkip, modifier = button) {
                Text(stringResource(R.string.onboarding_no_program))
            }
        } else {
            Button(onClick = actions.onNext, modifier = button) { Text(stringResource(R.string.onboarding_next)) }
        }
    }
}

@Composable
private fun ColumnScope.Intro(@StringRes title: Int, @StringRes body: Int) {
    Text(
        text = stringResource(title),
        modifier = Modifier
            .padding(top = LiftLogDimens.spacingL)
            .semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        text = stringResource(body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ColumnScope.UnitPage(unit: WeightUnit, onUnitChange: (WeightUnit) -> Unit) {
    Intro(R.string.onboarding_unit_title, R.string.onboarding_unit_body)
    Column(Modifier.selectableGroup()) {
        WeightUnit.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LiftLogDimens.primaryAction)
                    .selectable(selected = option == unit, role = Role.RadioButton) { onUnitChange(option) },
                horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == unit, onClick = null)
                Text(stringResource(option.labelRes()), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ColumnScope.PlatesPage(settings: UserSettings, actions: OnboardingActions) {
    val unit = settings.weightUnit
    val yours = settings.plates(unit).map { it.weight }
    Intro(R.string.onboarding_plates_title, R.string.onboarding_plates_body)
    Chips(R.string.onboarding_bar, unit, unit.commonBars, selected = setOf(settings.barWeight), actions.onBarChange)
    Chips(R.string.onboarding_plates, unit, unit.commonPlates, selected = yours.toSet(), actions.onTogglePlate)
}

@Composable
private fun Chips(
    @StringRes title: Int,
    unit: WeightUnit,
    weights: List<Double>,
    selected: Set<Double>,
    onClick: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = remember(unit) { SetText(unit) }
    Column(modifier) {
        Text(
            text = stringResource(title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS)) {
            weights.forEach { weight ->
                FilterChip(
                    selected = weight in selected,
                    onClick = { onClick(weight) },
                    label = { Text(text.weight(Weight.of(weight, unit))) },
                )
            }
        }
    }
}

/** What the pages can ask for. */
internal class OnboardingActions(
    val onUnitChange: (WeightUnit) -> Unit,
    val onBarChange: (Double) -> Unit,
    val onTogglePlate: (Double) -> Unit,
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onChooseProgram: () -> Unit,
    val onSkip: () -> Unit,
)
