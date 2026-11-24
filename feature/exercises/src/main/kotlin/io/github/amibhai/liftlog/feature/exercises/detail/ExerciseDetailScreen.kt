package io.github.amibhai.liftlog.feature.exercises.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.amibhai.liftlog.core.domain.model.ExerciseDetail
import io.github.amibhai.liftlog.core.domain.model.ExerciseId
import io.github.amibhai.liftlog.core.ui.component.ConfirmDialog
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.R
import io.github.amibhai.liftlog.core.ui.R as UiR

@Composable
internal fun ExerciseDetailRoute(
    onBack: () -> Unit,
    onEdit: (ExerciseId) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onBack, onEdit) {
        ExerciseDetailActions(
            onBack = onBack,
            onEdit = onEdit,
            onFavoriteChange = viewModel::setFavorite,
            onHiddenChange = viewModel::setHidden,
            onDelete = {
                viewModel.delete()
                onBack()
            },
            onNoteChange = viewModel::setNote,
            onDefaultsChange = viewModel::setDefaults,
        )
    }
    ExerciseDetailScreen(state = state, actions = actions)
}

@Composable
internal fun ExerciseDetailScreen(
    state: ExerciseDetailUiState,
    actions: ExerciseDetailActions,
    modifier: Modifier = Modifier,
) {
    val exercise = (state as? ExerciseDetailUiState.Loaded)?.exercise
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(LiftLogIcons.ArrowBack), stringResource(UiR.string.ui_back))
                    }
                },
                actions = { if (exercise != null) TopBarActions(exercise, actions) },
            )
        },
    ) { padding ->
        when (state) {
            ExerciseDetailUiState.Loading -> Unit

            ExerciseDetailUiState.NotFound -> EmptyState(
                icon = LiftLogIcons.Info,
                title = stringResource(R.string.exercises_detail_not_found),
                body = stringResource(R.string.exercises_detail_not_found_body),
                modifier = Modifier.padding(padding),
            )

            is ExerciseDetailUiState.Loaded -> DetailContent(
                state = state,
                actions = actions,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun RowScope.TopBarActions(exercise: ExerciseDetail, actions: ExerciseDetailActions) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }
    IconButton(onClick = { actions.onFavoriteChange(!exercise.isFavorite) }) {
        Icon(
            painterResource(if (exercise.isFavorite) LiftLogIcons.Favorite else LiftLogIcons.NotFavorite),
            stringResource(
                if (exercise.isFavorite) R.string.exercises_detail_unfavorite else R.string.exercises_detail_favorite,
            ),
        )
    }
    Box {
        IconButton(onClick = { isMenuOpen = true }) {
            Icon(painterResource(LiftLogIcons.More), stringResource(UiR.string.ui_more_options))
        }
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            MenuItem(stringResource(UiR.string.ui_edit), LiftLogIcons.Edit) {
                isMenuOpen = false
                actions.onEdit(exercise.id)
            }
            MenuItem(
                label = stringResource(
                    if (exercise.isArchived) R.string.exercises_detail_show else R.string.exercises_detail_hide,
                ),
                icon = if (exercise.isArchived) LiftLogIcons.Show else LiftLogIcons.Hide,
            ) {
                isMenuOpen = false
                actions.onHiddenChange(!exercise.isArchived)
            }
            if (exercise.isCustom) {
                MenuItem(stringResource(UiR.string.ui_delete), LiftLogIcons.Delete) {
                    isMenuOpen = false
                    isConfirmingDelete = true
                }
            }
        }
    }
    if (isConfirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.exercises_detail_delete_title),
            text = stringResource(R.string.exercises_detail_delete_text),
            confirmLabel = stringResource(UiR.string.ui_delete),
            onConfirm = actions.onDelete,
            onDismiss = { isConfirmingDelete = false },
            isDestructive = true,
        )
    }
}

@Composable
private fun MenuItem(label: String, icon: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
    )
}

@Composable
private fun DetailContent(
    state: ExerciseDetailUiState.Loaded,
    actions: ExerciseDetailActions,
    modifier: Modifier = Modifier,
) {
    val exercise = state.exercise
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = LiftLogDimens.spacingM),
        verticalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingL),
    ) {
        if (exercise.images.isNotEmpty()) ExerciseImages(exercise.images)
        val gutter = Modifier.padding(horizontal = LiftLogDimens.screenGutter)
        Tags(exercise, gutter)
        Muscles(exercise, gutter)
        PersonalSettings(exercise, state.settings, actions, gutter)
        History(state.sessions, gutter)
        Instructions(exercise, gutter)
        exercise.attribution?.let { AttributionCard(it, gutter) }
    }
}
