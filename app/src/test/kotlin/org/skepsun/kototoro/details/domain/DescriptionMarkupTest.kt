package org.skepsun.kototoro.details.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DescriptionMarkupTest {

    @Test
    fun `plain text and markdown skip the html parser`() {
        assertNull(descriptionHtmlSource("**Year:** 2024\n[AniList](https://anilist.co/manga/1)\n\n- One\n- Two"))
        assertNull(descriptionHtmlSource("I <3 this, and 1 < 2 > 0"))
    }

    @Test
    fun `inline html keeps the source line breaks`() {
        assertEquals(
            "<b>Year:</b> 2024<br>Tom &amp; Jerry<br><br>Last",
            descriptionHtmlSource("<b>Year:</b> 2024\r\nTom &amp; Jerry\n\rLast"),
        )
        assertEquals("Tom &amp; Jerry<br>**Ongoing**", descriptionHtmlSource("Tom &amp; Jerry\n**Ongoing**"))
    }

    @Test
    fun `html that lays out its own lines is left untouched`() {
        val paragraphs = "<p>First</p>\n<p>Second</p>"
        val breaks = "**Year:** 2024<br>\n[AniList](https://anilist.co)"
        val list = "<UL>\n<li>One</li>\n</UL>"
        assertEquals(paragraphs, descriptionHtmlSource(paragraphs))
        assertEquals(breaks, descriptionHtmlSource(breaks))
        assertEquals(list, descriptionHtmlSource(list))
    }
}
