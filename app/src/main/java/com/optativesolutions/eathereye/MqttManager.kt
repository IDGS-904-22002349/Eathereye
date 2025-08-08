package com.optativesolutions.eathereye

import android.content.Context
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

class MqttManager(private val context: Context) {

    private val client: Mqtt5AsyncClient

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // CONFIGURACIÓN DEL BROKER (reemplaza con tus datos)
    private val BROKER_HOST = "a726e4550cb749229a095b2d4c2e29e9.s1.eu.hivemq.cloud" // O "localhost" si es local
    private val BROKER_PORT = 8883 // O 1883 si es local y sin TLS
    private val BROKER_USERNAME = "cesarzr"
    private val BROKER_PASSWORD = "Hola123456"

    init {
        client = MqttClient.builder()
            .useMqttVersion5()
            .identifier(UUID.randomUUID().toString()) // Un ID de cliente único
            .serverHost(BROKER_HOST)
            .serverPort(BROKER_PORT)
            .sslWithDefaultConfig() // ¡Importante si usas el puerto 8883!
            .buildAsync()
    }

    fun connect() {
        if (client.state.isConnected) return
        client.connectWith()
            .simpleAuth()
            .username(BROKER_USERNAME)
            .password(BROKER_PASSWORD.toByteArray())
            .applySimpleAuth()
            .send()
            .whenComplete { connAck, throwable ->
                if (throwable != null) {
                    println("❌ Error al conectar al broker MQTT: ${throwable.message}")
                    _isConnected.value = false
                } else {
                    println("✅ Conectado exitosamente al broker MQTT")
                    _isConnected.value = true
                }
            }
    }

    fun subscribe(topic: String, onMessageReceived: (String) -> Unit) {
        client.subscribeWith()
            .topicFilter(topic)
            .callback { publish ->
                // Se ejecuta cada vez que llega un mensaje en el topic suscrito
                val message = StandardCharsets.UTF_8.decode(publish.payload.get()).toString()
                println("📩 Mensaje recibido en topic '$topic': $message")
                onMessageReceived(message)
            }
            .send()
            .whenComplete { subAck, throwable ->
                if (throwable != null) {
                    println("❌ Error al suscribirse al topic '$topic'")
                } else {
                    println("✅ Suscripción exitosa al topic '$topic'")
                }
            }
    }

    fun publish(topic: String, message: String) {
        // -> CAMBIO: Añadir una guarda para no publicar si no estamos conectados.
        if (!_isConnected.value || !client.state.isConnected) {
            println("⚠️ No se puede publicar en '$topic'. Cliente MQTT no conectado.")
            return // Salimos de la función para no causar un error.
        }

        client.publishWith()
            .topic(topic)
            .payload(message.toByteArray())
            .send()
            .whenComplete { publish, throwable ->
                if (throwable != null) {
                    // Este error ahora solo debería aparecer si la conexión se cae justo al publicar.
                    println("❌ Error al publicar en topic '$topic': ${throwable.message}")
                } else {
                    println("🚀 Mensaje publicado exitosamente en topic '$topic'")
                }
            }
    }

    fun disconnect() {
        client.disconnect()
        _isConnected.value = false
        println("🔌 Desconectado del broker MQTT")
    }
}