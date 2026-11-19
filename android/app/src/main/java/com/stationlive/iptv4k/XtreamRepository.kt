package com.stationlive.iptv4k

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct Xtream Codes client for the configured provider.
 * HTTP is intentionally limited by network_security_config to the provider host.
 */
internal object XtreamRepository {
    fun loadLiveChannels(username: String, password: String): List<M3uEntry> {
        require(username.isNotBlank() && password.isNotBlank()) { "Enter your username and password." }
        val loginUrl = Uri.parse("${ProviderConfig.BASE_URL}/player_api.php")
            .buildUpon()
            .appendQueryParameter("username", username)
            .appendQueryParameter("password", password)
            .build().toString()

        val account = JSONObject(readText(loginUrl))
        val userInfo = account.optJSONObject("user_info")
            ?: throw IllegalStateException("The server returned an invalid login response.")
        if (userInfo.optInt("auth", 0) != 1) {
            throw IllegalStateException("Login failed. Check your username and password.")
        }
        val status = userInfo.optString("status", "")
        if (status.isNotBlank() && !status.equals("Active", true)) {
            throw IllegalStateException("This subscription is not active: $status")
        }

        val streamsUrl = Uri.parse("${ProviderConfig.BASE_URL}/player_api.php")
            .buildUpon()
            .appendQueryParameter("username", username)
            .appendQueryParameter("password", password)
            .appendQueryParameter("action", "get_live_streams")
            .build().toString()
        val streams = JSONArray(readText(streamsUrl))
        val result = ArrayList<M3uEntry>(streams.length())
        for (i in 0 until streams.length()) {
            val item = streams.optJSONObject(i) ?: continue
            val id = item.optString("stream_id").takeIf { it.isNotBlank() } ?: continue
            val name = item.optString("name").ifBlank { "Channel $id" }
            val extension = item.optString("container_extension").ifBlank { "ts" }
            val streamUrl = Uri.parse("${ProviderConfig.BASE_URL}/live")
                .buildUpon()
                .appendPath(username)
                .appendPath(password)
                .appendPath("$id.$extension")
                .build().toString()
            result += M3uEntry(
                title = name,
                url = streamUrl,
                group = item.optString("category_name").takeIf { it.isNotBlank() && it != "null" },
                logo = item.optString("stream_icon").takeIf { it.startsWith("http", true) }
            )
        }
        return result
    }

    private fun readText(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Server error ($code).")
            if (body.isBlank()) throw IllegalStateException("The server returned an empty response.")
            return body
        } finally {
            connection.disconnect()
        }
    }
}
