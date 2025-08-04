package com.optativesolutions.eathereye

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.TimeZone

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

// --- CAMBIO: Lógica corregida para que siempre devuelva un SimpleDateFormat ---
@Composable
fun rememberAdaptiveAxisFormatter(history: List<Pair<Long, Float>>): AxisValueFormatter<AxisPosition.Horizontal.Bottom> {
    val dateFormat = remember(history) {
        // --- CAMBIO: Se define la zona horaria de CDMX ---
        val cdmxTimeZone = TimeZone.getTimeZone("America/Mexico_City")

        val timestamps = history.map { it.first }

        if (timestamps.isEmpty()) {
            // Se crea el formato y se le aplica la zona horaria
            SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = cdmxTimeZone
            }
        } else {
            val minTimestamp = timestamps.minOrNull() ?: 0L
            val maxTimestamp = timestamps.maxOrNull() ?: 0L
            val durationMillis = maxTimestamp - minTimestamp

            when {
                durationMillis > TimeUnit.DAYS.toMillis(1) ->
                    // Se crea el formato y se le aplica la zona horaria
                    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).apply {
                        timeZone = cdmxTimeZone
                    }
                else ->
                    // Se crea el formato y se le aplica la zona horaria
                    SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                        timeZone = cdmxTimeZone
                    }
            }
        }
    }

    return AxisValueFormatter { value, _ ->
        dateFormat.format(Date(value.toLong()))
    }
}


@Composable
fun VocChartSection(
    vocData: VocData,
    availableVocs: List<String>,
    selectedIndex: Int,
    onVocSelected: (Int) -> Unit
) {
    println("LOG UI: Historial para ${vocData.name} tiene ${vocData.history.size} puntos.")
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val chartEntryModelProducer = remember(vocData.history) {
        ChartEntryModelProducer(vocData.history.map { entryOf(it.first, it.second) })
    }

    val adaptiveBottomAxisFormatter = rememberAdaptiveAxisFormatter(history = vocData.history)

    val itemPlacer = if (vocData.history.size > 25) {
        AxisItemPlacer.Horizontal.default(spacing = 5, offset = 1)
    } else {
        AxisItemPlacer.Horizontal.default(spacing = 2, offset = 1)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- TÍTULO Y SPINNER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Nivel de ${vocData.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
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
        }
        Spacer(modifier = Modifier.height(16.dp))

        // El componente del gráfico
        Chart(
            chart = lineChart(),
            chartModelProducer = chartEntryModelProducer,
            bottomAxis = rememberBottomAxis(
                valueFormatter = adaptiveBottomAxisFormatter,
                title = "Hora",
                guideline = null, // Quitar guideline para que itemPlacer tenga control total
                labelRotationDegrees = 0f, // La rotación ya no debería ser necesaria
                // --- CAMBIO: Se aplica el itemPlacer ---
                itemPlacer = itemPlacer
            ),
            modifier = Modifier.height(150.dp)
        )
    }
}