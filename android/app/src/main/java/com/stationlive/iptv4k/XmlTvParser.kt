package com.stationlive.iptv4k

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String?,
    val startUtc: Instant?,
    val stopUtc: Instant?
)

/** Minimal XMLTV parser for EPG documents supplied by an authorized provider. */
object XmlTvParser {
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT)

    fun parse(xml: String): List<EpgProgram> {
        val result = mutableListOf<EpgProgram>()
        val programmeRegex = Regex("(?is)<programme\\b([^>]*)>(.*?)</programme>")
        val attrRegex = Regex("""([\w-]+)\s*=\s*["']([^"']*)["']""")
        val tagRegex = Regex("(?is)<(title|desc)\\b[^>]*>(.*?)</\\1>")

        programmeRegex.findAll(xml).forEach { match ->
            val attrs = attrRegex.findAll(match.groupValues[1])
                .associate { it.groupValues[1].lowercase(Locale.ROOT) to it.groupValues[2] }
            val tags = tagRegex.findAll(match.groupValues[2])
                .associate { it.groupValues[1].lowercase(Locale.ROOT) to decode(it.groupValues[2]) }
            val channel = attrs["channel"] ?: return@forEach
            val title = tags["title"]?.takeIf(String::isNotBlank) ?: return@forEach
            result += EpgProgram(
                channelId = channel,
                title = title,
                description = tags["desc"]?.takeIf(String::isNotBlank),
                startUtc = parseTime(attrs["start"]),
                stopUtc = parseTime(attrs["stop"])
            )
        }
        return result
    }

    private fun parseTime(value: String?): Instant? {
        if (value == null || value.length < 14) return null
        return runCatching {
            LocalDateTime.parse(value.take(14), timestampFormat).toInstant(ZoneOffset.UTC)
        }.getOrNull()
    }

    private fun decode(value: String): String = value
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")
        .replace("&amp;", "&").trim()
}
