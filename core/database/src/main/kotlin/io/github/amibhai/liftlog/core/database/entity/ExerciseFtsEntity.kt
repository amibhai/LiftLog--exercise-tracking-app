package io.github.amibhai.liftlog.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.FtsOptions

/**
 * External-content FTS4 index over [ExerciseEntity] (SPEC §4), kept in sync by Room's triggers and
 * joined on `exercise.rowid`. A `VACUUM` may renumber rowids, so always rebuild this index after one.
 */
@Fts4(
    contentEntity = ExerciseEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=2"],
)
@Entity(tableName = "exercise_fts")
data class ExerciseFtsEntity(
    val name: String,
    val aliases: String,
    @ColumnInfo(name = "search_terms") val searchTerms: String,
)
