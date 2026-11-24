package io.github.amibhai.liftlog.core.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.database.dao.ExerciseDao
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.search.FtsQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var database: LiftLogDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LiftLogDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        dao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun searchMatchesWordPrefixesOfTheName() = runTest {
        dao.insert(exercise("Bench Press"))
        dao.insert(exercise("Incline Bench Press"))
        dao.insert(exercise("Back Squat"))

        assertThat(search("ben")).containsExactly("Bench Press", "Incline Bench Press")
    }

    @Test
    fun searchMatchesAliasesAndGeneratedSearchTerms() = runTest {
        dao.insert(exercise("Romanian Deadlift", searchTerms = listOf("barbell", "bb", "dl", "rdl")))
        dao.insert(exercise("Lying Triceps Extension", aliases = listOf("Skullcrusher")))

        assertThat(search("rdl")).containsExactly("Romanian Deadlift")
        assertThat(search("skull")).containsExactly("Lying Triceps Extension")
        assertThat(search("bb romanian")).containsExactly("Romanian Deadlift")
    }

    @Test
    fun searchIgnoresDiacritics() = runTest {
        dao.insert(exercise("Développé Couché"))

        assertThat(search("developpe")).containsExactly("Développé Couché")
    }

    @Test
    fun archivedAndDeletedExercisesAreHiddenFromSearchAndCounts() = runTest {
        dao.insert(exercise("Leg Press"))
        dao.insert(exercise("Leg Extension", isArchived = true))
        dao.insert(exercise("Leg Curl", isCustom = true, deletedAt = NOW))

        assertThat(search("leg")).containsExactly("Leg Press")
        assertThat(dao.observeActiveCount().first()).isEqualTo(1)
    }

    @Test
    fun ftsIndexFollowsRenames() = runTest {
        dao.insert(exercise("Bench Press", id = "bench"))

        dao.update(dao.get("bench")!!.copy(name = "Paused Bench Press"))

        assertThat(search("paused")).containsExactly("Paused Bench Press")
        assertThat(search("ben")).containsExactly("Paused Bench Press")
    }

    @Test
    fun softDeleteOnlyAppliesToCustomExercises() = runTest {
        dao.insert(exercise("Built In Row", id = "builtin"))
        dao.insert(exercise("My Row", id = "custom", isCustom = true))

        dao.softDeleteCustom("builtin", NOW)
        dao.softDeleteCustom("custom", NOW)

        assertThat(search("row")).containsExactly("Built In Row")
        assertThat(dao.observeCustomCount().first()).isEqualTo(0)
    }

    @Test
    fun theCatalogHasPrimaryMusclesAndFirstImagesOfVisibleExercises() = runTest {
        dao.insert(exercise("Bench Press", id = "bench", aliases = listOf("Flat Press"), popularityRank = 1))
        dao.insert(exercise("Hidden Press", id = "hidden", isArchived = true))
        dao.upsertMuscles(
            listOf(
                muscle("bench", Muscle.CHEST, isPrimary = true),
                muscle("bench", Muscle.TRICEPS, isPrimary = false),
            ),
        )
        dao.insertImages(listOf(image("bench", 1, "https://img/1.jpg"), image("bench", 0, "https://img/0.jpg")))

        val row = dao.observeCatalog().first().single()

        assertThat(row.id).isEqualTo("bench")
        assertThat(row.aliases).isEqualTo("Flat Press")
        assertThat(row.popularityRank).isEqualTo(1)
        assertThat(dao.observePrimaryMuscles().first().map { it.muscle }).containsExactly(Muscle.CHEST)
        assertThat(dao.observeThumbnails().first().single().url).isEqualTo("https://img/0.jpg")
    }

    @Test
    fun userChoicesAreStored() = runTest {
        dao.insert(exercise("Bench Press", id = "bench"))

        dao.setFavorite("bench", favorite = true, now = NOW)
        dao.setNotes("bench", "Elbows in", NOW)
        dao.setDefaults("bench", restSeconds = 180, stepKg = 5.0, stepLb = 10.0, barKg = 15.0, now = NOW)

        val stored = dao.get("bench")!!
        assertThat(stored.isFavorite).isTrue()
        assertThat(stored.notes).isEqualTo("Elbows in")
        val info = dao.infos(listOf("bench", "missing")).single()
        assertThat(info.defaultRestSeconds).isEqualTo(180)
        assertThat(info.weightStepKg).isEqualTo(5.0)
        assertThat(info.weightStepLb).isEqualTo(10.0)
        assertThat(info.barWeightKg).isEqualTo(15.0)
    }

    @Test
    fun detailIncludesMusclesAndImages() = runTest {
        dao.insert(exercise("Bench Press", id = "bench"))
        dao.upsertMuscles(listOf(muscle("bench", Muscle.CHEST, isPrimary = true)))
        dao.insertImages(listOf(image("bench", 0, "https://img/0.jpg")))

        val detail = dao.observeWithDetails("bench").first()!!

        assertThat(detail.exercise.name).isEqualTo("Bench Press")
        assertThat(detail.muscles.map { it.muscle }).containsExactly(Muscle.CHEST)
        assertThat(detail.images.map { it.url }).containsExactly("https://img/0.jpg")
    }

    /** The names found, in no particular order. */
    private suspend fun search(query: String): List<String> {
        val ids = dao.matchingIds(FtsQuery.prefix(query)!!)
        return dao.getAll().filter { it.id in ids }.map { it.name }
    }
}
