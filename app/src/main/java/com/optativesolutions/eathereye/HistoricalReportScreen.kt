package com.optativesolutions.eathereye

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalReportScreen(
    reportViewModel: HistoricalReportViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by reportViewModel.uiState.collectAsState()

    // Dialog para seleccionar fecha de inicio
    if (uiState.showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { reportViewModel.showStartDatePicker(false) },
            confirmButton = {
                TextButton(onClick = { reportViewModel.showStartDatePicker(false) }) {
                    Text("Aceptar")
                }
            }
        ) {
            DatePicker(
                onDateSelected = { dateInMillis ->
                    reportViewModel.onStartDateSelected(dateInMillis)
                }
            )
        }
    }

    // Dialog para seleccionar fecha de fin
    if (uiState.showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { reportViewModel.showEndDatePicker(false) },
            confirmButton = {
                TextButton(onClick = { reportViewModel.showEndDatePicker(false) }) {
                    Text("Aceptar")
                }
            }
        ) {
            DatePicker(
                onDateSelected = { dateInMillis ->
                    reportViewModel.onEndDateSelected(dateInMillis)
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reporte Histórico") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Selecciona el rango de fechas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Selectores de fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { reportViewModel.showStartDatePicker(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(uiState.startDateString)
                }
                OutlinedButton(
                    onClick = { reportViewModel.showEndDatePicker(true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(uiState.endDateString)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Formato de descarga",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Selector de formato
            val formats = listOf("PDF", "CSV")
            formats.forEach { format ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (format == uiState.selectedFormat),
                            onClick = { reportViewModel.onFormatSelected(format) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (format == uiState.selectedFormat),
                        onClick = { reportViewModel.onFormatSelected(format) }
                    )
                    Text(
                        text = format,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { reportViewModel.downloadReport() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF))
            ) {
                Text("Descargar Reporte", fontSize = 18.sp)
            }
        }
    }
}

// Este es el componente DatePicker que usa el DatePickerDialog.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePicker(onDateSelected: (Long) -> Unit) {
    val datePickerState = rememberDatePickerState()
    // Observamos el estado para obtener la fecha seleccionada
    val selectedDate = datePickerState.selectedDateMillis
    if (selectedDate != null) {
        onDateSelected(selectedDate)
    }

    androidx.compose.material3.DatePicker(
        state = datePickerState,
        title = null,
        headline = null,
        showModeToggle = false // Oculta el toggle para cambiar a modo de input
    )
}