package com.optativesolutions.eathereye

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, homeViewModel: HomeViewModel) {
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    val selectedVoc = homeUiState.selectedVoc
    val vocThreshold = settingsUiState.vocThresholds[selectedVoc.key] ?: 0f

    val rangoDinamico = when (selectedVoc.key) {
        "benzene" -> 0f..1000f
        "toluene" -> 0f..400f
        else -> 0f..50f // Rango por defecto para otros VOCs
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECCIÓN UMBRALES DE ALERTA EN UNA TARJETA ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Umbrales de Alerta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ThresholdSlider(
                        title = "Umbral de ${selectedVoc.name}",
                        value = vocThreshold,
                        onValueChange = { newValue -> settingsViewModel.onVocThresholdChange(selectedVoc.key, newValue) },
                        valueRange = rangoDinamico,
                        unit = "ppm"
                    )
                }
            }
        }

        // --- SECCIÓN NOTIFICACIONES EN OTRA TARJETA ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notificaciones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch para activar/desactivar todas las alertas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alertas de Contaminación", fontSize = 16.sp)
                            Text(
                                text = "Activa o desactiva todas las notificaciones.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                lineHeight = 20.sp
                            )
                        }
                        Switch(
                            checked = settingsUiState.areNotificationsEnabled,
                            onCheckedChange = { settingsViewModel.onNotificationsToggle(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch para el sonido de alarma
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sonido de Alarma", fontSize = 16.sp)
                            Text(
                                text = "Usa un sonido de alarma persistente para alertas críticas.",
                                fontSize = 14.sp,
                                color = if (settingsUiState.areNotificationsEnabled) Color.Gray else Color.LightGray,
                                lineHeight = 20.sp
                            )
                        }
                        Switch(
                            checked = settingsUiState.isAlarmSoundOn,
                            onCheckedChange = { settingsViewModel.onAlarmSoundToggle(it) },
                            // Se desactiva si las notificaciones principales están apagadas
                            enabled = settingsUiState.areNotificationsEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThresholdSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 16.sp)
            Text(
                // Muestra un decimal para un feedback más suave
                text = "${"%.1f".format(value)} $unit",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start - 1).toInt() * 10 // Aumenta los pasos para decimales
        )
    }
}