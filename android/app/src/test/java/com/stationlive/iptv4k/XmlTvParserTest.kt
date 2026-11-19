

class XmlTvParserTest {
    @org.junit.Test
    fun parsesProgrammeAndDecodesEntities() {
        val xml = """
            <tv>
              <programme start="20261119180000 +0000" stop="20261119190000 +0000" channel="news.one">
                <title>World &amp; Local</title>
                <desc>Top &lt;stories&gt;</desc>
              </programme>
            </tv>
        """.trimIndent()

        val programmes = XmlTvParser.parse(xml)

        org.junit.Assert.assertEquals(1, programmes.size)
        org.junit.Assert.assertEquals("news.one", programmes.single().channelId)
        org.junit.Assert.assertEquals("World & Local", programmes.single().title)
        org.junit.Assert.assertEquals("Top <stories>", programmes.single().description)
        org.junit.Assert.assertNotNull(programmes.single().startUtc)
        org.junit.Assert.assertNotNull(programmes.single().stopUtc)
    }

    @org.junit.Test
    fun ignoresProgrammeWithoutChannelOrTitle() {
        val xml = """
            <tv>
              <programme start="20261119180000 +0000"><title>No channel</title></programme>
              <programme channel="news.one"><desc>No title</desc></programme>
            </tv>
        """.trimIndent()

        org.junit.Assert.assertTrue(XmlTvParser.parse(xml).isEmpty())
    }
}
