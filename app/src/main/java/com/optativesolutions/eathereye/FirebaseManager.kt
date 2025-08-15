package com.optativesolutions.eathereye

import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import java.util.TimeZone
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.ZoneId

data class AlertNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

class FirebaseManager {

    companion object {
        // Zona horaria consistente para toda la aplicación
        val APP_TIMEZONE: ZoneId = ZoneId.of("America/Mexico_City")
        private const val MAX_RETRIES = 3
    }

    private val database = Firebase.database.reference.child("notifications")
    private val _notifications = MutableStateFlow<List<AlertNotification>>(emptyList())
    val notifications: StateFlow<List<AlertNotification>> = _notifications
    private val historicalDb = Firebase.database.reference.child("historical_data")

    // Mantiene registro de listeners activos para evitar memory leaks
    private val activeListeners = mutableMapOf<String, ValueEventListener>()

    init {
        readNotifications()
    }

    fun saveHistoricalReading(
        sensorName: String,
        value: Float,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val reading = mapOf(
            "value" to value,
            "timestamp" to timestamp // Agregamos timestamp explícito
        )

        historicalDb.child(sensorName.lowercase())
            .child(timestamp.toString())
            .setValue(reading)
            .addOnSuccessListener {
                println("Dato histórico guardado para $sensorName: $value")
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                val errorMsg = "Error al guardar dato histórico: ${exception.message}"
                println(errorMsg)
                onError?.invoke(errorMsg)
            }
    }

    fun saveNotification(
        title: String,
        message: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val id = database.push().key
        if (id == null) {
            onError?.invoke("No se pudo generar ID para la notificación")
            return
        }

        val notification = AlertNotification(id, title, message, System.currentTimeMillis())
        database.child(id).setValue(notification)
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { onError?.invoke(it.message ?: "Error desconocido") }
    }

