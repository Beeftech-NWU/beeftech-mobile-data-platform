package com.beeftech.management

import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.ReportFormat
import com.beeftech.management.data.ReportKind
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportFileNameTest {

    private fun client(contentDisposition: String?): ManagementApiClient {
        val engine = MockEngine {
            val headers = if (contentDisposition == null) {
                headersOf(HttpHeaders.ContentType, "application/pdf")
            } else {
                headersOf(
                    HttpHeaders.ContentType to listOf("application/pdf"),
                    HttpHeaders.ContentDisposition to listOf(contentDisposition)
                )
            }
            respond(byteArrayOf(1, 2, 3), HttpStatusCode.OK, headers)
        }
        return ManagementApiClient(
            tokenProvider = object : TokenProvider {
                override suspend fun token(): String? = "tok"
            },
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine)
        )
    }

    private suspend fun nameFor(contentDisposition: String?): String {
        val result = client(contentDisposition)
            .reportFile(ReportKind.entries.first(), ReportFormat.PDF, from = 0, to = 1_800_000_000_000L)
        return (result as ManagementResult.Success).value.name
    }

    @Test
    fun `the report keeps the name the server gives it`() = runTest {
        assertEquals(
            "BF01-REPORT-20261008-140509-SERVER.pdf",
            nameFor("attachment; filename=BF01-REPORT-20261008-140509-SERVER.pdf")
        )
    }

    @Test
    fun `a missing or unsafe server name falls back to the app's own`() = runTest {
        val fallback = nameFor(null)
        assertEquals(true, fallback.startsWith("beeftech-") && fallback.endsWith(".pdf"))
        assertEquals(fallback, nameFor("attachment; filename=../evil.pdf"))
    }
}
