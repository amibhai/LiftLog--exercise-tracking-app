package io.github.amibhai.liftlog.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val savedState = SavedStateHandle()

    @Test
    fun choicesAreStoredAtOnceInTheChosenUnit() = runTest {
        val viewModel = viewModel()

        viewModel.setUnit(WeightUnit.LB)
        viewModel.setBarWeight(35.0)
        viewModel.togglePlate(2.5)
        viewModel.togglePlate(1.25)

        val stored = settings.settings.first()
        assertThat(stored.weightUnit).isEqualTo(WeightUnit.LB)
        assertThat(stored.barWeightLb).isEqualTo(35.0)
        assertThat(stored.platesLb.map { it.weight }).containsExactly(45.0, 35.0, 25.0, 10.0, 5.0, 1.25).inOrder()
        assertThat(stored.platesKg).isNotEmpty()
        assertThat(stored.onboardingDone).isFalse()
    }

    @Test
    fun thePagesAreKeptWhenTheAppIsClosed() = runTest {
        viewModel().next()

        assertThat(viewModel().uiState.value.page).isEqualTo(OnboardingPage.PLATES)
    }

    @Test
    fun thereIsNoPageBeforeTheFirstOrAfterTheLast() = runTest {
        val viewModel = viewModel()

        viewModel.back()
        assertThat(viewModel.uiState.value.page).isEqualTo(OnboardingPage.UNIT)

        repeat(5) { viewModel.next() }
        assertThat(viewModel.uiState.value.page).isEqualTo(OnboardingPage.PROGRAM)
    }

    @Test
    fun finishingIsRemembered() = runTest {
        viewModel().finish()

        assertThat(settings.settings.first().onboardingDone).isTrue()
    }

    private fun TestScope.viewModel(): OnboardingViewModel {
        val viewModel = OnboardingViewModel(savedState, settings)
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }
}
