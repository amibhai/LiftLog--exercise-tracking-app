package io.github.amibhai.liftlog.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Dark (the default), light, and dark at 200% font scale (SPEC §13). */
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true,
)
@Preview(
    name = "Light",
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL,
    showBackground = true,
)
@Preview(
    name = "Dark, 200% font",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    fontScale = 2f,
    showBackground = true,
)
annotation class PreviewLiftLog
