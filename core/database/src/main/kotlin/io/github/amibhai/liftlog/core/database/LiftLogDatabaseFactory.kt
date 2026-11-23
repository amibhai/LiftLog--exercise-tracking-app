package io.github.amibhai.liftlog.core.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

object LiftLogDatabaseFactory {
    /**
     * Opens (and on first launch copies from the prepopulated asset) the on-device database.
     * The bundled driver gives every Android version the same modern SQLite.
     */
    fun create(
        context: Context,
        driver: SQLiteDriver = BundledSQLiteDriver(),
        queryContext: CoroutineContext = Dispatchers.IO,
    ): LiftLogDatabase = Room.databaseBuilder(context, LiftLogDatabase::class.java, LiftLogDatabase.NAME)
        .createFromAsset(LiftLogDatabase.ASSET_PATH)
        .setDriver(driver)
        .setQueryCoroutineContext(queryContext)
        .addMigrations(*LiftLogMigrations.ALL.toTypedArray())
        .build()
}
