package com.stationlive.iptv4k

import java.net.HttpURLConnection
import java.net.URL

object XmlTvRepository {
    fun load(rawUrl: String): List<EpgProgram> {
        val parsed = URL(rawUrl.trim())
        require(parsed.protocol.equals("https", true)) { "XMLTV EPG links must use HTTPS." }
        val connection = (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 45_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/xml, text/xml, */*")
        }
        return try {
            val code = connection.responseCode
            require(code in 200..299) { "EPG server returned HTTP $code." }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(body.contains("<tv", true)) { "The URL did not return an XMLTV document." }
            XmlTvParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }
}
