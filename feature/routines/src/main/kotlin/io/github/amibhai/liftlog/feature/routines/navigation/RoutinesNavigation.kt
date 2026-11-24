package io.github.amibhai.liftlog.feature.routines.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.amibhai.liftlog.core.domain.model.RoutineId
import io.github.amibhai.liftlog.feature.routines.editor.RoutineEditorNavigation
import io.github.amibhai.liftlog.feature.routines.editor.RoutineEditorRoute
import io.github.amibhai.liftlog.feature.routines.programs.ProgramsNavigation
import io.github.amibhai.liftlog.feature.routines.programs.ProgramsRoute
import io.github.amibhai.liftlog.feature.routines.programs.detail.ProgramRoute
import io.github.amibhai.liftlog.feature.routines.programs.template.TemplateRoute
import kotlinx.serialization.Serializable

@Serializable
data class RoutineEditorDestination(val routineId: String)

@Serializable
data object ProgramsDestination

@Serializable
data class ProgramDestination(val programId: String)

/** [key] is the name of a built-in program. */
@Serializable
data class TemplateDestination(val key: String)

/** The routine editor and the programs. */
fun NavGraphBuilder.routinesGraph(navController: NavController, editorNavigation: RoutineEditorNavigation) {
    val back: () -> Unit = { navController.popBackStack() }
    composable<RoutineEditorDestination> { RoutineEditorRoute(navigation = editorNavigation) }
    composable<ProgramsDestination> {
        ProgramsRoute(
            ProgramsNavigation(
                onBack = back,
                onOpenProgram = { navController.navigate(ProgramDestination(it.value)) },
                onOpenTemplate = { navController.navigate(TemplateDestination(it.name)) },
            ),
        )
    }
    composable<TemplateDestination> {
        TemplateRoute(
            onBack = back,
            onCreate = { program ->
                navController.navigate(ProgramDestination(program.value)) {
                    popUpTo<TemplateDestination> { inclusive = true }
                }
            },
        )
    }
    composable<ProgramDestination> {
        ProgramRoute(onBack = back, onEditRoutine = { navController.navigateToRoutineEditor(it) })
    }
}

fun NavController.navigateToRoutineEditor(id: RoutineId) {
    navigate(RoutineEditorDestination(id.value)) { launchSingleTop = true }
}

fun NavController.navigateToPrograms() {
    navigate(ProgramsDestination) { launchSingleTop = true }
}
