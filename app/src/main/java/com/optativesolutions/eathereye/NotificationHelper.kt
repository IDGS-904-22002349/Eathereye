package com.optativesolutions.eathereye

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NORMAL_CHANNEL_ID = "normal_alerts_channel"
        const val ALARM_CHANNEL_ID = "alarm_alerts_channel"
    }

    init {
        val normalChannel = NotificationChannel(
            NORMAL_CHANNEL_ID,
            "Alertas de Contaminación",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones cuando se superan los umbrales."
        }

        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Alarmas de Contaminación",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas críticas con sonido de alarma."
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(normalChannel)
        notificationManager.createNotificationChannel(alarmChannel)
    }

    fun showNotification(title: String, message: String, useAlarmSound: Boolean) {
        val channelId = if (useAlarmSound) ALARM_CHANNEL_ID else NORMAL_CHANNEL_ID

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con tu ícono
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(context, NORMAL_CHANNEL_ID)
            .setContentTitle("AetherEye")
            .setContentText("Protección en tiempo real activada.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con tu ícono
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}