package com.beeftech.backend.api

object ReportCsv {

    /* A cell that starts with one of these is read as a formula by Excel and Sheets. */
    private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

    fun cell(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in FORMULA_STARTS) "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    fun render(report: ReportResponse): String {
        val out = StringBuilder()
        fun line(cells: List<String>) {
            out.append(cells.joinToString(",") { cell(it) }).append("\r\n")
        }
        line(report.columns)
        report.rows.forEach { line(it) }
        return out.toString()
    }
}
