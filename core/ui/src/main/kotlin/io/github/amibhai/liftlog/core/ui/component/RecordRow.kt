package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.github.amibhai.liftlog.core.domain.model.RecordEntry
import io.github.amibhai.liftlog.core.domain.units.WeightUnit
import io.github.amibhai.liftlog.core.ui.R
import io.github.amibhai.liftlog.core.ui.format.dateText
import io.github.amibhai.liftlog.core.ui.format.labelRes
import io.github.amibhai.liftlog.core.ui.format.recordValueText
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/** A personal record: the exercise, the kind of record and, with [withDate], its day, and the value. */
@Composable
fun RecordRow(
    entry: RecordEntry,
    unit: WeightUnit,
    modifier: Modifier = Modifier,
    withDate: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val kind = stringResource(entry.record.type.labelRes())
    val clickable = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LiftLogDimens.minTouchTarget)
            .then(clickable)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = LiftLogDimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(LiftLogDimens.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.exerciseName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (withDate) {
                    stringResource(R.string.ui_record_dated, kind, dateText(entry.record.achievedAt))
                } else {
                    kind
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = recordValueText(
                type = entry.record.type,
                value = entry.record.value,
                weightKg = entry.record.weightKg,
                exerciseType = entry.exerciseType,
                unit = unit,
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
        )
    }
}
