package org.skepsun.kototoro.scrobbling.common.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.core.model.ContentHistory
import org.skepsun.kototoro.core.model.TestContentSource
import org.skepsun.kototoro.core.prefs.AppSettings
import org.skepsun.kototoro.history.data.HistoryRepository
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.parsers.model.ContentChapter
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerService
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblingInfo
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblingStatus
import java.time.Instant

class TrackerProgressSyncTest {

    private val chapters = (1L..10L).map { id ->
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
        chapters = chapters,
    )
    private val history = mockk<HistoryRepository>(relaxed = true)
    private val settings = mockk<AppSettings> { every { isTrackingProgressSyncEnabled } returns true }
    private val scrobbler = mockk<Scrobbler>(relaxed = true)
    private val sync = TrackerProgressSync(history, settings)

    @Test
    fun `remote progress ahead marks the local chapters read up to it`() = runBlocking {
        remote(chapter = 7)
        local(chapterId = 3)

        assertEquals(TrackerProgressSync.Result.PULLED, sync.sync(scrobbler, manga))

        coVerify { scrobbler.refreshRemoteEntry(55) }
        coVerify {
            history.addOrUpdate(manga, 7, 0, 0, 0.7f, true, any(), any())
        }
        coVerify(exactly = 0) { scrobbler.scrobble(any(), any()) }
    }

    @Test
    fun `local progress ahead is sent to the tracker using the furthest read chapter`() = runBlocking {
        remote(chapter = 2)
        // Reopened chapter 4 after reading up to 9: chapters up to 9 still count as read.
        local(chapterId = 4, furthestChapterId = 9)

        assertEquals(TrackerProgressSync.Result.PUSHED, sync.sync(scrobbler, manga, refresh = false))

        coVerify { scrobbler.scrobble(manga, 9) }
        coVerify(exactly = 0) { scrobbler.refreshRemoteEntry(any()) }
        coVerify(exactly = 0) { history.addOrUpdate(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `nothing read locally takes the whole remote progress`() = runBlocking {
        remote(chapter = 25)
        coEvery { history.getOne(manga) } returns null

        assertEquals(TrackerProgressSync.Result.PULLED, sync.sync(scrobbler, manga))

        coVerify { history.addOrUpdate(manga, 10, 0, 0, 1f, true, any(), any()) }
    }

    @Test
    fun `equal progress and a disabled setting change nothing`() = runBlocking {
        remote(chapter = 5)
        local(chapterId = 5)
        assertEquals(TrackerProgressSync.Result.UNCHANGED, sync.sync(scrobbler, manga))

        every { settings.isTrackingProgressSyncEnabled } returns false
        remote(chapter = 9)
        assertEquals(TrackerProgressSync.Result.UNCHANGED, sync.sync(scrobbler, manga))

        coVerify(exactly = 0) { history.addOrUpdate(any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { scrobbler.scrobble(any(), any()) }
    }

    private fun remote(chapter: Int) {
        coEvery { scrobbler.getScrobblingInfoOrNull(55) } returns ScrobblingInfo(
            scrobbler = ScrobblerService.ANILIST,
            mangaId = 55,
            targetId = 377,
            status = ScrobblingStatus.READING,
            chapter = chapter,
            comment = null,
            rating = 0f,
            title = "Title",
            coverUrl = "",
            description = null,
            externalUrl = "",
        )
    }

    private fun local(chapterId: Long, furthestChapterId: Long = 0L) {
        coEvery { history.getOne(manga) } returns ContentHistory(
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            chapterId = chapterId,
            page = 0,
            scroll = 0,
            percent = 0f,
            chaptersCount = chapters.size,
            furthestChapterId = furthestChapterId,
        )
    }
}
