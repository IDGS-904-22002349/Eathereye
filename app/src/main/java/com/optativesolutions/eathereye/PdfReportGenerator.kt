package com.optativesolutions.eathereye

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.ZoneId

class PdfReportGenerator {

    fun generate(
        context: Context,
        sensorName: String,
        startDate: Long,
        endDate: Long,
        data: List<Pair<Long, Float>>
    ): Uri? {
        val fileName = "Reporte_${sensorName}_${System.currentTimeMillis()}.pdf"
        val outputStream: FileOutputStream

        var uri: Uri? = null

        // Lógica para guardar el archivo en la carpeta de Descargas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = resolver.openOutputStream(uri!!) as FileOutputStream
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            outputStream = FileOutputStream(file)
        }

        // --- Creación del PDF con iText ---
        val writer = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        // --- Contenido del PDF ---
        val cdmxTimeZone = TimeZone.getTimeZone("America/Mexico_City")
        val titleFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneOffset.UTC)

// Formateador para la tabla (con hora CDMX)
        val tableFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")
            .withZone(ZoneId.of("America/Mexico_City"))

        // Título y subtítulo
        document.add(
            Paragraph("Reporte Histórico de Calidad del Aire")
                .setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER)
        )
        document.add(
            Paragraph("Sensor: ${sensorName.capitalize(Locale.ROOT)}")
                .setTextAlignment(TextAlignment.CENTER)
        )
        document.add(
            Paragraph("Periodo: ${titleFormatter.format(Instant.ofEpochMilli(startDate))} - ${titleFormatter.format(Instant.ofEpochMilli(endDate))}")
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20f)
        )

        // Tabla con los datos
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        // Encabezados de la tabla
        table.addHeaderCell(Cell().add(Paragraph("Fecha y Hora (CDMX)").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("Lectura (ppm)").setBold()))

        // Filas con los datos
        for (entry in data) {
            table.addCell(Cell().add(Paragraph(tableFormatter.format(Instant.ofEpochMilli(entry.first)))))
            table.addCell(Cell().add(Paragraph(String.format("%.2f", entry.second))))
        }

        document.add(table)

        // --- Finalizar y cerrar el documento ---
        document.close()
        println("PDF generado exitosamente en la ruta: $uri")
        return uri
    }
}