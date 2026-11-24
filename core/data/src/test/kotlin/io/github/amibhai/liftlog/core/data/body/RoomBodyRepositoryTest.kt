package io.github.amibhai.liftlog.core.data.body

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.data.CountingIds
import io.github.amibhai.liftlog.core.data.NOW
import io.github.amibhai.liftlog.core.data.TestClock
import io.github.amibhai.liftlog.core.data.TestDatabase
import io.github.amibhai.liftlog.core.domain.model.BodyMetricId
import io.github.amibhai.liftlog.core.domain.progress.BodyMetric
import io.github.amibhai.liftlog.core.domain.progress.BodyMetricType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RoomBodyRepositoryTest {

    @get:Rule
    val db = TestDatabase()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = TestClock()
    private val ids = CountingIds()
    private val body by lazy { RoomBodyRepository(context, db.database.bodyDao(), ids, clock, Dispatchers.Unconfined) }
    private val weight = BodyMetric(BodyMetricId("m1"), BodyMetricType.BODY_WEIGHT, 81.5, NOW, notes = "morning")

    @Test
    fun measurementsAreKeptInOrderOfTime() = runTest {
        body.saveMetric(weight)
        body.saveMetric(weight.copy(id = BodyMetricId("m0"), value = 82.0, measuredAt = NOW - 1))
        body.saveMetric(weight.copy(id = BodyMetricId("w1"), type = BodyMetricType.WAIST, value = 84.0))

        assertThat(body.observeMetrics(BodyMetricType.BODY_WEIGHT).first().map { it.value })
            .containsExactly(82.0, 81.5).inOrder()
        assertThat(body.observeLatestMetrics().first().map { it.type to it.value }).containsExactly(
            BodyMetricType.BODY_WEIGHT to 81.5,
            BodyMetricType.WAIST to 84.0,
        ).inOrder()
    }

    @Test
    fun aMeasurementCanBeCorrectedAndDeletedAndBroughtBack() = runTest {
        body.saveMetric(weight)
        body.saveMetric(weight.copy(value = 80.0, notes = null))
        assertThat(body.observeMetrics(BodyMetricType.BODY_WEIGHT).first()).containsExactly(
            weight.copy(value = 80.0, notes = null),
        )

        body.deleteMetric(weight.id)
        assertThat(body.observeMetrics(BodyMetricType.BODY_WEIGHT).first()).isEmpty()

        body.restoreMetric(weight.id)
        assertThat(body.observeMetrics(BodyMetricType.BODY_WEIGHT).first()).hasSize(1)
    }

    @Test
    fun aPhotoIsCopiedIntoPrivateStorage() = runTest {
        val photo = body.addPhoto(picture("a picture"), takenAt = NOW)

        assertThat(photo).isNotNull()
        assertThat(File(photo!!.path).readText()).isEqualTo("a picture")
        assertThat(photo.path).startsWith(context.filesDir.path)
        assertThat(body.observePhotos().first()).containsExactly(photo)
    }

    @Test
    fun aPhotoThatCannotBeReadIsNotAdded() = runTest {
        val missing = Uri.fromFile(File(context.cacheDir, "missing.jpg")).toString()

        assertThat(body.addPhoto(missing, NOW)).isNull()
        assertThat(body.observePhotos().first()).isEmpty()
    }

    @Test
    fun deletedPhotosCanBeBroughtBackAndAreLaterRemovedForGood() = runTest {
        val photo = body.addPhoto(picture("old"), NOW)!!
        body.deletePhoto(photo.id)
        assertThat(body.observePhotos().first()).isEmpty()

        body.restorePhoto(photo.id)
        assertThat(body.observePhotos().first()).containsExactly(photo)

        body.deletePhoto(photo.id)
        clock.now += 8 * 24 * 3_600_000L
        body.addPhoto(picture("new"), NOW)
        assertThat(File(photo.path).exists()).isFalse()
    }

    private fun picture(content: String): String {
        val file = File(context.cacheDir, "source.jpg")
        file.writeText(content)
        return Uri.fromFile(file).toString()
    }
}
