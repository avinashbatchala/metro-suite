package com.metro.calendar.data.subscription

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IcsCalendarClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: IcsCalendarClient

    private val validFeed = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n"

    @Before
    fun setUp() {
        val heldCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(heldCertificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(heldCertificate.certificate)
            .build()

        server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()

        val okHttp = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()
        client = IcsCalendarClient(okHttp)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `downloads a valid feed and captures validators`() {
        server.enqueue(MockResponse().setBody(validFeed).setHeader("ETag", "\"v1\""))

        val result = client.fetch(server.url("/cal.ics").toString(), etag = null, lastModified = null)

        assertTrue(result is IcsCalendarClient.FetchResult.Success)
        assertEquals("\"v1\"", (result as IcsCalendarClient.FetchResult.Success).etag)
    }

    @Test
    fun `sends conditional headers and reports not modified`() {
        server.enqueue(MockResponse().setResponseCode(304).setHeader("ETag", "\"v1\""))

        val result = client.fetch(
            server.url("/cal.ics").toString(),
            etag = "\"v1\"",
            lastModified = "Wed, 01 Jan 2024 00:00:00 GMT",
        )

        assertTrue(result is IcsCalendarClient.FetchResult.NotModified)
        val request = server.takeRequest()
        assertEquals("\"v1\"", request.getHeader("If-None-Match"))
        assertEquals("Wed, 01 Jan 2024 00:00:00 GMT", request.getHeader("If-Modified-Since"))
    }

    @Test
    fun `rejects non-calendar responses`() {
        server.enqueue(MockResponse().setBody("<html>not a calendar</html>"))

        val result = client.fetch(server.url("/cal.ics").toString(), etag = null, lastModified = null)

        assertTrue(result is IcsCalendarClient.FetchResult.Failure)
    }

    @Test
    fun `reports server errors`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = client.fetch(server.url("/cal.ics").toString(), etag = null, lastModified = null)

        assertTrue(result is IcsCalendarClient.FetchResult.Failure)
    }

    @Test
    fun `refuses plain http urls without a request`() {
        val result = client.fetch("http://example.com/cal.ics", etag = null, lastModified = null)

        assertTrue(result is IcsCalendarClient.FetchResult.Failure)
        assertEquals(0, server.requestCount)
    }
}
