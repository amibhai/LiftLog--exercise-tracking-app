package io.github.amibhai.liftlog.core.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.amibhai.liftlog.core.data.CountingIds
import io.github.amibhai.liftlog.core.data.NOW
import io.github.amibhai.liftlog.core.data.TestClock
import io.github.amibhai.liftlog.core.data.TestDatabase
import io.github.amibhai.liftlog.core.data.exercise.RoomExerciseEditor
import io.github.amibhai.liftlog.core.data.program.RoomProgramRepository
import io.github.amibhai.liftlog.core.data.routine.RoomRoutineRepository
import io.github.amibhai.liftlog.core.data.settings.SettingsJson
import io.github.amibhai.liftlog.core.data.settings.SettingsSerializer
import io.github.amibhai.liftlog.core.data.workout.RoomWorkoutRepository
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseDefaults
import io.github.amibhai.liftlog.core.domain.model.ExerciseDraft
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.domain.model.ExerciseInfo
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.domain.model.Program
import io.github.amibhai.liftlog.core.domain.model.ProgramDay
import io.github.amibhai.liftlog.core.domain.model.ProgramDayId
import io.github.amibhai.liftlog.core.domain.model.ProgramId
import io.github.amibhai.liftlog.core.domain.model.ProgressionRule
import io.github.amibhai.liftlog.core.domain.model.Routine
import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.RoutineExerciseId
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.core.domain.model.SetValues
import io.github.amibhai.liftlog.core.domain.model.Workout
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExerciseId
import io.github.amibhai.liftlog.core.domain.model.WorkoutId
import io.github.amibhai.liftlog.core.domain.model.WorkoutSet
import io.github.amibhai.liftlog.core.domain.model.WorkoutSetId
import io.github.amibhai.liftlog.core.domain.units.Weight
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupStoreTest {

    @get:Rule
    val db = TestDatabase()

    @get:Rule
    val folder = TemporaryFolder()

    private val clock = TestClock()
    private val ids = CountingIds()
    private val bench = ExerciseInfo(ExerciseId("bench"), "bench", ExerciseType.WEIGHT_REPS, Equipment.BARBELL)
    private val push = Routine(
        RoutineId("push"),
        "Push",
        exercises = listOf(
            RoutineExercise(RoutineExerciseId("slot"), bench, progression = ProgressionRule.Linear(incrementKg = 5.0)),
        ),
    )
    private val program = Program(
        ProgramId("p"),
        "Split",
        isActive = true,
        days = listOf(ProgramDay(ProgramDayId("d"), "Push", push.id)),
    )
    private val done = Workout(
        id = WorkoutId("w"),
        name = "Push",
        startedAt = NOW,
        endedAt = NOW + 1,
        isInProgress = false,
        routineId = push.id,
        programDayId = ProgramDayId("d"),
        exercises = listOf(
            WorkoutExercise(
                id = WorkoutExerciseId("we"),
                exercise = bench,
                routineExerciseId = RoutineExerciseId("slot"),
                sets = listOf(
                    WorkoutSet(
                        id = WorkoutSetId("s"),
                        entered = SetValues(weight = Weight(82.5), reps = 8),
                        rpe = 8.5,
                        isCompleted = true,
                        completedAt = NOW,
                    ),
                ),
            ),
        ),
    )

    @Test
    fun everythingComesBackAsItWas() = runTest {
        val settings = dataStore()
        val store = store(settings)
        fill()
        settings.updateData { it.copy(weightUnit = WeightUnit.LB) }
        val custom = exercises().save(
            ExerciseDraft(name = "My lift", equipment = Equipment.OTHER, primaryMuscles = listOf(Muscle.CHEST)),
        )
        exercises().setFavorite(bench.id, favorite = true)
        exercises().setDefaults(bench.id, ExerciseDefaults(restSeconds = 200, weightStepKg = 1.25))
        val backup = export(store)

        // Everything changes after the backup.
        workouts().delete(done.id)
        workouts().create(Workout(WorkoutId("later"), startedAt = NOW + 5))
        routines().delete(push.id)
        programs().delete(program.id)
        exercises().deleteCustom(custom)
        exercises().setFavorite(bench.id, favorite = false)
        exercises().setDefaults(bench.id, ExerciseDefaults())
        settings.updateData { SettingsJson() }

        store.restore(store.read(backup.inputStream())!!)

        val defaults = ExerciseDefaults(restSeconds = 200, weightStepKg = 1.25)
        val restored = workouts().observeFinished(10).first().single()
        assertThat(restored.copy(exercises = restored.exercises.map { it.copy(exercise = bench) })).isEqualTo(done)
        assertThat(restored.exercises.single().exercise.defaults).isEqualTo(defaults)
        assertThat(workouts().observeActive().first()).isNull()
        assertThat(routines().get(push.id)!!.exercises.single().progression)
            .isEqualTo(ProgressionRule.Linear(incrementKg = 5.0))
        assertThat(programs().observeActive().first()?.lastDay).isEqualTo(ProgramDayId("d"))
        val customRow = db.database.exerciseDao().get(custom.value)!!
        assertThat(customRow.name).isEqualTo("My lift")
        assertThat(db.database.exerciseDao().musclesOf(custom.value).single().muscle).isEqualTo(Muscle.CHEST)
        assertThat(db.database.exerciseDao().get("bench")!!.isFavorite).isTrue()
        assertThat(settings.data.first().weightUnit).isEqualTo(WeightUnit.LB)
    }

    @Test
    fun aBackupCanBeRestoredIntoANewInstall() = runTest {
        val store = store(dataStore())
        fill()
        val backup = export(store)
        db.database.backupDao().run {
            clearWorkouts()
            clearPrograms()
            clearRoutines()
        }
        // The library of the new install knows the exercise under its new name.
        val dao = db.database.exerciseDao()
        dao.update(dao.get("bench")!!.copy(name = "Bench Press", libraryVersion = 2))

        store.restore(store.read(backup.inputStream())!!)

        assertThat(workouts().observeFinished(10).first().single().exercises.single().exercise.name)
            .isEqualTo("Bench Press")
        assertThat(dao.get("bench")!!.libraryVersion).isEqualTo(2)
    }

    @Test
    fun anExerciseTheLibraryNoLongerHasComesBackWithTheBackup() = runTest {
        val store = store(dataStore())
        fill()
        val backup = store.read(export(store).inputStream())!!
        val renamed = backup.copy(
            exercises = backup.exercises.map { it.copy(id = "old-lift", sourceRef = "test:old", name = "Old lift") },
            routineExercises = backup.routineExercises.map { it.copy(exerciseId = "old-lift") },
            workoutExercises = backup.workoutExercises.map { it.copy(exerciseId = "old-lift") },
        )

        store.restore(renamed)

        assertThat(workouts().observeFinished(10).first().single().exercises.single().exercise.name)
            .isEqualTo("Old lift")
    }

    @Test
    fun aBrokenBackupChangesNothing() = runTest {
        val store = store(dataStore())
        fill()
        val backup = store.read(export(store).inputStream())!!
        val broken = backup.copy(workoutSets = backup.workoutSets.map { it.copy(workoutExerciseId = "nowhere") })

        assertThat(runCatching { store.restore(broken) }.isFailure).isTrue()

        assertThat(workouts().observeFinished(10).first().single()).isEqualTo(done)
        assertThat(routines().get(push.id)).isEqualTo(push)
    }

    @Test
    fun theSummaryCountsWhatIsThere() = runTest {
        val store = store(dataStore())
        fill()
        workouts().create(Workout(WorkoutId("active"), startedAt = NOW + 5))

        val summary = store.read(export(store).inputStream())!!.summary()

        assertThat(summary.createdAt).isEqualTo(NOW)
        assertThat(summary.workouts).isEqualTo(1)
        assertThat(summary.routines).isEqualTo(1)
        assertThat(summary.programs).isEqualTo(1)
        assertThat(summary.customExercises).isEqualTo(0)
    }

    @Test
    fun otherFilesAndNewerBackupsAreNotRead() = runTest {
        val store = store(dataStore())
        val newer = store.read(export(store).inputStream())!!.copy(version = BackupFile.VERSION + 1)
        val output = ByteArrayOutputStream()
        BackupJson.write(newer, output)

        assertThat(store.read(output.toByteArray().inputStream())).isNull()
        assertThat(store.read("Date,Workout Name".byteInputStream())).isNull()
        assertThat(store.read("{}".byteInputStream())).isNull()
    }

    private suspend fun fill() {
        db.addExercise("bench")
        routines().create(push)
        programs().create(program)
        workouts().create(done)
    }

    private suspend fun export(store: BackupStore): ByteArray =
        ByteArrayOutputStream().also { store.write(it) }.toByteArray()

    private fun TestScope.dataStore(): DataStore<SettingsJson> = DataStoreFactory.create(
        serializer = SettingsSerializer,
        scope = CoroutineScope(backgroundScope.coroutineContext),
        produceFile = { folder.root.resolve("settings.json") },
    )

    private fun store(settings: DataStore<SettingsJson>) =
        BackupStore(db.database, db.database.backupDao(), settings, db.recordKeeper(clock), clock)

    private fun workouts() = RoomWorkoutRepository(db.database, db.database.workoutDao(), clock, db.recordKeeper(clock))

    private fun routines() =
        RoomRoutineRepository(db.database, db.database.routineDao(), db.database.historyDao(), clock)

    private fun programs() = RoomProgramRepository(db.database, db.database.programDao(), clock)

    private fun exercises() = RoomExerciseEditor(db.database, db.database.exerciseDao(), ids, clock)
}
