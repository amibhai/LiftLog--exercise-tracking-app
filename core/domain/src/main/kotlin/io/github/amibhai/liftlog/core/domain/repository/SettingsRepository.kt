package io.github.amibhai.liftlog.core.domain.repository

import io.github.amibhai.liftlog.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    /** Atomically replaces the settings with `transform(current)`. */
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
