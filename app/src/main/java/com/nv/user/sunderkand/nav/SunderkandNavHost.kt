package com.nv.user.sunderkand.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nv.user.sunderkand.ui.about.AboutScreen
import com.nv.user.sunderkand.ui.home.HomeScreen
import com.nv.user.sunderkand.ui.reader.ReaderScreen
import com.nv.user.sunderkand.ui.sankalp.SankalpScreen

/**
 * Type-safe-ish route constants. Using string-based Compose Navigation
 * (rather than the newer typed Kotlin Serialization destinations) keeps
 * the surface small and well-supported on Compose 1.7 / nav 2.8.
 */
object Routes {
    const val HOME = "home"
    const val ABOUT = "about"
    const val SANKALP = "sankalp"
    const val READER = "reader/{chalisaId}"
    fun reader(chalisaId: String) = "reader/$chalisaId"
}

@Composable
fun SunderkandNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenChalisa = { id -> navController.navigate(Routes.reader(id)) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenSankalp = { navController.navigate(Routes.SANKALP) },
            )
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("chalisaId") { type = NavType.StringType }),
        ) { entry ->
            val chalisaId = entry.arguments?.getString("chalisaId").orEmpty()
            ReaderScreen(
                chalisaId = chalisaId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SANKALP) {
            SankalpScreen(onBack = { navController.popBackStack() })
        }
    }
}
