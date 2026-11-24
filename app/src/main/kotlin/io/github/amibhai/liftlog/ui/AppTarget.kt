package io.github.amibhai.liftlog.ui

import io.github.amibhai.liftlog.core.domain.model.WorkoutId

/** A screen to open that was asked for from elsewhere: a shortcut, the widget or the first-run pages. */
sealed interface AppTarget {
    data class ActiveWorkout(val id: WorkoutId) : AppTarget

    data object Programs : AppTarget
}
