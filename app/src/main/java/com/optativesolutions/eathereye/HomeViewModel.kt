package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- Data classes (sin cambios) ---
data class VocData(
    val key: String,
    val name: String,
    val topic: String,
    val level: Float = 0f,
    val history: List<Pair<Long, Float>> = emptyList(),
    val change: Float = 0f
)

data class AirQualityUiState(
    val pressure: Float = 1013f,
    val temperature: Float = 22.0f,
    val humidity: Float = 60.0f,
    val availableVocs: List<VocData> = listOf(
        VocData(key = "benzene", name = "Acetona", topic = "sensor/voc/benzene"),
        VocData(key = "toluene", name = "Alcohol Isopropílico", topic = "sensor/voc/toluene")
    ),
    val selectedVocIndex: Int = 0,
    val isExtractionSystemActive: Boolean = false,
    val selectedScreenIndex: Int = 0,
    val screenTitles: List<String> = listOf("Inicio", "Extracción Manual", "Alertas", "Configuración"),
    val errorMessage: String? = null // Agregado para manejar errores
) {
    val currentScreenTitle: String get() = screenTitles[selectedScreenIndex]
    val selectedVoc: VocData get() = availableVocs.getOrElse(selectedVocIndex) { availableVocs.first() }
}

class HomeViewModel(
    private val mqttManager: MqttManager,
    private val firebaseManager: FirebaseManager,
    private val notificationHelper: NotificationHelper,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AirQualityUiState())
    val uiState: StateFlow<AirQualityUiState> = _uiState.asStateFlow()

    // historyListener se define como una propiedad de la clase
    private var historyListener: ChildEventListener? = null

    init {
        observeMqttConnection()
    }

    private fun publishSelectedVoc(vocKey: String) {
        mqttManager.publish("ui/selection/active_voc", vocKey)
    }

    private fun observeMqttConnection() {
        viewModelScope.launch {
            mqttManager.isConnected.collect { connected ->
                if (connected) {
                    subscribeToUiTopics()
                }
            }
        }
    }

    private fun subscribeToUiTopics() {
        // Suscripción a Presión y Temperatura
        mqttManager.subscribe("sensor/pressure") { message ->
            message.toFloatOrNull()?.let { newPressure ->
                viewModelScope.launch {
                    _uiState.update { it.copy(pressure = newPressure) }
                }
            }
        }

        mqttManager.subscribe("sensor/temperature") { message ->
            message.toFloatOrNull()?.let { newTemp ->
                viewModelScope.launch {
                    _uiState.update { it.copy(temperature = newTemp) }
                }
            }
        }

        mqttManager.subscribe("sensor/humidity") { message ->
            message.toFloatOrNull()?.let { newHumidity ->
                viewModelScope.launch {
                    _uiState.update { it.copy(humidity = newHumidity) }
                }
            }
        }

        // Suscripción a valores en tiempo real de los VOCs
        _uiState.value.availableVocs.forEach { voc ->
            mqttManager.subscribe(voc.topic) { message ->
                message.toFloatOrNull()?.let { newLevel ->

                    // La única responsabilidad aquí es actualizar la UI.
                    // La llamada a firebaseManager.saveHistoricalReading() se ha eliminado.
                    _uiState.update { currentState ->
                        val updatedVocs = currentState.availableVocs.map {
                            if (it.topic == voc.topic) it.copy(level = newLevel) else it
                        }
                        currentState.copy(availableVocs = updatedVocs)
                    }

                }
            }
        }

        viewModelScope.launch {
            delay(1500) // Espera 1.5 segundos antes de cargar los datos del gráfico.
            attachHistoryListenerFor(uiState.value.selectedVoc.key)
        }

        // Publicar el VOC inicial. Esto ahora se ejecutará antes de la carga pesada.
        publishSelectedVoc(uiState.value.selectedVoc.key)
    }

    // Nueva función para manejar la lógica del listener del historial
    private fun attachHistoryListenerFor(vocKey: String) {
        println("🔄 Adjuntando listener de historial para: $vocKey")
        removeHistoryListener() // Tu función de limpieza actual

        // 1. OBTENER LA CARGA INICIAL
        firebaseManager.getInitialHistory(
            sensorKey = vocKey,
            onComplete = { initialData ->
                // Actualiza la UI con los primeros 100 puntos
                updateHistoryState(vocKey, initialData)

                // 2. UNA VEZ QUE TENEMOS DATOS, ESCUCHAMOS SOLO LOS NUEVOS
                val lastTimestamp = initialData.lastOrNull()?.first ?: System.currentTimeMillis()

                historyListener = firebaseManager.getNewReadingsListener(
                    sensorKey = vocKey,
                    lastTimestamp = lastTimestamp,
                    onNewChild = { newReading ->
                        // ¡Mucho más eficiente! Solo añadimos el nuevo dato a la lista existente
                        _uiState.update { currentState ->
                            val updatedVocs = currentState.availableVocs.map { voc ->
                                if (voc.key == vocKey) {
                                    // Añade el nuevo dato y quita el más antiguo para mantener el tamaño
                                    val updatedHistory = (voc.history + newReading).takeLast(100)
                                    voc.copy(history = updatedHistory)
                                } else {
                                    voc
                                }
                            }
                            currentState.copy(availableVocs = updatedVocs)
                        }
                    },
                    onError = { /* maneja el error */ }
                )
            },
            onError = { /* maneja el error */ }
        )
    }

    private fun updateHistoryState(vocKey: String, data: List<Pair<Long, Float>>) {
        _uiState.update { currentState ->
            val updatedVocs = currentState.availableVocs.map { voc ->
                if (voc.key == vocKey) voc.copy(history = data) else voc
            }
            currentState.copy(availableVocs = updatedVocs)
        }
    }

    // Nueva función para limpiar el listener
    // En HomeViewModel.kt

    private fun removeHistoryListener() {
        historyListener?.let { listener ->
            val currentVocKey = uiState.value.selectedVoc.key
            println("🧹 Removiendo listener de historial para: $currentVocKey")

            // --- CAMBIO CLAVE AQUÍ ---
            // Llama a la nueva función en lugar de la antigua
            firebaseManager.removeNewReadingsListener(currentVocKey, listener)
        }
        historyListener = null
    }

    // Lógica mejorada para cuando se selecciona un nuevo VOC
    fun onVocSelected(index: Int) {
        val previousVocKey = uiState.value.selectedVoc.key

        // Actualiza el estado con el nuevo índice seleccionado
        _uiState.update { it.copy(selectedVocIndex = index, errorMessage = null) }

        // Se suscribe al historial del NUEVO VOC seleccionado
        val newSelectedVocKey = uiState.value.selectedVoc.key

        println("🔄 Cambiando de VOC: $previousVocKey -> $newSelectedVocKey")

        // Solo cambiar listener si es realmente diferente
        if (previousVocKey != newSelectedVocKey) {
            attachHistoryListenerFor(newSelectedVocKey)
        }

        publishSelectedVoc(newSelectedVocKey)
    }



    fun activateExtractionSystem(activate: Boolean) {
        val command = if (activate) "ON" else "OFF"
        mqttManager.publish("actuator/extraction/command", command)
        _uiState.update { it.copy(isExtractionSystemActive = activate) }
    }

    fun onScreenSelected(index: Int) {
        _uiState.update { it.copy(selectedScreenIndex = index) }
    }

    // Función para limpiar mensajes de error
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // onCleared ahora limpia el listener
    override fun onCleared() {
        super.onCleared()
        println("🧹 Limpiando HomeViewModel")
        removeHistoryListener()

        // Opcional: limpiar también el FirebaseManager si es necesario
        // firebaseManager.cleanup()
    }
}