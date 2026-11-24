package io.github.amibhai.liftlog.core.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration harness. Each new schema version adds a test here that creates the previous version
 * from its exported schema, runs the migration and lets Room validate the result.
 */
@RunWith(AndroidJUnit4::class)
class LiftLogMigrationsTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation,
        instrumentation.targetContext.getDatabasePath("migration-test.db"),
        AndroidSQLiteDriver(),
        LiftLogDatabase::class,
    )

    @Test
    fun exportedSchemaOfTheCurrentVersionMatchesTheEntities() = runTest {
        helper.createDatabase(LiftLogDatabase.VERSION).close()

        helper.runMigrationsAndValidate(LiftLogDatabase.VERSION, LiftLogMigrations.ALL).close()
    }

    @Test
    fun everyVersionAfterTheFirstHasAMigration() {
        val covered = LiftLogMigrations.ALL.map { it.startVersion to it.endVersion }

        assertThat(covered).containsExactlyElementsIn((1 until LiftLogDatabase.VERSION).map { it to it + 1 })
    }

    @Test
    fun version2KeepsLoggedSetsAndAddsEmptyDurationAndDistanceTargets() = runTest {
        helper.createDatabase(1).use { v1 ->
            v1.execSQL(
                "INSERT INTO exercise (id, name, aliases, search_terms, type, equipment, category, instructions, " +
                    "is_unilateral, is_favorite, is_archived, is_custom, created_at, updated_at) " +
                    "VALUES ('e', 'Bench', '', '', 'WEIGHT_REPS', 'BARBELL', 'STRENGTH', '', 0, 0, 0, 0, 1, 1)",
            )
            v1.execSQL(
                "INSERT INTO workout (id, started_at, is_in_progress, created_at, updated_at) VALUES ('w', 1, 0, 1, 1)",
            )
            v1.execSQL(
                "INSERT INTO workout_exercise (id, workout_id, exercise_id, position, created_at, updated_at) " +
                    "VALUES ('we', 'w', 'e', 0, 1, 1)",
            )
            v1.execSQL(
                "INSERT INTO workout_set (id, workout_exercise_id, position, type, weight_kg, reps, is_completed, " +
                    "created_at, updated_at) VALUES ('s', 'we', 0, 'WORKING', 80.0, 8, 1, 1, 1)",
            )
        }

        helper.runMigrationsAndValidate(2, LiftLogMigrations.ALL).use { v2 ->
            v2.prepare("SELECT weight_kg, reps, target_duration_s, target_distance_m FROM workout_set").use { row ->
                assertThat(row.step()).isTrue()
                assertThat(row.getDouble(0)).isEqualTo(80.0)
                assertThat(row.getLong(1)).isEqualTo(8)
                assertThat(row.isNull(2)).isTrue()
                assertThat(row.isNull(3)).isTrue()
            }
        }
    }
}
