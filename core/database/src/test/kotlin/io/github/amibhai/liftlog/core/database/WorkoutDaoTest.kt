package io.github.amibhai.liftlog.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.amibhai.liftlog.core.database.entity.RoutineEntity
import io.github.amibhai.liftlog.core.domain.model.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var database: LiftLogDatabase
    private val workouts get() = database.workoutDao()
    private val history get() = database.historyDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LiftLogDatabase::class.java,
        ).setDriver(AndroidSQLiteDriver()).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun onlyTheWorkoutInProgressIsActive() = runTest {
        add("done", daysAgo = 1)
        assertThat(workouts.observeActive().first()).isNull()

        add("now", daysAgo = 0, inProgress = true)

        val active = workouts.observeActive().first()!!
        assertThat(active.workout.id).isEqualTo("now")
        assertThat(active.exercises.single().exercise.name).isEqualTo("Bench")
        assertThat(active.exercises.single().sets).hasSize(2)
    }

    @Test
    fun finishedWorkoutsAreNewestFirstAndDeletedOnesCanBeRestored() = runTest {
        add("old", daysAgo = 10)
        add("new", daysAgo = 1)
        add("now", daysAgo = 0, inProgress = true)

        assertThat(workouts.observeFinished(10).first().map { it.workout.id }).containsExactly("new", "old").inOrder()
        assertThat(workouts.observeFinished(1).first().map { it.workout.id }).containsExactly("new")

        workouts.setDeleted("new", deletedAt = NOW, now = NOW)
        assertThat(workouts.observeFinishedCount().first()).isEqualTo(1)

        workouts.setDeleted("new", deletedAt = null, now = NOW)
        assertThat(workouts.observeFinishedCount().first()).isEqualTo(2)
    }

    @Test
    fun theLatestSessionOfEachExerciseIsThePreviousOne() = runTest {
        add("older", daysAgo = 8, exercises = listOf("bench", "row"), kg = 70.0)
        add("newer", daysAgo = 1, exercises = listOf("bench"), kg = 80.0)
        add("now", daysAgo = 0, inProgress = true, kg = 100.0)

        val rows = history.latestSets(before = NOW)

        assertThat(rows.filter { it.exerciseId == "bench" }.map { it.weightKg }).containsExactly(80.0, 80.0)
        assertThat(rows.filter { it.exerciseId == "row" }.map { it.workoutId }.distinct()).containsExactly("older")
        assertThat(history.latestSets(before = NOW, ids = listOf("row")).map { it.exerciseId }.distinct())
            .containsExactly("row")
        // Editing an old workout compares with what came before it.
        assertThat(
            history.latestSets(before = NOW - 1.days.inWholeMilliseconds, ids = listOf("bench")).map { it.weightKg },
        )
            .containsExactly(70.0, 70.0)
    }

    @Test
    fun sessionsWithoutACompletedSetDoNotCount() = runTest {
        add("real", daysAgo = 5, kg = 80.0)
        add("empty", daysAgo = 1, kg = 90.0, completed = false)

        assertThat(history.latestSets(before = NOW).map { it.workoutId }.distinct()).containsExactly("real")
        assertThat(history.usages(since = 0).map { it.workoutId }).containsExactly("real")
    }

    @Test
    fun deletedSetsExercisesAndWorkoutsDoNotCount() = runTest {
        add("w1", daysAgo = 3, exercises = listOf("bench", "row"))
        add("w2", daysAgo = 2)

        workouts.softDeleteExercises(listOf("w1-row"), NOW)
        workouts.softDeleteSets(listOf("w1-bench-0"), NOW)
        workouts.setDeleted("w2", NOW, NOW)

        val rows = history.latestSets(before = NOW)
        assertThat(rows.map { it.workoutId to it.exerciseId }).containsExactly("w1" to "bench")
    }

    @Test
    fun usagesStartAtTheGivenTime() = runTest {
        add("old", daysAgo = 400)
        add("new", daysAgo = 2, exercises = listOf("bench", "row"))

        val usages = history.usages(since = NOW - 300.days.inWholeMilliseconds)

        assertThat(usages.map { it.exerciseId to it.workoutId }).containsExactly("bench" to "new", "row" to "new")
    }

    @Test
    fun exerciseHistoryListsTheLatestSessionsFirst() = runTest {
        for (day in 1..5) add("w$day", daysAgo = day, kg = 80.0 + day)

        val rows = history.observeSessionSets("bench", limit = 2).first()

        assertThat(rows.map { it.workoutId }).containsExactly("w1", "w1", "w2", "w2").inOrder()
    }

    @Test
    fun routineUseIsTheLastFinishedWorkout() = runTest {
        database.routineDao().upsertRoutine(RoutineEntity("push", "Push", null, 0, NOW, NOW, null))
        add("w1", daysAgo = 9, routine = "push")
        add("w2", daysAgo = 2, routine = "push")
        add("now", daysAgo = 0, inProgress = true, routine = "push")

        assertThat(history.observeRoutineUse().first().single().at).isEqualTo(NOW - 2.days.inWholeMilliseconds)
    }

    @Test
    fun historyQueriesUseIndexesAndStayFastWithYearsOfTraining() = runTest {
        val ids = (0 until 30).map { "e$it" }
        ids.forEach { database.exerciseDao().insert(exercise(it, id = it)) }
        for (day in 0 until 800) {
            addWorkout(
                "y$day",
                daysAgo = day * 2 + 1,
                exercises = (0 until 6).map { ids[(day + it * 5) % 30] },
                sets = 4,
            )
        }

        val since = NOW - 300.days.inWholeMilliseconds
        history.usages(since)
        val usages = measureTime { history.usages(since) }
        val previous = measureTime { history.latestSets(NOW, ids.take(8)) }
        val all = measureTime { history.latestSets(NOW) }

        // A development machine is several times faster than a phone; the target there is 16 ms.
        assertWithMessage("usages").that(usages.inWholeMilliseconds).isLessThan(150)
        assertWithMessage("previous").that(previous.inWholeMilliseconds).isLessThan(150)
        assertWithMessage("all").that(all.inWholeMilliseconds).isLessThan(300)
        assertThat(queryPlan("SELECT * FROM workout_set WHERE workout_exercise_id = 'x' ORDER BY position"))
            .contains("USING INDEX index_workout_set_workout_exercise_id_position")
        assertThat(queryPlan("SELECT * FROM workout_exercise WHERE exercise_id IN ('a', 'b')"))
            .contains("USING INDEX index_workout_exercise_exercise_id_workout_id")
    }

    /** The plan for [sql], read through the framework: Room's test driver only runs SELECT statements as queries. */
    private suspend fun queryPlan(sql: String): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val onDisk = Room.databaseBuilder(context, LiftLogDatabase::class.java, "plan.db")
            .setDriver(AndroidSQLiteDriver())
            .build()
        onDisk.exerciseDao().getAll()
        onDisk.close()
        return SQLiteDatabase.openDatabase(context.getDatabasePath("plan.db").path, null, SQLiteDatabase.OPEN_READONLY)
            .use { db ->
                db.rawQuery("EXPLAIN QUERY PLAN $sql", null).use { row ->
                    buildString { while (row.moveToNext()) appendLine(row.getString(3)) }
                }
            }
    }

    private suspend fun add(
        id: String,
        daysAgo: Int,
        inProgress: Boolean = false,
        exercises: List<String> = listOf("bench"),
        kg: Double = 80.0,
        completed: Boolean = true,
        routine: String? = null,
    ) {
        for (exercise in exercises) {
            if (database.exerciseDao().get(exercise) == null) {
                database.exerciseDao().insert(exercise(exercise.replaceFirstChar(Char::uppercase), id = exercise))
            }
        }
        addWorkout(id, daysAgo, exercises, sets = 2, inProgress, kg, completed, routine)
    }

    private suspend fun addWorkout(
        id: String,
        daysAgo: Int,
        exercises: List<String>,
        sets: Int,
        inProgress: Boolean = false,
        kg: Double = 80.0,
        completed: Boolean = true,
        routine: String? = null,
    ) {
        workouts.upsertWorkout(workout(id, NOW - daysAgo.days.inWholeMilliseconds, inProgress, routine))
        workouts.upsertExercises(
            exercises.mapIndexed { i, exercise -> workoutExercise("$id-$exercise", id, exercise, i) },
        )
        workouts.upsertSets(
            exercises.flatMap { exercise ->
                (0 until sets).map {
                    workoutSet("$id-$exercise-$it", "$id-$exercise", it, SetType.WORKING, kg, reps = 8, completed)
                }
            },
        )
    }
}
