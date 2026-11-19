package com.stationlive.iptv4k

import java.net.HttpURLConnection
import java.net.URL

object M3uRepository {
    fun loadFromUrl(rawUrl: String): List<M3uEntry> {
        val parsed = URL(rawUrl.trim())
        require(parsed.protocol.equals("https", ignoreCase = true)) {
            "For security, playlist links must use HTTPS."
        }
        val connection = (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.apple.mpegurl, audio/x-mpegurl, text/plain, */*")
            instanceFollowRedirects = true
        }
        return try {
            val code = connection.responseCode
            require(code in 200..299) { "Playlist server returned HTTP $code." }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(body.lineSequence().any { it.trim().startsWith("#EXTM3U", ignoreCase = true) }) {
                "The URL did not return a valid M3U playlist."
            }
            M3uParser.parse(body)
        } finally {
            connection.disconnect()
        }
    }
}
