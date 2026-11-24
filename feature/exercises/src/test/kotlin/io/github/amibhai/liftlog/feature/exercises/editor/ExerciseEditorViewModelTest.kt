package io.github.amibhai.liftlog.feature.exercises.editor

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.testing.FakeExerciseEditor
import io.github.amibhai.liftlog.core.testing.FakeExerciseRepository
import io.github.amibhai.liftlog.core.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val editor = FakeExerciseEditor()

    @Test
    fun aNewExerciseStartsWithTheNameThatWasSearched() {
        val viewModel = viewModel(name = "Landmine Press")

        assertThat(viewModel.uiState.value.draft.name).isEqualTo("Landmine Press")
        assertThat(viewModel.uiState.value.isNew).isTrue()
    }

    @Test
    fun itNeedsANameAndAPrimaryMuscle() {
        val viewModel = viewModel(name = "Landmine Press")

        viewModel.save()
        assertThat(editor.saved).isEmpty()

        viewModel.togglePrimary(Muscle.CHEST)
        viewModel.save()
        assertThat(editor.saved.single().primaryMuscles).containsExactly(Muscle.CHEST)
        assertThat(viewModel.uiState.value.savedId).isEqualTo(ExerciseId("new-1"))
    }

    @Test
    fun aMuscleIsEitherPrimaryOrSecondary() {
        val viewModel = viewModel()

        viewModel.togglePrimary(Muscle.CHEST)
        viewModel.toggleSecondary(Muscle.CHEST)

        assertThat(viewModel.uiState.value.draft.primaryMuscles).isEmpty()
        assertThat(viewModel.uiState.value.draft.secondaryMuscles).containsExactly(Muscle.CHEST)
    }

    @Test
    fun editingLoadsTheExerciseAndKeepsItsId() {
        val viewModel = viewModel(exerciseId = "bench")

        val draft = viewModel.uiState.value.draft
        assertThat(viewModel.uiState.value.isNew).isFalse()
        assertThat(draft.name).isEqualTo("Bench")
        assertThat(draft.instructions).containsExactly("Press.")

        viewModel.change { it.copy(name = "Paused Bench") }
        viewModel.save()
        assertThat(editor.saved.single().id).isEqualTo(ExerciseId("bench"))
        assertThat(editor.saved.single().name).isEqualTo("Paused Bench")
    }

    private fun viewModel(name: String? = null, exerciseId: String? = null) = ExerciseEditorViewModel(
        SavedStateHandle(mapOf("name" to name, "exerciseId" to exerciseId)),
        FakeExerciseRepository(details = mapOf(ExerciseId("bench") to bench)),
        editor,
    )

    private val bench = ExerciseDetail(
        id = ExerciseId("bench"),
        name = "Bench",
        aliases = emptyList(),
        type = ExerciseType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        category = ExerciseCategory.STRENGTH,
        mechanic = null,
        primaryMuscles = listOf(Muscle.CHEST),
        secondaryMuscles = emptyList(),
        instructions = listOf("Press."),
        images = emptyList(),
        isUnilateral = false,
        isCustom = false,
        isFavorite = false,
        isArchived = false,
        notes = null,
        attribution = null,
    )
}
