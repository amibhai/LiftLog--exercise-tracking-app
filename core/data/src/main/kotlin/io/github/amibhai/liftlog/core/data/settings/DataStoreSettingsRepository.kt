package io.github.amibhai.liftlog.core.data.settings

import androidx.datastore.core.DataStore
import io.github.amibhai.liftlog.core.domain.model.UserSettings
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<SettingsJson>) :
    SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { it.toDomain() }.distinctUntilChanged()

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.updateData { current -> transform(current.toDomain()).toJson() }
    }
}
