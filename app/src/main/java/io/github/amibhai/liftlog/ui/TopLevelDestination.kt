package io.github.amibhai.liftlog.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.amibhai.liftlog.R
import io.github.amibhai.liftlog.core.ui.icon.LiftLogIcons
import io.github.amibhai.liftlog.feature.exercises.navigation.ExercisesDestination
import io.github.amibhai.liftlog.feature.history.navigation.HistoryDestination
import io.github.amibhai.liftlog.feature.home.navigation.HomeDestination
import io.github.amibhai.liftlog.feature.progress.navigation.ProgressDestination

/** The bottom-bar tabs; each keeps its own back stack. Settings opens from Home's top bar. */
enum class TopLevelDestination(val route: Any, @DrawableRes val icon: Int, @StringRes val label: Int) {
    HOME(HomeDestination, LiftLogIcons.Home, R.string.nav_home),
    HISTORY(HistoryDestination, LiftLogIcons.History, R.string.nav_history),
    PROGRESS(ProgressDestination, LiftLogIcons.Progress, R.string.nav_progress),
    EXERCISES(ExercisesDestination, LiftLogIcons.Exercises, R.string.nav_exercises),
}
