package com.optativesolutions.eathereye

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MqttForegroundService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var mqttManager: MqttManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var settingsManager: SettingsManager

    companion object {
        var isServiceRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true

        val app = application as MyApplication
        mqttManager = app.mqttManager
        notificationHelper = app.notificationHelper
        firebaseManager = app.firebaseManager
        settingsManager = app.settingsManager
        connectAndSubscribe()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Muestra la notificación persistente para que el servicio siga vivo
        val foregroundNotification = notificationHelper.createForegroundNotification()
        startForeground(1, foregroundNotification)

        return START_STICKY // El servicio se re-iniciará si el sistema lo mata
    }

    private fun connectAndSubscribe() {
        scope.launch {
            // Intenta conectar
            mqttManager.connect()

            // Espera a que la conexión sea exitosa antes de suscribirse
            mqttManager.isConnected.collect { connected ->
                if(connected) {
                    subscribeToNotificationTopics()
                }
            }
        }
    }

    private fun subscribeToNotificationTopics() {
        // Aquí solo van las suscripciones que generan notificaciones
        mqttManager.subscribe("sensor/voc/benzene") { message ->
            message.toFloatOrNull()?.let { vocLevel ->
                scope.launch {
                    checkThresholdAndNotify("Benceno", vocLevel)
                }
            }
        }
        // TODO: Añadir suscripciones para Tolueno y Xileno si también deben notificar
    }

    private suspend fun checkThresholdAndNotify(vocName: String, vocLevel: Float) {
        // Lee la configuración más reciente desde DataStore
        val preferences = settingsManager.settingsFlow.first()
        // Aquí también leerías el umbral específico para `vocName`
        val vocThreshold = 10f // TODO: Leer umbral desde DataStore

        if (preferences.areNotificationsEnabled && vocLevel > vocThreshold) {
            val title = "¡Alerta de $vocName!"
            val message = "Nivel detectado: $vocLevel ppm. Umbral: $vocThreshold ppm."

            firebaseManager.saveNotification(title, message)
            notificationHelper.showNotification(title, message, preferences.isAlarmSoundOn)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        job.cancel()
        mqttManager.disconnect()
    }
}