package io.github.amibhai.liftlog.feature.routines.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.feature.routines.editor.RoutineEditorNavigation
import io.github.amibhai.liftlog.feature.routines.editor.RoutineEditorRoute
import kotlinx.serialization.Serializable

@Serializable
data class RoutineEditorDestination(val routineId: String)

fun NavGraphBuilder.routineEditorScreen(navigation: RoutineEditorNavigation) {
    composable<RoutineEditorDestination> { RoutineEditorRoute(navigation = navigation) }
}

fun NavController.navigateToRoutineEditor(id: RoutineId) {
    navigate(RoutineEditorDestination(id.value)) { launchSingleTop = true }
}
