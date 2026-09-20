package com.beeftech.backend.api

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateBirthCertificate(calf: CalfRegistrationDto): ByteArray {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)

        val outputStream = ByteArrayOutputStream()

        PDPageContentStream(document, page).use { contentStream ->
            // Title Header
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
            contentStream.newLineAtOffset(50f, 750f)
            contentStream.showText("BEEFTECH DATA PLATFORM")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
            contentStream.newLineAtOffset(50f, 725f)
            contentStream.showText("OFFICIAL CALF BIRTH CERTIFICATE")
            contentStream.endText()

            // Line separator
            contentStream.setLineWidth(1f)
            contentStream.moveTo(50f, 710f)
            contentStream.lineTo(550f, 710f)
            contentStream.stroke()

            // Details
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = dateFormat.format(Date(calf.captureAt))

            val details = listOf(
                "Ear Tag ID (Animal ID):" to calf.animalId,
                "Breed / Animal Type:" to calf.breed,
                "Dam Tag Number:" to (calf.damId ?: "N/A"),
                "Sire Tag Number:" to (calf.sireId ?: "N/A"),
                "Capture Timestamp:" to dateStr,
                "Device Identifier:" to calf.deviceId,
                "Record GUID:" to calf.recordguid,
                "Sync Status:" to calf.syncStatus,
                "Photo Attachment:" to (calf.photoPath ?: "None")
            )

            var yPosition = 670f
            for ((label, value) in details) {
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11f)
                contentStream.newLineAtOffset(50f, yPosition)
                contentStream.showText(label)
                contentStream.endText()

                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA, 11f)
                contentStream.newLineAtOffset(220f, yPosition)
                contentStream.showText(value)
                contentStream.endText()

                yPosition -= 25f
            }

            // Footer
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 9f)
            contentStream.newLineAtOffset(50f, 50f)
            contentStream.showText("Generated automatically by Beeftech Mobile Data Platform • Verified Record")
            contentStream.endText()
        }

        document.save(outputStream)
        document.close()

        return outputStream.toByteArray()
    }
}
