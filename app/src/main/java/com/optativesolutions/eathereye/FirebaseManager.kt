package com.optativesolutions.eathereye

import com.google.firebase.Firebase
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
    private val database = Firebase.database.reference.child("notifications")
    private val _notifications = MutableStateFlow<List<AlertNotification>>(emptyList())
    val notifications: StateFlow<List<AlertNotification>> = _notifications
    private val historicalDb = Firebase.database.reference.child("historical_data")

    init {
        readNotifications()
    }

    fun saveHistoricalReading(sensorName: String, value: Float) {
        // Usamos el timestamp actual como clave única
        val timestamp = System.currentTimeMillis()
        val reading = mapOf("value" to value)

        // Guardamos en la ruta: historical_data/{nombre_sensor}/{timestamp}
        historicalDb.child(sensorName.lowercase()).child(timestamp.toString()).setValue(reading)
            .addOnSuccessListener {
                // Opcional: imprimir en log que se guardó bien
                println("Dato histórico guardado para $sensorName")
            }
            .addOnFailureListener {
                // Opcional: manejar el error
                println("Error al guardar dato histórico: ${it.message}")
            }
    }

    fun saveNotification(title: String, message: String) {
        val id = database.push().key ?: return
        val notification = AlertNotification(id, title, message, System.currentTimeMillis())
        database.child(id).setValue(notification)
    }

    private fun readNotifications() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificationList = snapshot.children.mapNotNull {
                    it.getValue(AlertNotification::class.java)
                }
                _notifications.value = notificationList.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                // TODO: Manejar error
            }
        })
    }

    fun getHistoricalDataListener(sensorKey: String, onDataChange: (DataSnapshot) -> Unit) : ValueEventListener {
        val query = historicalDb.child(sensorKey).orderByKey().limitToLast(100)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataChange(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                println("Error al leer historial: ${error.message}")
            }
        }
        query.addValueEventListener(listener)
        return listener
    }

    fun removeHistoricalDataListener(sensorKey: String, listener: ValueEventListener) {
        historicalDb.child(sensorKey).removeEventListener(listener)
    }

    fun getDatesWithData(sensorName: String, onComplete: (Set<Long>) -> Unit) {
        val query = historicalDb.child(sensorName.lowercase()).orderByKey()
        val userTimeZone = ZoneId.of("America/Mexico_City")

        query.get().addOnSuccessListener { snapshot ->
            val datesWithData = snapshot.children.mapNotNull {
                it.key?.toLongOrNull()
            }.map { timestamp ->
                val instant = Instant.ofEpochMilli(timestamp)
                val localDate = instant.atZone(userTimeZone).toLocalDate()
                val utcMidnightInstant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                utcMidnightInstant.toEpochMilli()
            }.toSet() // .toSet() elimina los duplicados y nos da los días únicos

            onComplete(datesWithData)
        }
    }

    fun fetchReportData(
        sensorName: String,
        startDate: Long,
        endDate: Long,
        onComplete: (List<Pair<Long, Float>>) -> Unit
    ) {
        val query = historicalDb.child(sensorName.lowercase())
            .orderByKey()
            .startAt(startDate.toString())
            .endAt(endDate.toString())

        query.get().addOnSuccessListener { snapshot ->
            val dataList = snapshot.children.mapNotNull {
                val timestamp = it.key?.toLongOrNull()
                val rawValue: Any? = it.child("value").value
                val value: Float? = when (rawValue) {
                    is Long -> rawValue.toFloat()
                    is Double -> rawValue.toFloat()
                    else -> null // Ignora cualquier otro tipo de dato
                }
                if (timestamp != null && value != null) Pair(timestamp, value) else null
            }
            onComplete(dataList)
        }
    }
}