package io.github.amibhai.liftlog.core.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
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
}
