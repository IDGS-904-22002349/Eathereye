package com.optativesolutions.eathereye

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel


// --- Estructura Principal de la Pantalla ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModelFactory: ViewModelFactory, onNavigateToReport: () -> Unit) {
    val context = LocalContext.current

    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val alertsViewModel: AlertsViewModel = viewModel(factory = viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    val uiState by homeViewModel.uiState.collectAsState()
    val screenIcons = listOf(Icons.Filled.Home, Icons.Filled.Air, Icons.Filled.Notifications, Icons.Filled.Settings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentScreenTitle) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Abrir menú lateral */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menú")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                uiState.screenTitles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = uiState.selectedScreenIndex == index,
                        onClick = { homeViewModel.onScreenSelected(index) },
                        icon = { Icon(screenIcons[index], contentDescription = title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // --- AQUÍ SE CORRIGEN LAS LLAMADAS ---
            when (uiState.selectedScreenIndex) {
                0 -> DashboardScreen(
                    state = uiState,
                    onVocSelected = homeViewModel::onVocSelected,
                    onNavigateToReport = onNavigateToReport
                )
                1 -> SensorsScreen(homeViewModel = homeViewModel) // Asumiendo que esta pantalla no ha cambiado
                2 -> AlertsScreen(alertsViewModel = alertsViewModel)
                3 -> SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    homeViewModel = homeViewModel
                )
            }
        }
    }
}