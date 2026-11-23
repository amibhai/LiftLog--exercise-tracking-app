package io.github.amibhai.liftlog.core.database

import androidx.room3.migration.Migration

/**
 * Every schema migration, oldest first. Never use destructive fallback: once a build is on the phone,
 * each version bump needs a migration here and a test in LiftLogMigrationsTest.
 */
object LiftLogMigrations {
    val ALL: List<Migration> = emptyList()
}
