package org.skepsun.kototoro.scrobbling.common.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.core.db.MangaDatabase
import org.skepsun.kototoro.core.model.TestContentSource
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.parsers.model.ContentChapter
import org.skepsun.kototoro.scrobbling.common.data.ScrobblerRepository
import org.skepsun.kototoro.scrobbling.common.data.ScrobblingDao
import org.skepsun.kototoro.scrobbling.common.data.ScrobblingEntity
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerService
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblingStatus

class ScrobblerProgressTest {

    private val dao = mockk<ScrobblingDao>(relaxed = true)
    private val db = mockk<MangaDatabase> { every { getScrobblingDao() } returns dao }
    private val repository = mockk<ScrobblerRepository>(relaxed = true)
    private val statusUpdates = ArrayList<ScrobblingStatus?>()
    private val scrobbler = object : Scrobbler(db, ScrobblerService.ANILIST, repository, mockk()) {
        init {
            statuses[ScrobblingStatus.PLANNED] = "PLANNING"
            statuses[ScrobblingStatus.READING] = "CURRENT"
            statuses[ScrobblingStatus.COMPLETED] = "COMPLETED"
        }

        override suspend fun updateScrobblingInfo(
            mangaId: Long,
            rating: Float,
            status: ScrobblingStatus?,
            comment: String?,
        ) {
            statusUpdates += status
        }
    }
    private val manga = Content(
        id = 55,
        title = "Title",
        altTitles = emptySet(),
        url = "/title",
        publicUrl = "https://example.test/title",
        rating = 0f,
        contentRating = null,
        coverUrl = null,
        tags = emptySet(),
        state = null,
        authors = emptySet(),
        source = TestContentSource,
        chapters = (1L..10L).map { id ->
            ContentChapter(
                id = id,
                title = "Chapter $id",
                number = id.toFloat(),
                volume = 0,
                url = "/chapter/$id",
                scanlator = null,
                uploadDate = 0L,
                branch = null,
                source = TestContentSource,
            )
        },
    )

    @Test
    fun `rereading an earlier chapter never lowers the tracker progress`() = runBlocking {
        bound(status = "CURRENT", chapter = 8)

        scrobbler.scrobble(manga, 5)
        scrobbler.scrobble(manga, 8)

        coVerify(exactly = 0) { repository.updateRate(any(), any(), any<Int>()) }
    }

    @Test
    fun `reading further reports the chapter and starts a planned title`() = runBlocking {
        bound(status = "PLANNING", chapter = 3)

        scrobbler.scrobble(manga, 6)

        coVerify { repository.updateRate(91, 55, 6) }
        assertEquals(listOf(ScrobblingStatus.READING), statusUpdates)
    }

    @Test
    fun `a finished entry keeps its status while progress goes up`() = runBlocking {
        bound(status = "COMPLETED", chapter = 3)

        scrobbler.scrobble(manga, 6)

        coVerify { repository.updateRate(91, 55, 6) }
        assertTrue(statusUpdates.isEmpty())
    }

    private fun bound(status: String, chapter: Int) {
        coEvery { dao.findByLocalManga(ScrobblerService.ANILIST.id, 55) } returns ScrobblingEntity(
            scrobbler = ScrobblerService.ANILIST.id,
            id = 91,
            mangaId = 55,
            targetId = 377,
            status = status,
            chapter = chapter,
            comment = null,
            rating = 0f,
        )
    }
}
