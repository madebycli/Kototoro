package org.skepsun.kototoro.core.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DescriptionAnnotatedStringTest {

    @Test
    fun `an extension description reads as formatted text with its line breaks`() {
        val text = buildDescriptionAnnotatedString(
            "**Year:** 2024\n**Status:** Ongoing\n[AniList](https://anilist.co/manga/1)\n\n- One\n- Two\n\n---\n\nEnd",
            Color.Blue,
        )

        assertEquals("Year: 2024\nStatus: Ongoing\nAniList\n\n• One\n• Two\n\n────────\n\nEnd", text.text)
        val bold = text.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }
            .map { text.text.substring(it.start, it.end) }
        assertEquals(listOf("Year:", "Status:"), bold)
    }

    @Test
    fun `only web urls become links`() {
        val text = buildDescriptionAnnotatedString(
            "[Site](https://example.org) [App](intent://open) see https://example.org/a.",
            Color.Blue,
        )

        assertEquals("Site App see https://example.org/a.", text.text)
        val links = text.getLinkAnnotations(0, text.length)
            .map { (it.item as LinkAnnotation.Url).url to text.text.substring(it.start, it.end) }
        assertEquals(
            listOf("https://example.org" to "Site", "https://example.org/a" to "https://example.org/a"),
            links,
        )
    }
}
