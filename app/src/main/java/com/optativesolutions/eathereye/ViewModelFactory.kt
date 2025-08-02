package com.optativesolutions.eathereye

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = application as MyApplication

        return when {
            // Sabe cómo crear HomeViewModel con sus dependencias
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    mqttManager = app.mqttManager,
                    firebaseManager = app.firebaseManager,
                    notificationHelper = app.notificationHelper,
                    settingsManager = app.settingsManager // <- Pasa el manager, no el ViewModel
                ) as T
            }
            // Sabe cómo crear AlertsViewModel
            modelClass.isAssignableFrom(AlertsViewModel::class.java) -> {
                AlertsViewModel(app.firebaseManager) as T
            }
            // Sabe cómo crear SettingsViewModel
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(app.settingsManager) as T
            }
            // Los ViewModels sin dependencias no necesitan estar aquí

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}