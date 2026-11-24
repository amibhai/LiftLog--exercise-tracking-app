package io.github.amibhai.liftlog.core.data.backup

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import io.github.amibhai.liftlog.core.domain.backup.BackupFrequency
import io.github.amibhai.liftlog.core.domain.backup.BackupSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private val DEFAULTS = BackupSettings()

/**
 * On-disk shape of [BackupSettings] (files/datastore/backup.json). It is in no backup: the access to the
 * folder belongs to this phone.
 */
@Serializable
internal data class BackupSettingsJson(
    val folderUri: String? = null,
    val folderName: String? = null,
    val frequency: BackupFrequency = DEFAULTS.frequency,
    val keep: Int = DEFAULTS.keep,
    val lastBackupAt: Long? = null,
    val lastFailed: Boolean = false,
)

internal fun BackupSettingsJson.toDomain() =
    BackupSettings(folderUri, folderName, frequency, keep, lastBackupAt, lastFailed)

internal fun BackupSettings.toJson() =
    BackupSettingsJson(folderUri, folderName, frequency, keep, lastBackupAt, lastFailed)

internal object BackupSettingsSerializer : Serializer<BackupSettingsJson> {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override val defaultValue = BackupSettingsJson()

    override suspend fun readFrom(input: InputStream): BackupSettingsJson = try {
        json.decodeFromString<BackupSettingsJson>(input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("backup.json is not valid", error)
    } catch (error: IllegalArgumentException) {
        throw CorruptionException("backup.json is not valid", error)
    }

    override suspend fun writeTo(t: BackupSettingsJson, output: OutputStream) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
