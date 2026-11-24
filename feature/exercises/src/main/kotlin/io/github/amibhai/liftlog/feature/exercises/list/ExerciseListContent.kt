package io.github.amibhai.liftlog.feature.exercises.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.amibhai.liftlog.core.domain.model.Equipment
import io.github.amibhai.liftlog.core.domain.model.Muscle
import io.github.amibhai.liftlog.core.ui.component.EmptyState
import io.github.amibhai.liftlog.core.ui.component.ExerciseThumbnail
import io.github.amibhai.liftlog.core.ui.component.HighlightedText
import io.github.amibhai.liftlog.core.ui.format.relativeDays
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.core.ui.label.labelRes
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens
import io.github.amibhai.liftlog.feature.exercises.R

/** Equipment menu and muscle chips; choosing one browses that part of the library. */
@Composable
internal fun FilterRow(
    filter: ExerciseFilter,
    onMuscleChange: (Muscle?) -> Unit,
    onEquipmentChange: (Equipment?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = LiftLogDimens.screenGutter),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingS),
    ) {
        item(key = "equipment") { EquipmentChip(filter.equipment, onEquipmentChange) }
        // The active muscle comes first, so it is visible and easy to switch off.
        items(Muscle.entries.sortedBy { it != filter.muscle }, key = { it.name }) { muscle ->
            val isSelected = muscle == filter.muscle
            FilterChip(
                selected = isSelected,
                onClick = { onMuscleChange(if (isSelected) null else muscle) },
                label = { Text(stringResource(muscle.labelRes())) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun EquipmentChip(selected: Equipment?, onSelect: (Equipment?) -> Unit, modifier: Modifier = Modifier) {
    var isOpen by remember { mutableStateOf(false) }
    val choose: (Equipment?) -> Unit = {
        isOpen = false
        onSelect(it)
    }
    Box(modifier) {
        FilterChip(
            selected = selected != null,
            onClick = { isOpen = true },
            label = { Text(stringResource(selected?.labelRes() ?: R.string.exercises_filter_equipment)) },
            trailingIcon = { Icon(painterResource(LiftLogIcons.ExpandMore), contentDescription = null) },
        )
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exercises_filter_any_equipment)) },
                onClick = { choose(null) },
            )
            for (equipment in Equipment.entries) {
                DropdownMenuItem(
                    text = { Text(stringResource(equipment.labelRes())) },
                    onClick = { choose(equipment) },
                )
            }
        }
    }
}

/**
 * The rows of [state]: suggestions, or the results of a search or filter. [typed] is the text field, which
 * runs ahead of the results; [row] draws one exercise.
 */
@Composable
internal fun ExerciseList(
    state: ExerciseListUiState,
    typed: String,
    onCreate: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    row: @Composable (ExerciseRowUiModel) -> Unit,
) {
    val isSearch = state.filter.query.isNotBlank()
    // Each search starts at its best match. With one shared state the list would stay anchored to the
    // previous first row and hide better matches that now sort above it.
    val listState = key(state.filter) { rememberLazyListState() }
    LazyColumn(modifier = modifier.fillMaxSize(), state = listState, contentPadding = contentPadding) {
        for (section in state.sections) {
            if (section.kind != ExerciseSectionKind.RESULTS) {
                item(key = section.kind, contentType = "header") { SectionHeader(section.kind) }
            }
            items(section.rows, key = { "${section.kind}-${it.id.value}" }, contentType = { "exercise" }) { row(it) }
        }
        val isSettled = state.filter.query == typed
        if (state.hasNoResults && isSettled) {
            item(key = "empty") {
                EmptyState(
                    icon = LiftLogIcons.Search,
                    title = if (isSearch) {
                        stringResource(R.string.exercises_no_matches_title, typed.trim())
                    } else {
                        stringResource(R.string.exercises_no_filter_matches)
                    },
                    body = stringResource(R.string.exercises_no_matches_body),
                )
            }
        }
        if (state.canCreate && isSettled) {
            item(key = "create") { CreateRow(typed.trim(), onClick = { onCreate(typed.trim()) }) }
        }
    }
}

@Composable
private fun SectionHeader(kind: ExerciseSectionKind, modifier: Modifier = Modifier) {
    val title = when (kind) {
        ExerciseSectionKind.FREQUENT -> R.string.exercises_section_frequent
        ExerciseSectionKind.GOES_WITH -> R.string.exercises_section_goes_with
        ExerciseSectionKind.RECENT -> R.string.exercises_section_recent
        ExerciseSectionKind.FAVORITES -> R.string.exercises_section_favorites
        ExerciseSectionKind.STARTERS -> R.string.exercises_section_starters
        ExerciseSectionKind.RESULTS -> R.string.exercises_section_results
    }
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(horizontal = LiftLogDimens.screenGutter)
            .padding(top = LiftLogDimens.spacingL, bottom = LiftLogDimens.spacingXs)
            .semantics { heading() },
    )
}

@Composable
private fun CreateRow(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        leadingContent = { Icon(painterResource(LiftLogIcons.Add), contentDescription = null) },
    ) {
        Text(stringResource(R.string.exercises_create, name))
    }
}

/** Equipment, muscle and what was done last time. */
@Composable
internal fun ExerciseRowDetails(row: ExerciseRowUiModel, modifier: Modifier = Modifier) {
    val equipment = stringResource(row.equipment.labelRes())
    val muscle = row.muscle?.let { stringResource(it.labelRes()) }
    val details = muscle?.let { stringResource(R.string.exercises_row_details, equipment, it) } ?: equipment
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Text(text = details, color = muted)
        when {
            row.isAdded -> Text(stringResource(R.string.exercises_row_added), color = MaterialTheme.colorScheme.primary)

            row.lastSet != null -> Text(
                text = stringResource(R.string.exercises_row_last, row.lastSet, relativeDays(row.lastDaysAgo)),
                color = muted,
            )
        }
    }
}

/** A row that opens the exercise. */
@Composable
internal fun BrowseRow(row: ExerciseRowUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        leadingContent = { ExerciseThumbnail(row.thumbnailUrl) },
        supportingContent = { ExerciseRowDetails(row) },
        trailingContent = { Icon(painterResource(LiftLogIcons.ChevronRight), contentDescription = null) },
    ) {
        HighlightedText(row.name, row.highlights)
    }
}
