package com.optativesolutions.eathereye

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// TARJETA PARA LAS ESTADÍSTICAS (CO, VOC, Temp, Humedad)
@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun VocChartSection(
    vocData: VocData,
    availableVocs: List<String>,
    selectedIndex: Int,
    onVocSelected: (Int) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val chartEntryModelProducer = remember(vocData.history) {
        ChartEntryModelProducer(vocData.history.map { entryOf(it.first, it.second) })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- TÍTULO Y SPINNER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Nivel de ${vocData.name}", // Título dinámico
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            // --- Lógica del Dropdown ---
            Box {
                Row(
                    modifier = Modifier.clickable { isDropdownExpanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(vocData.name, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar VOC", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    availableVocs.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onVocSelected(index)
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${vocData.level} ppm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            // ... resto de la info (cambio, últimas 24h, etc.)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // El componente del gráfico
        Chart(
            chart = lineChart(),
            chartModelProducer = chartEntryModelProducer,
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ -> "${value.toInt()}h" }
            ),
            modifier = Modifier.height(150.dp)
        )
    }
}