package io.github.amibhai.liftlog.core.ui.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import io.github.amibhai.liftlog.core.domain.time.PartOfDay
import io.github.amibhai.liftlog.core.ui.R

/** A workout's name, or the time of day for a workout without one ("Evening workout"). */
@Immutable
data class WorkoutTitle(val name: String?, val partOfDay: PartOfDay)

/** "Push", or "Evening workout" for a workout without a name. */
@Composable
fun workoutTitle(title: WorkoutTitle): String = title.name ?: stringResource(title.partOfDay.workoutTitleRes())

/** The name of a workout without one, for places without Compose such as notifications. */
@StringRes
fun PartOfDay.workoutTitleRes(): Int = when (this) {
    PartOfDay.MORNING -> R.string.ui_workout_morning
    PartOfDay.AFTERNOON -> R.string.ui_workout_afternoon
    PartOfDay.EVENING -> R.string.ui_workout_evening
    PartOfDay.NIGHT -> R.string.ui_workout_night
}
