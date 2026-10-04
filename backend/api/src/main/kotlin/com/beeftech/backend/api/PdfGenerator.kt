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
                "Ear Tag ID:" to calf.tagNumber,
                "Breed / Animal Type:" to calf.breed,
                "Dam Tag Number:" to (calf.damTagNumber ?: "N/A"),
                "Sire Tag Number:" to (calf.sireTagNumber ?: "N/A"),
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

    /*
     * A plain paginated table: the first column is left aligned and the others are
     * spread across the page. Text is cut to its column, and characters Helvetica
     * can't draw become '?', because showText throws on them.
     */
    fun generateReport(report: ReportResponse): ByteArray {
        val document = PDDocument()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val scope = report.siteName ?: if (report.siteId != null) report.siteId else "All sites"
        val subtitle = "$scope  |  ${dateFormat.format(Date(report.from))} to ${dateFormat.format(Date(report.to))} (UTC)"

        val left = 50f
        val width = 500f
        val columnWidth = width / report.columns.size
        val lineHeight = 16f
        val bottom = 60f

        fun text(stream: PDPageContentStream, font: PDType1Font, size: Float, x: Float, y: Float, value: String, maxWidth: Float = width) {
            var shown = value.map { if (it.code in 32..126 || it.code in 160..255) it else '?' }.joinToString("")
            while (shown.length > 1 && font.getStringWidth(shown) / 1000f * size > maxWidth) {
                shown = shown.dropLast(1)
            }
            stream.beginText()
            stream.setFont(font, size)
            stream.newLineAtOffset(x, y)
            stream.showText(shown)
            stream.endText()
        }

        fun header(stream: PDPageContentStream, first: Boolean): Float {
            var y = 780f
            if (first) {
                text(stream, PDType1Font.HELVETICA_BOLD, 16f, left, y, "BEEFTECH - ${report.title}")
                y -= 20f
                text(stream, PDType1Font.HELVETICA, 10f, left, y, subtitle)
                y -= 24f
                for (figure in report.summary) {
                    text(stream, PDType1Font.HELVETICA_BOLD, 11f, left, y, figure.label, 200f)
                    text(stream, PDType1Font.HELVETICA, 11f, left + 210f, y, figure.value, 280f)
                    y -= lineHeight
                }
                y -= 10f
            }
            report.columns.forEachIndexed { i, name ->
                text(stream, PDType1Font.HELVETICA_BOLD, 10f, left + i * columnWidth, y, name, columnWidth - 6f)
            }
            y -= 4f
            stream.moveTo(left, y)
            stream.lineTo(left + width, y)
            stream.stroke()
            return y - lineHeight
        }

        var page = PDPage()
        document.addPage(page)
        var stream = PDPageContentStream(document, page)
        var y = header(stream, first = true)

        for (row in report.rows) {
            if (y < bottom) {
                stream.close()
                page = PDPage()
                document.addPage(page)
                stream = PDPageContentStream(document, page)
                y = header(stream, first = false)
            }
            row.forEachIndexed { i, cell ->
                text(stream, PDType1Font.HELVETICA, 10f, left + i * columnWidth, y, cell, columnWidth - 6f)
            }
            y -= lineHeight
        }
        if (report.rows.isEmpty()) {
            text(stream, PDType1Font.HELVETICA_OBLIQUE, 10f, left, y, "No records in this period.")
        }

        report.footer?.let { text(stream, PDType1Font.HELVETICA_OBLIQUE, 8f, left, 40f, it) }
        text(stream, PDType1Font.HELVETICA_OBLIQUE, 8f, left, 28f, "Generated by Beeftech Mobile Data Platform")
        stream.close()

        val output = ByteArrayOutputStream()
        document.save(output)
        document.close()
        return output.toByteArray()
    }
}
