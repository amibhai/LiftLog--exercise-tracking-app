package io.github.amibhai.liftlog.feature.settings.importer

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.concurrent.AppDispatchers
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.importer.LogSource
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.domain.usecase.ImportChoice
import io.github.amibhai.liftlog.core.domain.usecase.ImportLog
import io.github.amibhai.liftlog.core.domain.usecase.ImportResult
import io.github.amibhai.liftlog.core.domain.usecase.SearchExercises
import io.github.amibhai.liftlog.core.testing.FakeBackupRepository
import io.github.amibhai.liftlog.core.testing.FakeExerciseEditor
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.FakeWorkoutRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bench = exercise("Barbell Bench Press", Equipment.BARBELL)
    private val squat = exercise("Barbell Back Squat", Equipment.BARBELL)
    private val backups = FakeBackupRepository()
    private val workouts = FakeWorkoutRepository()

    @Test
    fun namesAreMatchedAndTheRestIsAskedAbout() = runTest {
        val state = ready(viewModel(CSV))

        assertThat(state.source).isEqualTo(LogSource.STRONG)
        assertThat(state.workouts).isEqualTo(2)
        assertThat(state.isUnitGuessed).isTrue()
        assertThat(state.unit).isEqualTo(WeightUnit.KG)
        assertThat(state.rows.map { it.name to it.needsCheck }).containsExactly(
            "Mystery Machine" to true,
            "Bench Press (Barbell)" to false,
        ).inOrder()
        assertThat(state.rows.first().choice).isEqualTo(ImportChoice.Create)
        assertThat(state.rows.last().exerciseName).isEqualTo("Barbell Bench Press")
    }

    @Test
    fun theChoicesDecideWhatIsImported() = runTest {
        val viewModel = viewModel(CSV)
        viewModel.setUnit(WeightUnit.LB)
        viewModel.choose("Bench Press (Barbell)", squat)
        viewModel.skip("Mystery Machine")

        viewModel.import()

        assertThat(viewModel.uiState.value).isEqualTo(ImportUiState.Done(ImportResult(added = 2, alreadyThere = 0)))
        val imported = workouts.observeFinished(10).first()
        assertThat(imported.flatMap { it.exercises }.map { it.exercise.id }.distinct()).containsExactly(squat.id)
        val set = imported.last().exercises.single().sets.single()
        assertThat(set.entered.weight?.displayText(WeightUnit.LB)).isEqualTo("185")
    }

    @Test
    fun importingTwiceAddsNothing() = runTest {
        viewModel(CSV).import()

        val second = viewModel(CSV)
        second.skip("Mystery Machine")
        second.import()

        assertThat(second.uiState.value).isEqualTo(ImportUiState.Done(ImportResult(added = 0, alreadyThere = 2)))
    }

    @Test
    fun anotherExerciseCanBeSearched() = runTest {
        val viewModel = viewModel(CSV)

        viewModel.search("squat")

        assertThat(ready(viewModel).found.map { it.name }).containsExactly("Barbell Back Squat")
    }

    @Test
    fun otherFilesAreNotImported() = runTest {
        assertThat(viewModel("a,b\n1,2").uiState.value).isEqualTo(ImportUiState.Unknown)
        assertThat(viewModel(null).uiState.value).isEqualTo(ImportUiState.Unknown)
    }

    private fun ready(viewModel: ImportViewModel) = viewModel.uiState.value as ImportUiState.Ready

    private fun TestScope.viewModel(csv: String?): ImportViewModel {
        val library = FakeExerciseRepository(catalog = listOf(bench, squat))
        if (csv == null) backups.texts.clear() else backups.texts[URI] = csv
        return ImportViewModel(
            savedStateHandle = SavedStateHandle(mapOf("uri" to URI)),
            backups = backups,
            importLog = ImportLog(workouts, library, FakeExerciseEditor()),
            exercises = library,
            searchExercises = SearchExercises(library),
            settings = FakeSettingsRepository(),
            work = SequentialWork(CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher)),
            dispatchers = AppDispatchers(default = mainDispatcherRule.dispatcher),
        )
    }

    private fun exercise(name: String, equipment: Equipment) = CatalogExercise(
        id = ExerciseId(name),
        name = name,
        type = ExerciseType.WEIGHT_REPS,
        equipment = equipment,
    )

    private companion object {
        const val URI = "content://log.csv"
        const val CSV = "Date,Workout Name,Exercise Name,Set Order,Weight,Reps\n" +
            "2024-01-01 10:00:00,Push,Bench Press (Barbell),1,185,5\n" +
            "2024-01-01 10:00:00,Push,Mystery Machine,1,50,10\n" +
            "2024-01-03 10:00:00,Push,Bench Press (Barbell),1,190,5\n"
    }
}
