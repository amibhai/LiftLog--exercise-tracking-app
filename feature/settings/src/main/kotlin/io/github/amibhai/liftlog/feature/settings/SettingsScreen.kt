package io.github.amibhai.liftlog.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ThemeMode
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

@Composable
internal fun SettingsRoute(
    showDeveloperOptions: Boolean,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenGallery: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setDynamicColor,
            onWeightUnitChange = viewModel::setWeightUnit,
            onOpenAbout = onOpenAbout,
            onOpenGallery = onOpenGallery,
        ),
        showDeveloperOptions = showDeveloperOptions,
        dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
}

/** Everything the settings screen can ask for, grouped so the screen stays easy to call and test. */
internal class SettingsActions(
    val onBack: () -> Unit,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onWeightUnitChange: (WeightUnit) -> Unit,
    val onOpenAbout: () -> Unit,
    val onOpenGallery: () -> Unit,
)

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    showDeveloperOptions: Boolean,
    dynamicColorAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            painterResource(LiftLogIcons.ArrowBack),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val settings = (state as? SettingsUiState.Ready)?.settings ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            AppearanceSection(settings, actions, dynamicColorAvailable)
            SectionHeader(stringResource(R.string.settings_section_units))
            ChoiceRow(
                title = stringResource(R.string.settings_weight_unit),
                options = WeightUnit.entries,
                selected = settings.weightUnit,
                label = { stringResource(it.labelRes()) },
                onSelect = actions.onWeightUnitChange,
            )
            SectionHeader(stringResource(R.string.settings_section_about))
            NavigationRow(title = stringResource(R.string.settings_about), onClick = actions.onOpenAbout)
            if (showDeveloperOptions) {
                SectionHeader(stringResource(R.string.settings_section_developer))
                NavigationRow(title = stringResource(R.string.settings_gallery), onClick = actions.onOpenGallery)
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    settings: UserSettings,
    actions: SettingsActions,
    dynamicColorAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val dynamicColorOn = settings.useDynamicColor && dynamicColorAvailable
    val dynamicColorHint = if (dynamicColorAvailable) {
        R.string.settings_dynamic_color_body
    } else {
        R.string.settings_dynamic_color_unavailable
    }
    Column(modifier = modifier) {
        SectionHeader(stringResource(R.string.settings_section_appearance))
        ChoiceRow(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { stringResource(it.labelRes()) },
            onSelect = actions.onThemeModeChange,
        )
        ListItem(
            checked = dynamicColorOn,
            onCheckedChange = actions.onDynamicColorChange,
            enabled = dynamicColorAvailable,
            supportingContent = { Text(stringResource(dynamicColorHint)) },
            trailingContent = {
                Switch(checked = dynamicColorOn, onCheckedChange = null, enabled = dynamicColorAvailable)
            },
        ) {
            Text(stringResource(R.string.settings_dynamic_color))
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(
                start = LiftLogDimens.screenGutter,
                end = LiftLogDimens.screenGutter,
                top = LiftLogDimens.spacingXl,
                bottom = LiftLogDimens.spacingS,
            )
            .semantics { heading() },
    )
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = LiftLogDimens.screenGutter, vertical = LiftLogDimens.spacingS)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = LiftLogDimens.spacingS)) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

@Composable
private fun NavigationRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        trailingContent = { Icon(painterResource(LiftLogIcons.ChevronRight), contentDescription = null) },
    ) {
        Text(title)
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.LIGHT -> R.string.settings_theme_light
}
