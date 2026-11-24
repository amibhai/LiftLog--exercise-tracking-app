package io.github.amibhai.liftlog.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.amibhai.liftlog.core.ui.theme.LiftLogDimens

/**
 * The content card used across the app. Its colors are explicit because the default container can be
 * the same color as `surfaceVariant` (dynamic color), and Material would then dim the titles.
 */
@Composable
fun LiftLogCard(
    modifier: Modifier = Modifier,
    spacing: Dp = LiftLogDimens.spacingS,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(LiftLogDimens.spacingL),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}