    private fun readNotifications() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val notificationList = snapshot.children.mapNotNull {
                        it.getValue(AlertNotification::class.java)
                    }
                    _notifications.value = notificationList.sortedByDescending { it.timestamp }
                } catch (e: Exception) {
                    println("Error al leer notificaciones: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error en listener de notificaciones: ${error.message}")
            }
        })
    }

    fun getHistoricalDataListener(
        sensorKey: String,
        onDataChange: (DataSnapshot) -> Unit,
        onError: ((String) -> Unit)? = null
    ): ValueEventListener {
        val query = historicalDb.child(sensorKey).orderByKey().limitToLast(100)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    onDataChange(snapshot)
                } catch (e: Exception) {
                    onError?.invoke("Error al procesar datos históricos: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                val errorMsg = "Error al leer historial: ${error.message}"
                println(errorMsg)
                onError?.invoke(errorMsg)
            }
        }

        query.addValueEventListener(listener)
        activeListeners[sensorKey] = listener
        return listener
    }

    fun removeHistoricalDataListener(sensorKey: String, listener: ValueEventListener) {
        historicalDb.child(sensorKey).removeEventListener(listener)
        activeListeners.remove(sensorKey)
    }

    fun getDatesWithData(
        sensorName: String,
        onComplete: (Set<Long>) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val query = historicalDb.child(sensorName.lowercase()).orderByKey()

        query.get().addOnSuccessListener { snapshot ->
            try {
                val datesWithData = snapshot.children.mapNotNull {
                    it.key?.toLongOrNull()
                }.map { timestamp ->
                    // Convertir timestamp a fecha UTC medianoche para consistency
                    val instant = Instant.ofEpochMilli(timestamp)
                    val localDate = instant.atZone(APP_TIMEZONE).toLocalDate()
                    val utcMidnightInstant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                    utcMidnightInstant.toEpochMilli()
                }.toSet()

                onComplete(datesWithData)
            } catch (e: Exception) {
                onError?.invoke("Error al procesar fechas: ${e.message}")
            }
        }.addOnFailureListener { exception ->
            val errorMsg = "Error al obtener fechas con datos: ${exception.message}"
            onError?.invoke(errorMsg)
        }
    }

    fun fetchReportData(
        sensorName: String,
        startDate: Long,
        endDate: Long,
        onComplete: (List<Pair<Long, Float>>) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        // Validación de parámetros
        if (startDate > endDate) {
            onError?.invoke("La fecha de inicio no puede ser mayor que la fecha de fin")
            return
        }

        val query = historicalDb.child(sensorName.lowercase())
            .orderByKey()
            .startAt(startDate.toString())
            .endAt((endDate + 1).toString()) // +1 ms para incluir el último registro

        query.get().addOnSuccessListener { snapshot ->
            try {
                val dataList = snapshot.children.mapNotNull { dataSnapshot ->
                    val timestamp = dataSnapshot.key?.toLongOrNull()
                    val rawValue: Any? = dataSnapshot.child("value").value

                    val value: Float? = when (rawValue) {
                        is Long -> rawValue.toFloat()
                        is Double -> rawValue.toFloat()
                        is Float -> rawValue
                        is Int -> rawValue.toFloat()
                        else -> {
                            println("Tipo de dato no reconocido para valor: $rawValue (${rawValue?.javaClass})")
                            null
                        }
                    }

                    if (timestamp != null && value != null &&
                        timestamp >= startDate && timestamp <= endDate) {
                        Pair(timestamp, value)
                    } else null
                }.sortedBy { it.first } // Ordenar por timestamp

                println("Datos obtenidos para reporte: ${dataList.size} registros")
                onComplete(dataList)
            } catch (e: Exception) {
                val errorMsg = "Error al procesar datos del reporte: ${e.message}"
                onError?.invoke(errorMsg)
            }
        }.addOnFailureListener { exception ->
            val errorMsg = "Error al obtener datos del reporte: ${exception.message}"
            onError?.invoke(errorMsg)
        }
    }

    // Método para limpiar todos los listeners al destruir la instancia
    fun cleanup() {
        activeListeners.forEach { (sensorKey, listener) ->
            historicalDb.child(sensorKey).removeEventListener(listener)
        }
        activeListeners.clear()
    }

    // Método para verificar conectividad
    fun checkConnectivity(onResult: (Boolean) -> Unit) {
        val testRef = Firebase.database.reference.child(".info/connected")
        testRef.get().addOnCompleteListener { task ->
            onResult(task.isSuccessful && task.result?.getValue(Boolean::class.java) == true)
        }
    }

    // En FirebaseManager.kt

    fun getInitialHistory(
        sensorKey: String,
        onComplete: (List<Pair<Long, Float>>) -> Unit,
        onError: (String) -> Unit
    ) {
        historicalDb.child(sensorKey).orderByKey().limitToLast(100).get()
            .addOnSuccessListener { snapshot ->
                // --- INICIA LA CORRECCIÓN ---
                // Esta es la lógica que faltaba para procesar los datos
                val dataList = snapshot.children.mapNotNull { dataSnapshot ->
                    val timestamp = dataSnapshot.key?.toLongOrNull()
                    val rawValue: Any? = dataSnapshot.child("value").value

                    val value: Float? = when (rawValue) {
                        is Long -> rawValue.toFloat()
                        is Double -> rawValue.toFloat()
                        is Float -> rawValue
                        is Int -> rawValue.toFloat()
                        else -> null // Si el tipo no es numérico, lo ignoramos
                    }

                    if (timestamp != null && value != null) {
                        Pair(timestamp, value)
                    } else {
                        null
                    }
                }.sortedBy { it.first } // Ordenar por timestamp
                // --- TERMINA LA CORRECCIÓN ---

                onComplete(dataList)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error desconocido")
            }
    }

    fun removeNewReadingsListener(sensorKey: String, listener: ChildEventListener) {
        historicalDb.child(sensorKey).removeEventListener(listener)
        // Ya no necesitas el mapa 'activeListeners' para este tipo de listener
    }

    // NUEVA FUNCIÓN para escuchar solo los nuevos datos
    fun getNewReadingsListener(
        sensorKey: String,
        lastTimestamp: Long, // Escucharemos a partir del último dato que ya tenemos
        onNewChild: (Pair<Long, Float>) -> Unit,
        onError: (String) -> Unit
    ): ChildEventListener {
        val query = historicalDb.child(sensorKey).orderByKey().startAt(lastTimestamp.toString())

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // El timestamp del snapshot.key debe ser > lastTimestamp para ser realmente nuevo
                val newTimestamp = snapshot.key?.toLongOrNull() ?: 0L
                if (newTimestamp > lastTimestamp) {
                    // (El mismo código para procesar un solo snapshot)
                    val value = snapshot.child("value").value as? Float // Simplificado
                    if (value != null) {
                        onNewChild(Pair(newTimestamp, value))
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        query.addChildEventListener(listener)
        return listener
    }
}