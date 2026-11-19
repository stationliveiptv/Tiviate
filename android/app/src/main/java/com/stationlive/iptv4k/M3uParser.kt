package com.stationlive.iptv4k

/**
 * A single playable entry from an M3U/M3U8 playlist.
 * Parsing is intentionally independent from networking and UI.
 */
data class M3uEntry(
    val title: String,
    val url: String,
    val group: String? = null,
    val logo: String? = null,
    val tvgId: String? = null,
    val durationSeconds: Double? = null
)

object M3uParser {
    private val attributeRegex = Regex("""([\w-]+)\s*=\s*"([^"]*)""")

    fun parse(content: String): List<M3uEntry> {
        val entries = mutableListOf<M3uEntry>()
        var pending: PendingEntry? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pending = parseExtInf(line)
                }
                line.startsWith("#") -> Unit
                pending != null -> {
                    val item = pending!!
                    entries += M3uEntry(
                        title = item.title.ifBlank { line },
                        url = line,
                        group = item.group,
                        logo = item.logo,
                        tvgId = item.tvgId,
                        durationSeconds = item.durationSeconds
                    )
                    pending = null
                }
            }
        }
        return entries
    }

    private fun parseExtInf(line: String): PendingEntry {
        val comma = line.indexOf(',')
        val metadata = if (comma >= 0) line.substring(0, comma) else line
        val title = if (comma >= 0) line.substring(comma + 1).trim() else ""
        val attributes = attributeRegex.findAll(metadata)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }

        val durationText = metadata.substringAfter("#EXTINF:", "")
            .substringBefore(',')
            .trim()

        return PendingEntry(
            title = title,
            group = attributes["group-title"]?.takeIf(String::isNotBlank),
            logo = attributes["tvg-logo"]?.takeIf(String::isNotBlank),
            tvgId = attributes["tvg-id"]?.takeIf(String::isNotBlank),
            durationSeconds = durationText.toDoubleOrNull()
        )
    }

    private data class PendingEntry(
        val title: String,
        val group: String?,
        val logo: String?,
        val tvgId: String?,
        val durationSeconds: Double?
    )
}
