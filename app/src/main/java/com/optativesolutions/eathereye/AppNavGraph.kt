package com.optativesolutions.eathereye

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

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
                onNavigateToReport = { sensorKey ->
                    // La llamada a navigate ya no tiene el "route:"
                    navController.navigate("report/$sensorKey")
                }
            )
        }
        composable(
            route = "report/{sensorKey}",
            arguments = listOf(navArgument("sensorKey") { type = NavType.StringType })
        ) { backStackEntry -> // <- El contenido empieza aquí

            // Obtenemos la clave del sensor desde los argumentos de la ruta
            val sensorKey = backStackEntry.arguments?.getString("sensorKey")

            // Si la clave existe, inicializamos el ViewModel con ella
            if (sensorKey != null) {
                val reportViewModel: HistoricalReportViewModel = viewModel(factory = factory)
                reportViewModel.setSensorKey(sensorKey)

                HistoricalReportScreen(
                    reportViewModel = reportViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}