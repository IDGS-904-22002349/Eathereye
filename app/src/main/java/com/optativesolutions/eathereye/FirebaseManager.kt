package com.optativesolutions.eathereye

import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    init {
        readNotifications()
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
}