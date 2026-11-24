package io.github.amibhai.liftlog.core.data.exercise

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseImageEntity
import io.github.amibhai.liftlog.core.database.entity.ExerciseMuscleEntity
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.LibraryStats
import io.github.amibhai.liftlog.core.domain.model.Muscle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomExerciseRepositoryTest {

    private lateinit var database: LiftLogDatabase
    private lateinit var repository: RoomExerciseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LiftLogDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        repository = RoomExerciseRepository(database.exerciseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun blankQueryHasNoResults() = runTest {
        insertBench()

        assertThat(repository.search("   ").first()).isEmpty()
    }

    @Test
    fun searchReturnsSummariesWithPrimaryMuscleAndThumbnail() = runTest {
        insertBench()

        val result = repository.search("bench").first().single()

        assertThat(result.id).isEqualTo(ExerciseId("bench"))
        assertThat(result.name).isEqualTo("Barbell Bench Press")
        assertThat(result.primaryMuscle).isEqualTo(Muscle.CHEST)
        assertThat(result.thumbnailUrl).isEqualTo("https://img/thumb0.png")
    }

    @Test
    fun detailSplitsAliasesAndStepsAndCreditsTheSource() = runTest {
        insertBench()

        val detail = repository.observeDetail(ExerciseId("bench")).first()!!

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
        database.exerciseDao().insert(exercise(id = "mine", name = "My Press", sourceRef = null, isCustom = true))

        assertThat(repository.observeDetail(ExerciseId("mine")).first()!!.attribution).isNull()
    }

    @Test
    fun statsCountActiveAndCustomExercises() = runTest {
        insertBench()
        database.exerciseDao().insert(exercise(id = "mine", name = "My Press", sourceRef = null, isCustom = true))

        assertThat(repository.observeStats().first()).isEqualTo(LibraryStats(totalExercises = 2, customExercises = 1))
    }

    private suspend fun insertBench() {
        val dao = database.exerciseDao()
        dao.insert(
            exercise(
                id = "bench",
                name = "Barbell Bench Press",
                aliases = "Bench Press\nFlat Bench",
                instructions = "Lie on the bench.\nPress the bar.",
                sourceRef = "wger:abc",
            ),
        )
        dao.insertMuscles(listOf(muscle("bench", Muscle.CHEST, true), muscle("bench", Muscle.TRICEPS, false)))
        dao.insertImages(listOf(image("bench", 1, "https://img/1.jpg"), image("bench", 0, "https://img/0.jpg")))
    }

    private fun exercise(
        id: String,
        name: String,
        aliases: String = "",
        instructions: String = "",
        sourceRef: String?,
        isCustom: Boolean = false,
    ) = ExerciseEntity(
        id = id,
        name = name,
        aliases = aliases,
        searchTerms = "",
        type = ExerciseType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        category = ExerciseCategory.STRENGTH,
        mechanic = null,
        instructions = instructions,
        isUnilateral = false,
        defaultRestSeconds = null,
        weightStepKg = null,
        weightStepLb = null,
        barWeightKg = null,
        notes = null,
        isFavorite = false,
        isArchived = false,
        isCustom = isCustom,
        popularityRank = null,
        sourceRef = sourceRef,
        license = "CC-BY-SA 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/",
        licenseAuthor = "someone",
        libraryVersion = 1,
        userModifiedAt = null,
        createdAt = NOW,
        updatedAt = NOW,
        deletedAt = null,
    )

    private fun muscle(exerciseId: String, muscle: Muscle, isPrimary: Boolean) = ExerciseMuscleEntity(
        id = "$exerciseId-$muscle",
        exerciseId = exerciseId,
        muscle = muscle,
        isPrimary = isPrimary,
        createdAt = NOW,
        updatedAt = NOW,
        deletedAt = null,
    )

    private fun image(exerciseId: String, position: Int, url: String) = ExerciseImageEntity(
        id = "$exerciseId-$position",
        exerciseId = exerciseId,
        position = position,
        url = url,
        thumbnailUrl = "https://img/thumb$position.png",
        license = null,
        author = null,
        createdAt = NOW,
        updatedAt = NOW,
        deletedAt = null,
    )

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
