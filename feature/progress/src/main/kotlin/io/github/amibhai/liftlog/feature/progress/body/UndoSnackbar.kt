package io.github.amibhai.liftlog.feature.progress.body

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.feature.progress.R
import io.github.amibhai.liftlog.core.ui.R as UiR

/** Offers to bring back what was just deleted; [deleted] identifies it. */
@Composable
internal fun UndoSnackbar(deleted: Any?, host: SnackbarHostState, onUndo: () -> Unit, onDone: () -> Unit) {
    val undo by rememberUpdatedState(onUndo)
    val done by rememberUpdatedState(onDone)
    val text = stringResource(R.string.progress_deleted)
    val action = stringResource(UiR.string.ui_undo)
    LaunchedEffect(deleted) {
        if (deleted != null) {
            val result = host.showSnackbar(text, actionLabel = action)
            if (result == SnackbarResult.ActionPerformed) undo() else done()
        }
    }
}
