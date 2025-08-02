package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class AlertsViewModel (firebaseManager: FirebaseManager) : ViewModel() {
    val notifications: StateFlow<List<AlertNotification>> = firebaseManager.notifications
}