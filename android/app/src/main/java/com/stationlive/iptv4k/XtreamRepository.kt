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
    data class Catalog(
        val live: List<M3uEntry>,
        val movies: List<M3uEntry>,
        val series: List<M3uEntry>
    )

    fun loadLiveChannels(username: String, password: String): List<M3uEntry> = loadCatalog(username, password).live

    fun loadCatalog(username: String, password: String): Catalog {
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
        val categoryUrl = Uri.parse("${ProviderConfig.BASE_URL}/player_api.php")
            .buildUpon().appendQueryParameter("username", username)
            .appendQueryParameter("password", password)
            .appendQueryParameter("action", "get_live_categories").build().toString()
        val categories = JSONArray(readText(categoryUrl))
        val categoryNames = buildMap<String, String> {
            for (i in 0 until categories.length()) {
                val category = categories.optJSONObject(i) ?: continue
                val id = category.optString("category_id")
                val name = category.optString("category_name")
                if (id.isNotBlank() && name.isNotBlank()) put(id, name)
            }
        }
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
                group = categoryNames[item.optString("category_id")] ?: item.optString("category_name").takeIf { it.isNotBlank() && it != "null" },
                logo = item.optString("stream_icon").takeIf { it.startsWith("http", true) }
            )
        }
        return Catalog(
            live = result,
            movies = loadVod(username, password, "get_vod_streams", "movie"),
            series = loadVod(username, password, "get_series", "series")
        )
    }

    private fun loadVod(username: String, password: String, action: String, kind: String): List<M3uEntry> {
        val endpoint = Uri.parse("${ProviderConfig.BASE_URL}/player_api.php")
            .buildUpon()
            .appendQueryParameter("username", username)
            .appendQueryParameter("password", password)
            .appendQueryParameter("action", action)
            .build().toString()
        val array = JSONArray(readText(endpoint))
        val result = ArrayList<M3uEntry>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString(if (kind == "movie") "stream_id" else "series_id")
                .takeIf { it.isNotBlank() } ?: continue
            val title = item.optString("name").ifBlank { if (kind == "movie") "Movie $id" else "Series $id" }
            val extension = item.optString("container_extension").ifBlank { "mp4" }
            val url = if (kind == "movie") {
                Uri.parse("${ProviderConfig.BASE_URL}/movie").buildUpon()
                    .appendPath(username).appendPath(password).appendPath("$id.$extension").build().toString()
            } else {
                "series:$id"
            }
            result += M3uEntry(
                title = title,
                url = url,
                group = item.optString("category_name").takeIf { it.isNotBlank() && it != "null" },
                logo = item.optString("stream_icon").takeIf { it.startsWith("http", true) }
                    ?: item.optString("cover").takeIf { it.startsWith("http", true) }
            )
        }
        return result
    }

    fun loadSeriesEpisodes(username: String, password: String, seriesId: String): List<M3uEntry> {
        val endpoint = Uri.parse("${ProviderConfig.BASE_URL}/player_api.php")
            .buildUpon().appendQueryParameter("username", username)
            .appendQueryParameter("password", password)
            .appendQueryParameter("action", "get_series_info")
            .appendQueryParameter("series_id", seriesId).build().toString()
        val info = JSONObject(readText(endpoint))
        val episodes = info.optJSONObject("episodes") ?: return emptyList()
        val result = mutableListOf<M3uEntry>()
        val seasons = episodes.keys()
        while (seasons.hasNext()) {
            val seasonName = seasons.next()
            val season = episodes.optJSONArray(seasonName) ?: continue
            for (i in 0 until season.length()) {
                val episode = season.optJSONObject(i) ?: continue
                val id = episode.optString("id").takeIf { it.isNotBlank() } ?: continue
                val ext = episode.optString("container_extension").ifBlank { "mp4" }
                val title = episode.optString("title").ifBlank { "Episode $id" }
                val url = Uri.parse("${ProviderConfig.BASE_URL}/series").buildUpon()
                    .appendPath(username).appendPath(password).appendPath("$id.$ext").build().toString()
                result += M3uEntry(title, url, "Season $seasonName")
            }
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
