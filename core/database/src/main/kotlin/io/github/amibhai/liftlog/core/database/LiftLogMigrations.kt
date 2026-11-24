package io.github.amibhai.liftlog.core.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Every schema migration, oldest first. Never use destructive fallback: once a build is on the phone,
 * each version bump needs a migration here and a test in LiftLogMigrationsTest.
 */
object LiftLogMigrations {

    /** Ghost values for timed and distance sets. */
    private val from1To2 = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `workout_set` ADD COLUMN `target_duration_s` INTEGER")
            connection.execSQL("ALTER TABLE `workout_set` ADD COLUMN `target_distance_m` REAL")
        }
    }

    val ALL: List<Migration> = listOf(from1To2)
}
