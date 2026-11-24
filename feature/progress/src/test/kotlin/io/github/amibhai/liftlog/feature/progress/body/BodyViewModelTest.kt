package io.github.amibhai.liftlog.feature.progress.body

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.model.ProgressPhotoId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import io.github.amibhai.liftlog.core.domain.progress.ProgressPhoto
import io.github.amibhai.liftlog.core.testing.FakeBodyRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BodyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = 1_700_000_000_000L
    private val body = FakeBodyRepository()
    private val photo = ProgressPhoto(ProgressPhotoId("p"), now, "/files/p.jpg")

    @Test
    fun everyKindOfMeasurementIsListedWithItsLatestValue() = runTest {
        val viewModel = viewModel()
        body.saveMetric(BodyMetric(BodyMetricId("1"), BodyMetricType.WAIST, 84.0, now))

        val metrics = viewModel.uiState.value.metrics
        assertThat(metrics.map { it.type }).containsExactlyElementsIn(BodyMetricType.entries)
        assertThat(metrics.single { it.type == BodyMetricType.WAIST }.latest?.value).isEqualTo(84.0)
        assertThat(metrics.single { it.type == BodyMetricType.CHEST }.latest).isNull()
    }

    @Test
    fun aDeletedPhotoCanBeBroughtBack() = runTest {
        val viewModel = viewModel()
        body.photoToAdd = photo
        viewModel.addPhoto("content://picture")
        assertThat(viewModel.uiState.value.photos).hasSize(1)

        viewModel.deletePhoto(photo.id)
        assertThat(viewModel.uiState.value.photos).isEmpty()
        assertThat(viewModel.uiState.value.deletedPhoto).isEqualTo(photo.id)

        viewModel.undoDeletePhoto()
        assertThat(viewModel.uiState.value.photos).hasSize(1)
        assertThat(viewModel.uiState.value.deletedPhoto).isNull()
    }

    @Test
    fun anUnreadablePictureIsReported() = runTest {
        val viewModel = viewModel()

        viewModel.addPhoto("content://broken")

        assertThat(viewModel.uiState.value.hasFailedToAdd).isTrue()
        viewModel.onMessageShown()
        assertThat(viewModel.uiState.value.hasFailedToAdd).isFalse()
    }

    private fun TestScope.viewModel(): BodyViewModel {
        val viewModel = BodyViewModel(
            body = body,
            settings = FakeSettingsRepository(),
            clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC),
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }
}
