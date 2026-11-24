package io.github.amibhai.liftlog.feature.routines.programs

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.concurrent.SequentialWork
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.CatalogExercise
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseSession
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LoggedSet
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetType
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.program.ProgramTemplates
import io.github.amibhai.liftlog.core.domain.program.TemplateDayName
import io.github.amibhai.liftlog.core.domain.program.TemplateKey
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.usecase.CreateProgram
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.FakeProgramRepository
import io.github.amibhai.liftlog.core.testing.FakeProgressRepository
import io.github.amibhai.liftlog.core.testing.FakeRoutineRepository
import io.github.amibhai.liftlog.core.testing.FakeSettingsRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import io.github.amibhai.liftlog.feature.routines.programs.detail.ProgramAction
import io.github.amibhai.liftlog.feature.routines.programs.detail.ProgramUiState
import io.github.amibhai.liftlog.feature.routines.programs.detail.ProgramViewModel
import io.github.amibhai.liftlog.feature.routines.programs.detail.RoutineChoice
import io.github.amibhai.liftlog.feature.routines.programs.template.TemplateUiState
import io.github.amibhai.liftlog.feature.routines.programs.template.TemplateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class ProgramViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var next = 0
    private val ids = IdGenerator { "id${next++}" }
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    private val routines = FakeRoutineRepository(Routine(PUSH, "Push"), Routine(PULL, "Pull"))
    private val program = Program(
        ProgramId("p"),
        "My split",
        days = listOf(
            ProgramDay(ProgramDayId("d1"), "Push", PUSH),
            ProgramDay(ProgramDayId("d2"), "Pull", PULL),
        ),
    )
    private val programs = FakeProgramRepository(program)
    private val stored get() = programs.all.single()

    @Test
    fun aNewProgramOpensOnceItIsStored() = runTest {
        val viewModel = ProgramsViewModel(programs, ids, work())
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }

        viewModel.newProgram()

        val created = programs.all.last().id
        assertThat(viewModel.uiState.value.openProgram).isEqualTo(created)
        viewModel.onProgramOpened()
        assertThat(viewModel.uiState.value.openProgram).isNull()
    }

    @Test
    fun theRotationCanBeChangedAndFollowed() = runTest {
        val viewModel = programViewModel()

        viewModel.onAction(ProgramAction.AddDay(RoutineChoice(PUSH, "Push")))
        viewModel.onAction(ProgramAction.RenameDay(ProgramDayId("d1"), " Heavy push "))
        viewModel.onAction(ProgramAction.Reorder(listOf(ProgramDayId("d2"), ProgramDayId("d1"))))
        viewModel.onAction(ProgramAction.Follow(isActive = true))

        assertThat(stored.days.map { it.name }).containsExactly("Pull", "Heavy push", "Push").inOrder()
        val state = viewModel.uiState.value as ProgramUiState.Ready
        assertThat(state.isActive).isTrue()
        assertThat(state.days.map { it.isNext }).containsExactly(true, false, false).inOrder()

        viewModel.onAction(ProgramAction.Follow(isActive = false))
        assertThat(stored.isActive).isFalse()
    }

    @Test
    fun aRemovedDayComesBackWithUndo() = runTest {
        val viewModel = programViewModel()

        viewModel.onAction(ProgramAction.RemoveDay(ProgramDayId("d1")))
        assertThat(stored.days.map { it.name }).containsExactly("Pull")
        assertThat((viewModel.uiState.value as ProgramUiState.Ready).canUndo).isTrue()

        viewModel.onAction(ProgramAction.Undo)
        assertThat(stored.days.map { it.name }).containsExactly("Push", "Pull").inOrder()
        assertThat((viewModel.uiState.value as ProgramUiState.Ready).canUndo).isFalse()
    }

    @Test
    fun deletingClosesThePage() = runTest {
        val viewModel = programViewModel()

        viewModel.onAction(ProgramAction.Delete)

        assertThat(viewModel.uiState.value).isEqualTo(ProgramUiState.Gone)
    }

    @Test
    fun theTrainingMaxIsSuggestedFromTheHistory() = runTest {
        val squat = ProgramTemplates.of(TemplateKey.FIVE_THREE_ONE).trainingMaxLifts.last()
        val library = FakeExerciseRepository(
            listOf(CatalogExercise(squat, "Squat", type = ExerciseType.WEIGHT_REPS, equipment = Equipment.BARBELL)),
        )
        val progress = FakeProgressRepository()
        val set = LoggedSet(SetType.WORKING, SetValues(weight = Weight(100.0), reps = 5))
        progress.sessions.value = mapOf(squat to listOf(ExerciseSession(WorkoutId("w"), NOW - 1, listOf(set))))
        val settings = FakeSettingsRepository()
        val viewModel = TemplateViewModel(
            savedStateHandle = SavedStateHandle(mapOf("key" to TemplateKey.FIVE_THREE_ONE.name)),
            exercises = library,
            progress = progress,
            settings = settings,
            createProgram = CreateProgram(programs, routines, library, settings, ids, clock),
            clock = clock,
        )

        val state = viewModel.uiState.value as TemplateUiState.Ready
        // 100 kg × 5 is an estimated 116.7 kg; 90% of it, in whole steps, is 105 kg.
        assertThat(state.lifts.single().suggestion).isEqualTo(Weight(105.0))

        viewModel.create("5/3/1", mapOf(TemplateDayName.SQUAT to "Squat day"), mapOf(squat to Weight(105.0)))
        val created = (viewModel.uiState.value as TemplateUiState.Ready).created
        assertThat(created).isEqualTo(programs.all.last().id)
        assertThat(programs.all.last().isActive).isTrue()
    }

    private fun TestScope.programViewModel(): ProgramViewModel {
        val viewModel = ProgramViewModel(
            savedStateHandle = SavedStateHandle(mapOf("programId" to "p")),
            programs = programs,
            ids = ids,
            work = work(),
            routines = routines,
        )
        backgroundScope.launch(mainDispatcherRule.dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TestScope.work() =
        SequentialWork(CoroutineScope(backgroundScope.coroutineContext + mainDispatcherRule.dispatcher))

    private companion object {
        const val NOW = 1_700_000_000_000L
        val PUSH = RoutineId("push")
        val PULL = RoutineId("pull")
    }
}
