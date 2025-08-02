package com.optativesolutions.eathereye

import android.app.Application

class MyApplication : Application() {

    // Instancias únicas (Singletons) que vivirán durante toda la app.
    // Usamos 'lazy' para que se creen solo cuando se necesiten por primera vez.
    val notificationHelper: NotificationHelper by lazy { NotificationHelper(applicationContext) }
    val settingsManager: SettingsManager by lazy { SettingsManager(applicationContext) }
    val firebaseManager: FirebaseManager by lazy { FirebaseManager() }
    val mqttManager: MqttManager by lazy { MqttManager(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        // Opcional: Puedes inicializar alguno aquí si quieres que se cree al iniciar la app.
        // Por ejemplo, para que MQTT se prepare desde el principio.
        mqttManager
    }
}