package io.github.amibhai.liftlog.core.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** JSON DataStore serializer (one of the documented DataStore flavours; see docs/DECISIONS.md). */
internal object SettingsSerializer : Serializer<SettingsJson> {

    private val json = Json {
        ignoreUnknownKeys = true // keys written by a newer build
        coerceInputValues = true // unknown enum values (and nulls) become the default
        encodeDefaults = true // the file states every value explicitly
        prettyPrint = true
    }

    override val defaultValue: SettingsJson = SettingsJson()

    override suspend fun readFrom(input: InputStream): SettingsJson = try {
        json.decodeFromString(SettingsJson.serializer(), input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("settings.json is not valid", error)
    } catch (error: IllegalArgumentException) {
        throw CorruptionException("settings.json is not valid", error)
    }

    override suspend fun writeTo(t: SettingsJson, output: OutputStream) {
        output.write(json.encodeToString(SettingsJson.serializer(), t).encodeToByteArray())
    }
}
