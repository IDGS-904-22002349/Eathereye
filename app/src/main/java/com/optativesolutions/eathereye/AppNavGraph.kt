package com.optativesolutions.eathereye

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val factory = ViewModelFactory(LocalContext.current.applicationContext as Application)

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                viewModelFactory = factory,
                onNavigateToReport = { navController.navigate("report") }
            )
        }
        composable("report") {
            val reportViewModel: HistoricalReportViewModel = viewModel()
            HistoricalReportScreen(
                reportViewModel = reportViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}