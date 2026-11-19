package com.stationlive.iptv4k

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesMetadataAndStreamUrl() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news.id" tvg-logo="https://example.com/logo.png" group-title="News",World News
            #EXTVLCOPT:http-referrer=https://example.com/
            https://stream.example.com/live.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("World News", entries[0].title)
        assertEquals("https://stream.example.com/live.m3u8", entries[0].url)
        assertEquals("News", entries[0].group)
        assertEquals("https://example.com/logo.png", entries[0].logo)
        assertEquals("news.id", entries[0].tvgId)
        assertEquals(-1.0, entries[0].durationSeconds!!, 0.0)
    }

    @Test
    fun ignoresCommentsAndIncompleteEntries() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,First
            https://example.com/1.m3u8
            #EXTINF:-1,No URL
            #EXTVLCOPT:http-user-agent=Player
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("First", entries.single().title)
        assertNull(entries.single().group)
    }
}
