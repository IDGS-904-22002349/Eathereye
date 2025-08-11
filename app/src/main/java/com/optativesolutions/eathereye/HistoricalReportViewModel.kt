package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import java.util.TimeZone
import java.util.Calendar
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Date

// Estado de la UI para la pantalla de reportes
data class ReportUiState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val selectedFormat: String = "PDF",
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingDates: Boolean = false,
    val userMessage: String? = null,
    val selectableDates: Set<Long>? = null,
    val errorMessage: String? = null
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

    val canGenerateReport: Boolean get() = startDate != null &&
            endDate != null &&
            !isLoading &&
            startDate <= endDate
}

class HistoricalReportViewModel(
    private val firebaseManager: FirebaseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    private var sensorKeyToReport: String = "benzene"

    fun setSensorKey(key: String) {
        sensorKeyToReport = key
        // Recarga las fechas disponibles cuando cambia el sensor
        loadSelectableDates()
    }

    fun onStartDateSelected(date: Long?) {
        _uiState.update {
            val newState = it.copy(startDate = date, showStartDatePicker = false)
            // Valida que la fecha de inicio no sea mayor que la fecha de fin
            if (newState.endDate != null && date != null && date > newState.endDate) {
                newState.copy(endDate = null, errorMessage = "La fecha de inicio no puede ser mayor que la fecha de fin")
            } else {
                newState.copy(errorMessage = null)
            }
        }
    }

    fun onEndDateSelected(date: Long?) {
        _uiState.update {
            val newState = it.copy(endDate = date, showEndDatePicker = false)
            // Valida que la fecha de fin no sea menor que la fecha de inicio
            if (newState.startDate != null && date != null && date < newState.startDate) {
                newState.copy(startDate = null, errorMessage = "La fecha de fin no puede ser menor que la fecha de inicio")
            } else {
                newState.copy(errorMessage = null)
            }
        }
    }

    fun loadSelectableDates() {
        // Evita recargar si ya las tenemos para el mismo sensor
        if (_uiState.value.selectableDates != null) return

        _uiState.update { it.copy(isLoadingDates = true, errorMessage = null) }

        firebaseManager.getDatesWithData(
            sensorName = sensorKeyToReport,
            onComplete = { dates ->
                _uiState.update {
                    it.copy(
                        selectableDates = dates,
                        isLoadingDates = false
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isLoadingDates = false,
                        errorMessage = "Error al cargar fechas disponibles: $error"
                    )
                }
            }
        )
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

        if (!state.canGenerateReport) {
            _uiState.update { it.copy(userMessage = "Por favor, selecciona un rango de fechas válido.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, userMessage = null, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Forzamos a UTC para que coincida con Firebase
                val startDateLocalDate = Instant.ofEpochMilli(state.startDate!!)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()

                val endDateLocalDate = Instant.ofEpochMilli(state.endDate!!)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()

// Crea el rango de consulta (inicio y fin del día) USANDO LA ZONA HORARIA DE LA APP
                val finalStartDate = startDateLocalDate.atStartOfDay(FirebaseManager.APP_TIMEZONE)
                    .toInstant()
                    .toEpochMilli()

                val finalEndDate = endDateLocalDate.atTime(23, 59, 59, 999_999_999)
                    .atZone(FirebaseManager.APP_TIMEZONE)
                    .toInstant()
                    .toEpochMilli()

                val sensorToReportName = VocUtils.getVocNameByKey(sensorKeyToReport)

                firebaseManager.fetchReportData(
                    sensorName = sensorKeyToReport,
                    startDate = finalStartDate,
                    endDate = finalEndDate,
                    onComplete = { data ->
                        handleReportData(context, data, sensorToReportName, state)
                    },
                    onError = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Error al obtener datos: $error"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleReportData(
        context: Context,
        data: List<Pair<Long, Float>>,
        sensorName: String,
        state: ReportUiState
    ) {
        if (data.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userMessage = "No se encontraron registros en ese rango."
                )
            }
            return
        }

        try {
            when (state.selectedFormat) {
                "PDF" -> {
                    val reportGenerator = PdfReportGenerator()
                    reportGenerator.generate(
                        context,
                        sensorName,
                        state.startDate!!,
                        state.endDate!!,
                        data
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "Reporte PDF guardado en Descargas."
                        )
                    }
                }
                "CSV" -> {
                    val reportGenerator = CsvReportGenerator()
                    reportGenerator.generate(context, sensorName, data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "Reporte CSV guardado en Descargas."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Error al generar ${state.selectedFormat}: ${e.message}"
                )
            }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearDatesCache() {
        _uiState.update { it.copy(selectableDates = null) }
    }
}