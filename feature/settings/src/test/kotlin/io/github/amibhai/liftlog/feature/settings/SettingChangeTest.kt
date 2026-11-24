package io.github.amibhai.liftlog.feature.settings

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.EffortMode
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import org.junit.Test

class SettingChangeTest {

    @Test
    fun weightsChangeForTheChosenUnitOnly() {
        val kg = UserSettings(weightUnit = WeightUnit.KG)
            .with(SettingChange.WeightStep(1.25))
            .with(SettingChange.DumbbellStep(1.0))
            .with(SettingChange.BarWeight(15.0))

        assertThat(kg.weightStep).isEqualTo(1.25)
        assertThat(kg.dumbbellStep).isEqualTo(1.0)
        assertThat(kg.barWeight).isEqualTo(15.0)
        assertThat(kg.stepsLb).isEqualTo(UserSettings().stepsLb)
        assertThat(kg.barWeightLb).isEqualTo(UserSettings().barWeightLb)

        val lb = kg.copy(weightUnit = WeightUnit.LB).with(SettingChange.BarWeight(35.0))
        assertThat(lb.barWeight).isEqualTo(35.0)
        assertThat(lb.barWeightKg).isEqualTo(15.0)
    }

    @Test
    fun timerAndLoggingSettingsChange() {
        val changed = listOf(
            SettingChange.DefaultRest(90),
            SettingChange.AutoStartRest(false),
            SettingChange.TimerSound(false),
            SettingChange.TimerVibration(false),
            SettingChange.Effort(EffortMode.RIR),
            SettingChange.KeepScreenOn(false),
        ).fold(UserSettings()) { settings, change -> settings.with(change) }

        assertThat(changed).isEqualTo(
            UserSettings(
                defaultRestSeconds = 90,
                autoStartRestTimer = false,
                restTimerSound = false,
                restTimerVibration = false,
                effortMode = EffortMode.RIR,
                keepScreenOn = false,
            ),
        )
    }
}
