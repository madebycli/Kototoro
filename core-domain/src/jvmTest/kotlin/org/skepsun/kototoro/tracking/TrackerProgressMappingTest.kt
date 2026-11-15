package org.skepsun.kototoro.tracking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.tracking.domain.TrackingChapter
import org.skepsun.kototoro.tracking.domain.chapterCoveredByTrackerProgress
import org.skepsun.kototoro.tracking.domain.resolveAbsoluteChapterNumber
import org.skepsun.kototoro.tracking.domain.resolveAbsoluteChapterNumbers

class TrackerProgressMappingTest {

    private val seasons = (1..12).map { TrackingChapter(it.toLong(), it.toFloat(), "A", "S1 第 $it 集", "/s1/$it") } +
        (1..3).map { TrackingChapter(100L + it, it.toFloat(), "A", "S2 第 $it 集", "/s2/$it") } +
        TrackingChapter(900, 999f, "B", "另一分支", "/b")

    @Test
    fun `bulk numbers match the per chapter resolver`() {
        val lists = listOf(
            seasons,
            (1..12).map { TrackingChapter(it.toLong(), it.toFloat(), null, null, "/$it") }.reversed(),
            (1..3).map { TrackingChapter(it.toLong(), 0f, null, null, "/$it") },
            listOf(0.5f, 1f, 1.5f, 2f).mapIndexed { i, n -> TrackingChapter(i.toLong(), n, null, null, "/$i") },
        )
        for (chapters in lists) {
            for (branch in chapters.map { it.branch }.distinct()) {
                val bulk = resolveAbsoluteChapterNumbers(chapters, branch)
                for (chapter in chapters.filter { it.branch == branch }) {
                    assertEquals(resolveAbsoluteChapterNumber(chapters, chapter), bulk[chapter.id], "$chapter")
                }
            }
        }
    }

    @Test
    fun `tracker progress covers chapters up to its number in reading order`() {
        assertEquals(8L, chapterCoveredByTrackerProgress(seasons, "A", 8)?.id)
        // Season two continues the count: 14 is its second chapter.
        assertEquals(102L, chapterCoveredByTrackerProgress(seasons, "A", 14)?.id)
        // More than the source has still covers everything it has.
        assertEquals(103L, chapterCoveredByTrackerProgress(seasons, "A", 500)?.id)
        assertNull(chapterCoveredByTrackerProgress(seasons, "A", 0))
        assertNull(chapterCoveredByTrackerProgress(seasons, "missing", 5))
    }

    @Test
    fun `newest first lists and split chapters resolve to the last covered chapter`() {
        val newestFirst = (1..10).map { TrackingChapter(it.toLong(), it.toFloat(), null, null, "/$it") }.reversed()
        assertEquals(6L, chapterCoveredByTrackerProgress(newestFirst, null, 6)?.id)
        val split = listOf(1f, 2f, 2.5f, 3f).mapIndexed { i, n -> TrackingChapter(i.toLong(), n, null, null, "/$i") }
        // 2 and 2.5 both count as chapter 2 on a tracker; the later part is the one read through.
        assertEquals(2L, chapterCoveredByTrackerProgress(split, null, 2)?.id)
    }
}
