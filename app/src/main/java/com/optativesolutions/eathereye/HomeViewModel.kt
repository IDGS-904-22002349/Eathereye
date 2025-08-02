package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// --- Modelo de datos para los VOCs ---
data class VocData(
    val name: String,
    val topic: String, // Topic MQTT específico para este VOC
    val level: Float = 0f,
    val history: List<Pair<Int, Float>> = emptyList(),
    val change: Float = 0f
)

// --- Modelo de datos principal de la UI ---
data class AirQualityUiState(
    // Valores de los sensores
    val pressure: Float = 1013f, // Nuevo estado para la presión
    val temperature: Int = 22,
    val humidity: Int = 60,

    // Gestión de VOCs
    val availableVocs: List<VocData> = listOf( // Lista de VOCs para el Spinner
        VocData(name = "Benceno", topic = "sensor/voc/benzene"),
        VocData(name = "Tolueno", topic = "sensor/voc/toluene"),
        VocData(name = "Xileno", topic = "sensor/voc/xylene")
    ),
    val selectedVocIndex: Int = 0, // Índice del VOC seleccionado
    val isExtractionSystemActive: Boolean = false,
    // Gestión de la navegación
    val selectedScreenIndex: Int = 0,
    val screenTitles: List<String> = listOf("Inicio", "Extracción Manual", "Alertas", "Configuración")
) {
    val currentScreenTitle: String get() = screenTitles[selectedScreenIndex]
    val selectedVoc: VocData get() = availableVocs.getOrElse(selectedVocIndex) { availableVocs.first() }
}

class HomeViewModel (
    private val mqttManager: MqttManager,
    private val firebaseManager: FirebaseManager,
    private val notificationHelper: NotificationHelper,
    private val settingsManager: SettingsManager
    // TODO: Inyectar FirebaseManager y NotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AirQualityUiState())
    val uiState: StateFlow<AirQualityUiState> = _uiState.asStateFlow()

    init {
        observeMqttConnection()
    }

    private fun observeMqttConnection() {
        viewModelScope.launch {
            // Recolectamos el flujo. Si alguna vez nos desconectamos y
            // reconectamos, este código se volverá a ejecutar.
            mqttManager.isConnected.collect { connected ->
                if (connected) {
                    // Solo cuando estemos conectados, nos suscribimos.
                    subscribeToUiTopics()
                }
            }
        }
    }

    private fun subscribeToUiTopics() {
        // Esta función ahora solo es llamada cuando es seguro hacerlo.
        // El código de adentro es el mismo que tenías.

        // Suscripción a Presión y Temperatura
        mqttManager.subscribe("sensor/pressure") { message ->
            message.toFloatOrNull()?.let { newPressure ->
                _uiState.update { it.copy(pressure = newPressure) }
            }
        }
        mqttManager.subscribe("sensor/temperature") { message ->
            message.toIntOrNull()?.let { newTemp ->
                _uiState.update { it.copy(temperature = newTemp) }
            }
        }

        // Suscripción a VOCs
        _uiState.value.availableVocs.forEach { voc ->
            mqttManager.subscribe(voc.topic) { message ->
                message.toFloatOrNull()?.let { newLevel ->
                    _uiState.update { currentState ->
                        val updatedVocs = currentState.availableVocs.map {
                            if (it.topic == voc.topic) it.copy(level = newLevel) else it
                        }
                        currentState.copy(availableVocs = updatedVocs)
                    }
                }
            }
        }
    }

    fun onVocSelected(index: Int) {
        _uiState.update { it.copy(selectedVocIndex = index) }
    }

    fun activateExtractionSystem(activate: Boolean) {
        val command = if (activate) "ON" else "OFF"
        mqttManager.publish("actuator/extraction/command", command)
        _uiState.update { it.copy(isExtractionSystemActive = activate) }
    }

    fun onScreenSelected(index: Int) {
        _uiState.update { it.copy(selectedScreenIndex = index) }
    }

    override fun onCleared() {
        super.onCleared()
    }
}