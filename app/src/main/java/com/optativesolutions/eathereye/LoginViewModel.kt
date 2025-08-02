package com.optativesolutions.eathereye

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// Define los posibles estados de la UI para el login
sealed interface LoginUiState {
    object Idle : LoginUiState // Estado inicial
    object Loading : LoginUiState // Podrías usarlo para mostrar un spinner
    object Success : LoginUiState // Login exitoso
    data class Error(val message: String) : LoginUiState // Error de login
}

class LoginViewModel : ViewModel() {
    // Estado del usuario (público e inmutable)
    var username by mutableStateOf("")
        private set

    // Estado de la contraseña (público e inmutable)
    var password by mutableStateOf("")
        private set

    // Estado de la UI (público e inmutable)
    var uiState: LoginUiState by mutableStateOf(LoginUiState.Idle)
        private set

    // Funciones para actualizar el estado desde la UI
    fun onUsernameChange(newUsername: String) {
        username = newUsername
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    // Lógica del login
    fun login() {
        if (username == "admin" && password == "password") {
            uiState = LoginUiState.Success
        } else {
            uiState = LoginUiState.Error("Usuario o contraseña incorrectos")
        }
    }
}