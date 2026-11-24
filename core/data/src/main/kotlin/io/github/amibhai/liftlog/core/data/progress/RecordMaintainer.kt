package io.github.amibhai.liftlog.core.data.progress

import io.github.amibhai.liftlog.core.data.di.ApplicationScope
import io.github.amibhai.liftlog.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Works the records out again when that is needed as a whole; started once, with the app. */
@Singleton
class RecordMaintainer @Inject internal constructor(
    private val keeper: RecordKeeper,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        // An install from before records were kept, or a restored backup.
        scope.launch { keeper.rebuildIfEmpty() }
        // Estimated one-rep maxes change with the formula.
        scope.launch {
            settings.settings.map { it.e1rmFormula }.distinctUntilChanged().drop(1).collect { keeper.rebuildAll() }
        }
    }
}
