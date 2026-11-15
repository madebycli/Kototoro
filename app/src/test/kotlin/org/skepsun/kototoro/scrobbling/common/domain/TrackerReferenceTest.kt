package org.skepsun.kototoro.scrobbling.common.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerService

class TrackerReferenceTest {

    @Test
    fun `id prefixes are entry ids and bare numbers stay titles`() {
        assertEquals(30013L, parseTrackerReference(" id: 30013 ", ScrobblerService.ANILIST))
        assertEquals(13L, parseTrackerReference("ID:13", ScrobblerService.MAL))
        assertNull(parseTrackerReference("id:0", ScrobblerService.ANILIST))
        assertNull(parseTrackerReference("86", ScrobblerService.ANILIST))
    }

    @Test
    fun `links to the searched tracker give their entry id`() {
        assertEquals(30013L, parseTrackerReference("https://anilist.co/manga/30013/One-Piece/", ScrobblerService.ANILIST))
        assertEquals(13L, parseTrackerReference("https://myanimelist.net/manga/13/One_Piece?q=1", ScrobblerService.MAL))
        assertEquals(1234L, parseTrackerReference("https://shikimori.one/mangas/z1234-one-piece", ScrobblerService.SHIKIMORI))
        assertEquals(975L, parseTrackerReference("https://bgm.tv/subject/975", ScrobblerService.BANGUMI))
    }

    @Test
    fun `titles and other trackers links are searched as text`() {
        assertNull(parseTrackerReference("SSS-Class Revival Hunter", ScrobblerService.ANILIST))
        assertNull(parseTrackerReference("86 Eighty-Six", ScrobblerService.ANILIST))
        assertNull(parseTrackerReference("https://myanimelist.net/manga/13", ScrobblerService.ANILIST))
        assertNull(parseTrackerReference("https://anilist.co/search/manga", ScrobblerService.ANILIST))
    }
}
