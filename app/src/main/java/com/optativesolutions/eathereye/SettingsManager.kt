package com.optativesolutions.eathereye

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {
    private val dataStore = context.dataStore

    // Claves para cada ajuste
    companion object {
        val ARE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("are_notifications_enabled")
        val IS_ALARM_SOUND_ON = booleanPreferencesKey("is_alarm_sound_on")
    }

    // Flujo para leer si las notificaciones están activadas
    val settingsFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            areNotificationsEnabled = preferences[ARE_NOTIFICATIONS_ENABLED] ?: true,
            isAlarmSoundOn = preferences[IS_ALARM_SOUND_ON] ?: false
        )
    }

    // Función para guardar el estado de las notificaciones
    suspend fun setNotificationsEnabled(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ARE_NOTIFICATIONS_ENABLED] = isEnabled
        }
    }

    suspend fun setAlarmSoundOn(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_ALARM_SOUND_ON] = isEnabled
        }
    }

    // Aquí también guardarías y leerías los umbrales de VOCs
}

data class UserPreferences(
    val areNotificationsEnabled: Boolean,
    val isAlarmSoundOn: Boolean
)