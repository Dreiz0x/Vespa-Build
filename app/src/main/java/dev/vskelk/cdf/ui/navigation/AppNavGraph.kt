package dev.vskelk.cdf.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.vskelk.cdf.ui.splash.SplashScreen
import dev.vskelk.cdf.ui.main.MainScreen
import dev.vskelk.cdf.ui.simulator.SimulatorScreen
import dev.vskelk.cdf.ui.diagnosis.DiagnosisScreen
import dev.vskelk.cdf.ui.interview.InterviewScreen
import dev.vskelk.cdf.ui.investigator.InvestigatorScreen
import dev.vskelk.cdf.ui.quarantine.QuarantineScreen
import dev.vskelk.cdf.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController, startDestination: String = Routes.SPLASH) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SPLASH) {
            SplashScreen(onNavigateToMain = { navController.navigate(Routes.MAIN) { popUpTo(Routes.SPLASH) { inclusive = true } } })
        }
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToSimulator = { navController.navigate(Routes.SIMULATOR) },
                onNavigateToDiagnosis = { navController.navigate(Routes.DIAGNOSIS) },
                onNavigateToInterview = { navController.navigate(Routes.INTERVIEW) },
                onNavigateToInvestigator = { navController.navigate(Routes.INVESTIGATOR) },
                onNavigateToQuarantine = { navController.navigate(Routes.QUARANTINE) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) } 
            )
        }
        composable(Routes.SIMULATOR) { SimulatorScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.DIAGNOSIS) { DiagnosisScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToInvestigator = { navController.navigate(Routes.INVESTIGATOR) }) }
        composable(Routes.INTERVIEW) { InterviewScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.INVESTIGATOR) { InvestigatorScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.QUARANTINE) { QuarantineScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}
