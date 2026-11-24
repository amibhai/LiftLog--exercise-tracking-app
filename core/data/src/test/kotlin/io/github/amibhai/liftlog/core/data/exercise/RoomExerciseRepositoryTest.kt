package io.github.amibhai.liftlog.core.data.exercise

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.data.CountingIds
import io.github.amibhai.liftlog.core.data.NOW
import io.github.amibhai.liftlog.core.data.TestClock
import io.github.amibhai.liftlog.core.data.TestDatabase
import io.github.amibhai.liftlog.core.data.exerciseEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseImageEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.units.Weight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomExerciseRepositoryTest {

    @get:Rule
    val db = TestDatabase()

    private val dao get() = db.database.exerciseDao()

    @Test
    fun blankQueriesMatchNothing() = runTest {
        insertBench()

        assertThat(repository().matching("   ")).isEmpty()
        assertThat(repository().matching("ben")).containsExactly(ExerciseId("bench"))
    }

    @Test
    fun theCatalogHasAliasesMusclesAndThumbnails() = runTest {
        insertBench()

        val entry = repository().observeCatalog().first().single()

        assertThat(entry.name).isEqualTo("Barbell Bench Press")
        assertThat(entry.aliases).containsExactly("Bench Press", "Flat Bench").inOrder()
        assertThat(entry.primaryMuscles).containsExactly(Muscle.CHEST)
        assertThat(entry.thumbnailUrl).isEqualTo("https://img/thumb0.png")
    }

    @Test
    fun detailSplitsAliasesAndStepsAndCreditsTheSource() = runTest {
        insertBench()

        val detail = repository().observeDetail(ExerciseId("bench")).first()!!

        assertThat(detail.aliases).containsExactly("Bench Press", "Flat Bench").inOrder()
        assertThat(detail.instructions).containsExactly("Lie on the bench.", "Press the bar.").inOrder()
        assertThat(detail.primaryMuscles).containsExactly(Muscle.CHEST)
        assertThat(detail.secondaryMuscles).containsExactly(Muscle.TRICEPS)
        assertThat(detail.images.map { it.url }).containsExactly("https://img/0.jpg", "https://img/1.jpg").inOrder()
        assertThat(detail.attribution!!.sourceName).isEqualTo("wger")
        assertThat(detail.attribution!!.author).isEqualTo("someone")
    }

    @Test
    fun customExercisesHaveNoAttribution() = runTest {
        dao.insert(exerciseEntity(id = "mine", name = "My Press", sourceRef = null, isCustom = true))

        assertThat(repository().observeDetail(ExerciseId("mine")).first()!!.attribution).isNull()
    }

    @Test
    fun statsCountActiveAndCustomExercises() = runTest {
        insertBench()
        dao.insert(exerciseEntity(id = "mine", name = "My Press", sourceRef = null, isCustom = true))

        assertThat(repository().observeStats().first()).isEqualTo(LibraryStats(totalExercises = 2, customExercises = 1))
    }

    @Test
    fun infosKeepTheRequestedOrder() = runTest {
        db.addExercise("a")
        db.addExercise("b")

        val infos = repository().infos(listOf(ExerciseId("b"), ExerciseId("gone"), ExerciseId("a")))

        assertThat(infos.map { it.name }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun aNewCustomExerciseCanBeFoundByItsGroupWords() = runTest {
        val id = editor().save(
            ExerciseDraft(
                name = " Landmine Press ",
                equipment = Equipment.DUMBBELL,
                primaryMuscles = listOf(Muscle.CHEST, Muscle.FRONT_DELTS),
                secondaryMuscles = listOf(Muscle.TRICEPS),
                instructions = listOf("Press.", ""),
            ),
        )

        val detail = repository().observeDetail(id).first()!!
        assertThat(detail.name).isEqualTo("Landmine Press")
        assertThat(detail.isCustom).isTrue()
        assertThat(detail.primaryMuscles).containsExactly(Muscle.CHEST, Muscle.FRONT_DELTS)
        assertThat(detail.secondaryMuscles).containsExactly(Muscle.TRICEPS)
        assertThat(detail.instructions).containsExactly("Press.")
        assertThat(repository().matching("db chest")).containsExactly(id)
    }

    @Test
    fun editingKeepsTheIdAndCanBringAMuscleBack() = runTest {
        insertBench()
        val id = ExerciseId("bench")
        val draft = ExerciseDraft(id, "Bench", equipment = Equipment.BARBELL, primaryMuscles = listOf(Muscle.TRICEPS))

        assertThat(editor().save(draft)).isEqualTo(id)
        val changed = repository().observeDetail(id).first()!!
        assertThat(changed.name).isEqualTo("Bench")
        assertThat(changed.isCustom).isFalse()
        assertThat(changed.primaryMuscles).containsExactly(Muscle.TRICEPS)
        assertThat(changed.secondaryMuscles).isEmpty()

        editor().save(draft.copy(primaryMuscles = listOf(Muscle.CHEST), secondaryMuscles = listOf(Muscle.TRICEPS)))
        val back = repository().observeDetail(id).first()!!
        assertThat(back.primaryMuscles).containsExactly(Muscle.CHEST)
        assertThat(back.secondaryMuscles).containsExactly(Muscle.TRICEPS)
    }

    @Test
    fun hiddenExercisesLeaveTheCatalogAndSearch() = runTest {
        insertBench()

        editor().setHidden(ExerciseId("bench"), hidden = true)

        assertThat(repository().observeCatalog().first()).isEmpty()
        assertThat(repository().matching("bench")).isEmpty()
        assertThat(repository().observeDetail(ExerciseId("bench")).first()!!.isArchived).isTrue()
    }

    @Test
    fun personalSettingsOfAnExerciseAreStored() = runTest {
        db.addExercise("bench", type = ExerciseType.WEIGHT_REPS)
        val id = ExerciseId("bench")
        val defaults = ExerciseDefaults(180, weightStepKg = 5.0, weightStepLb = 10.0, barWeight = Weight(15.0))

        editor().setFavorite(id, favorite = true)
        editor().setStickyNote(id, "  Elbows in ")
        editor().setDefaults(id, defaults)

        val info = repository().infos(listOf(id)).single()
        assertThat(info.stickyNote).isEqualTo("Elbows in")
        assertThat(info.defaults).isEqualTo(defaults)
        assertThat(repository().observeCatalog().first().single().isFavorite).isTrue()

        editor().setStickyNote(id, " ")
        assertThat(repository().infos(listOf(id)).single().stickyNote).isNull()
    }

    private fun TestScope.repository() =
        RoomExerciseRepository(dao, CoroutineScope(backgroundScope.coroutineContext), Dispatchers.IO)

    private fun editor() = RoomExerciseEditor(db.database, dao, CountingIds(), TestClock())

    private suspend fun insertBench() {
        dao.insert(
            exerciseEntity(
                id = "bench",
                name = "Barbell Bench Press",
                aliases = "Bench Press\nFlat Bench",
                instructions = "Lie on the bench.\nPress the bar.",
                sourceRef = "wger:abc",
            ),
        )
        dao.upsertMuscles(listOf(muscle(Muscle.CHEST, isPrimary = true), muscle(Muscle.TRICEPS, isPrimary = false)))
        dao.insertImages(listOf(image(1, "https://img/1.jpg"), image(0, "https://img/0.jpg")))
    }

    private fun muscle(muscle: Muscle, isPrimary: Boolean) = ExerciseMuscleEntity(
        id = "bench-$muscle",
        exerciseId = "bench",
        muscle = muscle,
        isPrimary = isPrimary,
        createdAt = NOW,
        updatedAt = NOW,
        deletedAt = null,
    )

    private fun image(position: Int, url: String) = ExerciseImageEntity(
        id = "bench-$position",
        exerciseId = "bench",
        position = position,
        url = url,
        thumbnailUrl = "https://img/thumb$position.png",
        license = null,
        author = null,
        createdAt = NOW,
        updatedAt = NOW,
        deletedAt = null,
    )
}
