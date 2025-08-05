package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import java.util.TimeZone
import java.util.Calendar
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

// Estado de la UI para la pantalla de reportes
data class ReportUiState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val selectedFormat: String = "PDF",
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val selectableDates: Set<Long>? = null
) {
    // Formatea las fechas para mostrarlas en los botones
    private fun Long?.toFormattedString(): String {
        if (this == null) return "Seleccionar fecha"
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneOffset.UTC)
        return formatter.format(Instant.ofEpochMilli(this))
    }

    val startDateString: String get() = startDate.toFormattedString()
    val endDateString: String get() = endDate.toFormattedString()
}

class HistoricalReportViewModel(private val firebaseManager: FirebaseManager) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    private var sensorKeyToReport: String = "benzene"

    fun setSensorKey(key: String) {
        sensorKeyToReport = key
    }

    fun onStartDateSelected(date: Long?) {
        _uiState.update { it.copy(startDate = date, showStartDatePicker = false) }
    }

    fun onEndDateSelected(date: Long?) {
        _uiState.update { it.copy(endDate = date, showEndDatePicker = false) }
    }

    fun loadSelectableDates() {
        // Evita recargar si ya las tenemos
        if (_uiState.value.selectableDates != null) return

        val sensorToReport = "benzene"
        firebaseManager.getDatesWithData(sensorToReport) { dates ->
            _uiState.update { it.copy(selectableDates = dates) }
        }
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

    fun downloadReport(context: Context) {
        val state = _uiState.value
        // Necesitarás saber qué sensor descargar, por ejemplo, "benzene"
        if (state.startDate == null || state.endDate == null) {
            _uiState.update { it.copy(userMessage = "Por favor, selecciona un rango de fechas.") }
            return
        }

        val sensorToReport = sensorKeyToReport
        _uiState.update { it.copy(isLoading = true, userMessage = null) }
        val finalStartDate = state.startDate

        val finalEndDate = Instant.ofEpochMilli(state.endDate)
            .atZone(ZoneOffset.UTC)
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999_000_000)
            .toInstant()
            .toEpochMilli()

        firebaseManager.fetchReportData(sensorToReport, finalStartDate, finalEndDate) { data ->

            if (data.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, userMessage = "No se encontraron registros en ese rango.") }
                return@fetchReportData
            }

            if (state.selectedFormat == "PDF") {
                try {
                    val reportGenerator = PdfReportGenerator()
                    reportGenerator.generate(context, sensorToReport, state.startDate!!, state.endDate!!, data)
                    _uiState.update { it.copy(isLoading = false, userMessage = "Reporte PDF guardado en Descargas.") }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, userMessage = "Error al generar PDF: ${e.message}") }
                }
            } else {
                try {
                    val reportGenerator = CsvReportGenerator()
                    reportGenerator.generate(context, sensorToReport, data)
                    _uiState.update { it.copy(isLoading = false, userMessage = "Reporte CSV guardado en Descargas.") }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, userMessage = "Error al generar CSV: ${e.message}") }
                }
            }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}