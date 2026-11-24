package io.github.amibhai.liftlog.core.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.amibhai.liftlog.core.database.LiftLogDatabase
import io.github.amibhai.liftlog.core.database.entity.ExerciseEntity
import io.github.amibhai.liftlog.core.domain.id.IdGenerator
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.ExerciseCategory
import io.github.amibhai.liftlog.core.domain.model.ExerciseType
import org.junit.rules.ExternalResource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal const val NOW = 1_700_000_000_000L

/** An empty in-memory database per test. */
class TestDatabase : ExternalResource() {
    lateinit var database: LiftLogDatabase
        private set

    override fun before() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            LiftLogDatabase::class.java,
        ).setDriver(AndroidSQLiteDriver()).build()
    }

    override fun after() {
        database.close()
    }

    suspend fun addExercise(id: String, name: String = id, type: ExerciseType = ExerciseType.WEIGHT_REPS) {
        database.exerciseDao().insert(exerciseEntity(id = id, name = name, type = type, sourceRef = "test:$id"))
    }
}

/** A clock that only moves when told to. */
class TestClock(var now: Long = NOW) : Clock() {
    override fun instant(): Instant = Instant.ofEpochMilli(now)

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}

class CountingIds : IdGenerator {
    private var next = 0

    override fun newId(): String = "id${next++}"
}

internal fun exerciseEntity(
    id: String,
    name: String,
    aliases: String = "",
    instructions: String = "",
    sourceRef: String?,
    isCustom: Boolean = false,
    type: ExerciseType = ExerciseType.WEIGHT_REPS,
) = ExerciseEntity(
    id = id,
    name = name,
    aliases = aliases,
    searchTerms = "",
    type = type,
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
