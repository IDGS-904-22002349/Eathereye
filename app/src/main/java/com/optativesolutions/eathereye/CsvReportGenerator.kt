package com.optativesolutions.eathereye

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CsvReportGenerator {

    fun generate(
        context: Context,
        sensorName: String,
        data: List<Pair<Long, Float>>
    ): Uri? {
        val fileName = "Reporte_${sensorName}_${System.currentTimeMillis()}.csv"
        val stringBuilder = StringBuilder()

        // --- Creación del Contenido del CSV ---
        val cdmxTimeZone = TimeZone.getTimeZone("America/Mexico_City")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = cdmxTimeZone }

        // Encabezado del CSV
        stringBuilder.append("Fecha y Hora (CDMX),Lectura (ppm)\n")

        // Filas con los datos
        data.forEach { entry ->
            val dateString = dateFormat.format(Date(entry.first))
            val valueString = String.format(Locale.US, "%.2f", entry.second) // Usa Locale.US para asegurar el punto decimal
            stringBuilder.append("$dateString,$valueString\n")
        }

        // --- Lógica para guardar el archivo en la carpeta de Descargas ---
        try {
            var uri: Uri?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                resolver.openOutputStream(uri!!)?.use { outputStream ->
                    outputStream.write(stringBuilder.toString().toByteArray())
                }
            } else {
                // Lógica para versiones antiguas de Android (opcional pero recomendado)
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(stringBuilder.toString())
                uri = Uri.fromFile(file)
            }
            println("CSV generado exitosamente en la ruta: $uri")
            return uri
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error al generar CSV: ${e.message}")
            return null
        }
    }
}