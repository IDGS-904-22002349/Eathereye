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
import java.util.TimeZone
import java.util.concurrent.TimeUnit

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
fun rememberAdaptiveAxisFormatter(history: List<Pair<Long, Float>>): AxisValueFormatter<AxisPosition.Horizontal.Bottom> {
    val dateFormat = remember(history) {
        val cdmxTimeZone = TimeZone.getTimeZone("America/Mexico_City")
        val timestamps = history.map { it.first }

        if (timestamps.isEmpty()) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                timeZone = cdmxTimeZone
            }
        } else {
            val minTimestamp = timestamps.minOrNull() ?: 0L
            val maxTimestamp = timestamps.maxOrNull() ?: 0L
            val durationMillis = maxTimestamp - minTimestamp

            when {
                durationMillis > TimeUnit.DAYS.toMillis(1) ->
                    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).apply {
                        timeZone = cdmxTimeZone
                    }
                else ->
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

    // --- INICIA LA CORRECCIÓN ---
    // 1. Creamos el productor de datos UNA SOLA VEZ y lo recordamos.
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }

    // 2. Usamos un `LaunchedEffect` que se ejecutará cada vez que `vocData.history` cambie.
    LaunchedEffect(vocData.history) {
        // 3. Le decimos al productor existente que actualice sus entradas con la nueva lista.
        chartEntryModelProducer.setEntries(vocData.history.map { entryOf(it.first, it.second) })
    }
    // --- TERMINA LA CORRECCIÓN ---

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
                guideline = null,
                labelRotationDegrees = 0f,
                itemPlacer = itemPlacer
            ),
            modifier = Modifier.height(150.dp)
        )
    }
}