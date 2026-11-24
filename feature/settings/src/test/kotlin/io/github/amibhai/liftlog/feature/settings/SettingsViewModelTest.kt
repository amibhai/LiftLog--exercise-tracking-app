package io.github.amibhai.liftlog.feature.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.ThemeMode
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository()

    @Test
    fun exposesTheStoredSettings() = runTest {
        val viewModel = SettingsViewModel(repository)

        viewModel.uiState.test {
            assertThat(awaitReady().settings).isEqualTo(UserSettings())
        }
    }

    @Test
    fun choicesAreSaved() = runTest {
        val viewModel = SettingsViewModel(repository)

        viewModel.setWeightUnit(WeightUnit.LB)
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setDynamicColor(false)

        assertThat(repository.current.weightUnit).isEqualTo(WeightUnit.LB)
        assertThat(repository.current.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(repository.current.useDynamicColor).isFalse()
        viewModel.uiState.test {
            assertThat(awaitReady().settings).isEqualTo(repository.current)
        }
    }

    /** `stateIn` starts with Loading; skip it. */
    private suspend fun ReceiveTurbine<SettingsUiState>.awaitReady(): SettingsUiState.Ready {
        while (true) {
            val item = awaitItem()
            if (item is SettingsUiState.Ready) return item
        }
    }
}
