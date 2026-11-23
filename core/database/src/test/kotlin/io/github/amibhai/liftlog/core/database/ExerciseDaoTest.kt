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
import io.github.amibhai.liftlog.core.domain.search.SearchText
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

        assertThat(search("ben").map { it.name }).containsExactly("Bench Press", "Incline Bench Press").inOrder()
    }

    @Test
    fun searchMatchesAliasesAndGeneratedSearchTerms() = runTest {
        dao.insert(exercise("Romanian Deadlift", searchTerms = "rdl dl bb barbell"))
        dao.insert(exercise("Lying Triceps Extension", aliases = listOf("Skullcrusher")))

        assertThat(search("rdl").map { it.name }).containsExactly("Romanian Deadlift")
        assertThat(search("skull").map { it.name }).containsExactly("Lying Triceps Extension")
        assertThat(search("bb romanian").map { it.name }).containsExactly("Romanian Deadlift")
    }

    @Test
    fun exactNameRanksFirstThenNamePrefixThenOtherMatches() = runTest {
        dao.insert(exercise("Front Squat"))
        dao.insert(exercise("Squat Jump"))
        dao.insert(exercise("Squat"))

        assertThat(search("squat").map { it.name }).containsExactly("Squat", "Squat Jump", "Front Squat").inOrder()
    }

    @Test
    fun popularityBreaksTiesBeforeName() = runTest {
        dao.insert(exercise("Barbell Curl", popularityRank = 20))
        dao.insert(exercise("Alternate Hammer Curl"))
        dao.insert(exercise("Cable Curl", popularityRank = 5))

        assertThat(search("curl").map { it.name })
            .containsExactly("Cable Curl", "Barbell Curl", "Alternate Hammer Curl").inOrder()
    }

    @Test
    fun searchIgnoresDiacritics() = runTest {
        dao.insert(exercise("Développé Couché"))

        assertThat(search("developpe").map { it.name }).containsExactly("Développé Couché")
    }

    @Test
    fun archivedAndDeletedExercisesAreHiddenFromSearchAndCounts() = runTest {
        dao.insert(exercise("Leg Press"))
        dao.insert(exercise("Leg Extension", isArchived = true))
        dao.insert(exercise("Leg Curl", isCustom = true, deletedAt = NOW))

        assertThat(search("leg").map { it.name }).containsExactly("Leg Press")
        assertThat(dao.observeActiveCount().first()).isEqualTo(1)
    }

    @Test
    fun ftsIndexFollowsRenames() = runTest {
        dao.insert(exercise("Bench Press", id = "bench"))

        dao.rename(id = "bench", name = "Paused Bench Press", now = NOW + 1)

        assertThat(search("paused").map { it.id }).containsExactly("bench")
        assertThat(search("ben").map { it.name }).containsExactly("Paused Bench Press")
    }

    @Test
    fun softDeleteOnlyAppliesToCustomExercises() = runTest {
        dao.insert(exercise("Built In Row", id = "builtin"))
        dao.insert(exercise("My Row", id = "custom", isCustom = true))

        dao.softDeleteCustom("builtin", NOW)
        dao.softDeleteCustom("custom", NOW)

        assertThat(search("row").map { it.id }).containsExactly("builtin")
        assertThat(dao.observeCustomCount().first()).isEqualTo(0)
    }

    @Test
    fun searchRowsCarryPrimaryMuscleAndFirstImage() = runTest {
        dao.insert(exercise("Bench Press", id = "bench"))
        dao.insertMuscles(
            listOf(
                muscle("bench", Muscle.CHEST, isPrimary = true),
                muscle("bench", Muscle.TRICEPS, isPrimary = false),
            ),
        )
        dao.insertImages(listOf(image("bench", 1, "https://img/1.jpg"), image("bench", 0, "https://img/0.jpg")))

        val row = search("bench").single()

        assertThat(row.primaryMuscle).isEqualTo(Muscle.CHEST)
        assertThat(row.thumbnailUrl).isEqualTo("https://img/0.jpg")
    }

    @Test
    fun detailIncludesMusclesAndImages() = runTest {
        dao.insert(exercise("Bench Press", id = "bench"))
        dao.insertMuscles(listOf(muscle("bench", Muscle.CHEST, isPrimary = true)))
        dao.insertImages(listOf(image("bench", 0, "https://img/0.jpg")))

        val detail = dao.observeWithDetails("bench").first()!!

        assertThat(detail.exercise.name).isEqualTo("Bench Press")
        assertThat(detail.muscles.map { it.muscle }).containsExactly(Muscle.CHEST)
        assertThat(detail.images.map { it.url }).containsExactly("https://img/0.jpg")
    }

    private suspend fun search(query: String) = dao.search(
        match = FtsQuery.prefix(query)!!,
        exactName = SearchText.normalize(query),
        namePrefix = SearchText.normalize(query) + "%",
        limit = 50,
    ).first()
}
