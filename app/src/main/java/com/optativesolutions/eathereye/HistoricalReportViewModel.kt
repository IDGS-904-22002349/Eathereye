package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*

// Estado de la UI para la pantalla de reportes
data class ReportUiState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val selectedFormat: String = "PDF",
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false
) {
    // Formatea las fechas para mostrarlas en los botones
    private fun Long?.toFormattedString(): String {
        if (this == null) return "Seleccionar fecha"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(this))
    }

    val startDateString: String get() = startDate.toFormattedString()
    val endDateString: String get() = endDate.toFormattedString()
}

class HistoricalReportViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    fun onStartDateSelected(date: Long?) {
        _uiState.update { it.copy(startDate = date, showStartDatePicker = false) }
    }

    fun onEndDateSelected(date: Long?) {
        _uiState.update { it.copy(endDate = date, showEndDatePicker = false) }
    }

    fun onFormatSelected(format: String) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun showStartDatePicker(show: Boolean) {
        _uiState.update { it.copy(showStartDatePicker = show) }
    }

    fun showEndDatePicker(show: Boolean) {
        _uiState.update { it.copy(showEndDatePicker = show) }
    }

    fun downloadReport() {
        // TODO: Aquí iría la lógica para generar y descargar el reporte
        println("Descargando reporte en formato ${_uiState.value.selectedFormat} desde ${_uiState.value.startDateString} hasta ${_uiState.value.endDateString}")
    }
}