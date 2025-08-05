package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
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
    val screenTitles: List<String> = listOf("Inicio", "Extracción Manual", "Alertas", "Configuración")
) {
    val currentScreenTitle: String get() = screenTitles[selectedScreenIndex]
    val selectedVoc: VocData get() = availableVocs.getOrElse(selectedVocIndex) { availableVocs.first() }
}


// --- CAMBIO --- El constructor ya no incluye historyListener
class HomeViewModel(
    private val mqttManager: MqttManager,
    private val firebaseManager: FirebaseManager,
    private val notificationHelper: NotificationHelper,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AirQualityUiState())
    val uiState: StateFlow<AirQualityUiState> = _uiState.asStateFlow()

    // --- CAMBIO --- historyListener se define como una propiedad de la clase
    private var historyListener: ValueEventListener? = null

    init {
        observeMqttConnection()
    }

    private fun publishSelectedVoc(vocKey: String) {
        // Publicamos la clave del VOC seleccionado en un tópico específico.
        // Otros dispositivos pueden suscribirse a este tópico para saber qué ve el usuario.
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
                viewModelScope.launch { // Añadir este launch
                    _uiState.update { it.copy(pressure = newPressure) }
                }
            }
        }
        mqttManager.subscribe("sensor/temperature") { message ->
            message.toFloatOrNull()?.let { newTemp ->
                viewModelScope.launch { // Añadir este launch
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
                    viewModelScope.launch { // Añadir este launch
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

        // Se suscribe al historial del VOC seleccionado por defecto al iniciar
        attachHistoryListenerFor(uiState.value.selectedVoc.key)

        publishSelectedVoc(uiState.value.selectedVoc.key)
    }

    // --- CAMBIO --- Nueva función para manejar la lógica del listener del historial
    private fun attachHistoryListenerFor(vocName: String) {
        removeHistoryListener()

        // El callback ahora recibe un DataSnapshot
        historyListener = firebaseManager.getHistoricalDataListener(vocName) { snapshot ->
            // Inicia una corrutina en un hilo secundario para no bloquear la UI
            viewModelScope.launch(Dispatchers.Default) {
                // --- TRABAJO PESADO EN SEGUNDO PLANO ---
                val dataList = snapshot.children.mapNotNull {
                    val timestamp = it.key?.toLongOrNull()
                    val rawValue: Any? = it.child("value").value
                    // Obtenemos el valor como un objeto y lo convertimos a Number de forma segura
                    val value: Float? = when (rawValue) {
                        is Long -> rawValue.toFloat()
                        is Double -> rawValue.toFloat()
                        else -> null // Ignora cualquier otro tipo de dato
                    }
                    if (timestamp != null && value != null) {
                        Pair(timestamp, value)
                    } else {
                        null
                    }
                }
                println("LOG VM: Datos históricos recibidos para $vocName: ${dataList.size} puntos.")

                // Una vez procesada la lista, actualizamos el estado de la UI
                // en el hilo principal, que es un requisito de Compose.
                launch(Dispatchers.Main) {
                    _uiState.update { currentState ->
                        val updatedVocs = currentState.availableVocs.map {
                            if (it.key == vocName) {
                                it.copy(history = dataList)
                            } else {
                                it
                            }
                        }
                        currentState.copy(availableVocs = updatedVocs)
                    }
                }
            }
        }
    }

    // --- CAMBIO --- Nueva función para limpiar el listener
    private fun removeHistoryListener() {
        historyListener?.let {
            firebaseManager.removeHistoricalDataListener(uiState.value.selectedVoc.key, it)
        }
        historyListener = null
    }

    // --- CAMBIO --- Lógica mejorada para cuando se selecciona un nuevo VOC
    fun onVocSelected(index: Int) {
        // Actualiza el estado con el nuevo índice seleccionado
        _uiState.update { it.copy(selectedVocIndex = index) }
        // Se suscribe al historial del NUEVO VOC seleccionado
        val newSelectedVocKey = uiState.value.selectedVoc.key
        // El estado ya se actualizó en la línea anterior, por lo que selectedVoc.name es el nuevo.
        attachHistoryListenerFor(newSelectedVocKey)

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

    // --- CAMBIO --- onCleared ahora limpia el listener
    override fun onCleared() {
        super.onCleared()
        removeHistoryListener()
    }
}