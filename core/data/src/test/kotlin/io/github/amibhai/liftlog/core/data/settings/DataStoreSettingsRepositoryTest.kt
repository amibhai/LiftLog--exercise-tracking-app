package io.github.amibhai.liftlog.core.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Plate
import io.github.amibhai.liftlog.core.domain.model.ThemeMode
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.model.WarmupStep
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

class DataStoreSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun withoutASavedFileTheDomainDefaultsApply() = runTest {
        val repository = repository(folder.root.resolve("settings.json"), backgroundScope)

        assertThat(repository.settings.first()).isEqualTo(UserSettings())
    }

    @Test
    fun updatesArePersistedToTheFile() = runTest {
        val file = folder.root.resolve("settings.json")
        val repository = repository(file, backgroundScope)

        repository.update {
            it.copy(
                weightUnit = WeightUnit.LB,
                themeMode = ThemeMode.LIGHT,
                platesLb = listOf(Plate(45.0, 4)),
                warmupSteps = listOf(WarmupStep(50, 5)),
            )
        }

        val copy = folder.root.resolve("copy.json").also { file.copyTo(it) }
        val reloaded = repository(copy, backgroundScope).settings.first()
        assertThat(reloaded.weightUnit).isEqualTo(WeightUnit.LB)
        assertThat(reloaded.themeMode).isEqualTo(ThemeMode.LIGHT)
        assertThat(reloaded.platesLb).containsExactly(Plate(45.0, 4))
        assertThat(reloaded.warmupSteps).containsExactly(WarmupStep(50, 5))
    }

    @Test
    fun keysAndEnumValuesFromANewerBuildFallBackToDefaults() = runTest {
        val file = folder.root.resolve("settings.json")
        file.writeText("""{"weightUnit":"STONE","themeMode":"LIGHT","settingFromTheFuture":true}""")

        val settings = repository(file, backgroundScope).settings.first()

        assertThat(settings.weightUnit).isEqualTo(UserSettings().weightUnit)
        assertThat(settings.themeMode).isEqualTo(ThemeMode.LIGHT)
    }

    /**
     * Malformed JSON must surface as [CorruptionException]: that is what makes DataStore apply the
     * ReplaceFileCorruptionHandler (reset to defaults) instead of crashing. The replacement itself is
     * DataStore's job and is not exercised here, because its overwrite-by-rename fails on a Windows
     * host JVM (it works on Android).
     */
    @Test
    fun malformedJsonIsReportedAsCorruption() = runTest {
        val error = runCatching { SettingsSerializer.readFrom("this is not json".byteInputStream()) }.exceptionOrNull()

        assertThat(error).isInstanceOf(CorruptionException::class.java)
    }

    @Test
    fun everyValueIsWrittenExplicitly() = runTest {
        val output = ByteArrayOutputStream()

        SettingsSerializer.writeTo(SettingsJson(), output)

        val written = output.toString(Charsets.UTF_8)
        assertThat(written).contains("\"weightUnit\": \"KG\"")
        assertThat(written).contains("\"platesKg\"")
    }

    private fun repository(file: File, scope: CoroutineScope) = DataStoreSettingsRepository(
        DataStoreFactory.create(
            serializer = SettingsSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { SettingsJson() },
            scope = scope,
            produceFile = { file },
        ),
    )
}
