package com.optativesolutions.eathereye

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    state: AirQualityUiState,
    onVocSelected: (Int) -> Unit,
    onNavigateToReport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Lecturas recientes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // --- TARJETA DE PRESIÓN (Antes CO) ---
                    StatCard(
                        title = "Presión",
                        value = "${state.pressure.toInt()} hPa", // Nueva unidad
                        modifier = Modifier.weight(1f)
                    )
                    // --- TARJETA DE TEMPERATURA ---
                    StatCard(
                        title = "Temperatura",
                        value = "${state.temperature}°C",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- SECCIÓN DE VOC CON SPINNER ---
        item {
            VocChartSection(
                vocData = state.selectedVoc,
                availableVocs = state.availableVocs.map { it.name },
                selectedIndex = state.selectedVocIndex,
                onVocSelected = onVocSelected
            )
        }

        item {
            Button(
                onClick = onNavigateToReport,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF))
            ) {
                Text("Reporte histórico")
            }
        }
    }
}