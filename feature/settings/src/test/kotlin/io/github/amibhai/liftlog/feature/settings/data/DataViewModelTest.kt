package io.github.amibhai.liftlog.feature.settings.data

import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.backup.BackupFrequency
import io.github.amibhai.liftlog.core.domain.backup.BackupSummary
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.testing.FakeBackupRepository
import io.github.amibhai.liftlog.core.testing.FakeOfflineImages
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DataViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val backups = FakeBackupRepository()
    private val images = FakeOfflineImages()
    private val summary = BackupSummary(
        createdAt = 1,
        workouts = 12,
        routines = 3,
        programs = 1,
        customExercises = 2,
        measurements = 4,
    )

    @Test
    fun aNewFolderGetsABackupAtOnce() = runTest {
        val viewModel = viewModel()

        viewModel.chooseFolder("content://folder")

        assertThat(backups.calls).containsExactly("folder content://folder", "backup").inOrder()
        assertThat(viewModel.uiState.value.message).isEqualTo(DataMessage.BACKED_UP)
        assertThat(viewModel.uiState.value.backup?.folderUri).isEqualTo("content://folder")
    }

    @Test
    fun aFolderThatDoesNotWorkIsReported() = runTest {
        backups.works = false
        val viewModel = viewModel()

        viewModel.chooseFolder("content://folder")

        assertThat(viewModel.uiState.value.message).isEqualTo(DataMessage.FOLDER_FAILED)
    }

    @Test
    fun theScheduleIsStored() = runTest {
        val viewModel = viewModel()

        viewModel.setSchedule(BackupFrequency.WEEKLY, keep = 5)

        assertThat(backups.settings.value.frequency).isEqualTo(BackupFrequency.WEEKLY)
        assertThat(backups.settings.value.keep).isEqualTo(5)
    }

    @Test
    fun restoringAsksFirstAndCanBeUndone() = runTest {
        backups.summary = summary
        val viewModel = viewModel()

        viewModel.offerRestore("content://backup")
        assertThat(viewModel.uiState.value.restore).isEqualTo(RestoreOffer("content://backup", summary))
        assertThat(backups.calls).isEmpty()

        viewModel.restore()
        assertThat(backups.calls).containsExactly("restore content://backup")
        assertThat(viewModel.uiState.value.message).isEqualTo(DataMessage.RESTORED)
        assertThat(viewModel.uiState.value.restore).isNull()

        viewModel.undoRestore()
        assertThat(viewModel.uiState.value.message).isEqualTo(DataMessage.RESTORE_UNDONE)
    }

    @Test
    fun anotherFileIsNoBackup() = runTest {
        val viewModel = viewModel()

        viewModel.offerRestore("content://photo")

        assertThat(viewModel.uiState.value.restore).isNull()
        assertThat(viewModel.uiState.value.message).isEqualTo(DataMessage.NOT_A_BACKUP)
    }

    @Test
    fun exportsSayHowTheyWent() = runTest {
        val viewModel = viewModel()

        viewModel.exportJson("content://out")
        assertThat(viewModel.uiState.value.message).isEqualTo(DataMessage.EXPORTED)

        backups.works = false
        viewModel.exportCsv("content://out")
        assertThat(viewModel.uiState.value.message).isEqualTo(DataMessage.EXPORT_FAILED)
        assertThat(viewModel.uiState.value.isBusy).isFalse()
    }

    @Test
    fun picturesAreDownloadedAndStopped() = runTest {
        val viewModel = viewModel()

        viewModel.toggleImageDownload()
        assertThat(viewModel.uiState.value.images?.isRunning).isTrue()

        viewModel.toggleImageDownload()
        assertThat(viewModel.uiState.value.images?.isRunning).isFalse()
    }

    private fun TestScope.viewModel(): DataViewModel {
        val viewModel = DataViewModel(
            backups = backups,
            images = images,
            work = SequentialWork(CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher)),
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }
}
