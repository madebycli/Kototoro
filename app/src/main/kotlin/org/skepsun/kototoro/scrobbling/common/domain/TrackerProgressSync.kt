package org.skepsun.kototoro.scrobbling.common.domain

import org.skepsun.kototoro.core.prefs.AppSettings
import org.skepsun.kototoro.core.ui.util.ReversibleHandle
import org.skepsun.kototoro.core.util.ext.printStackTraceDebug
import org.skepsun.kototoro.details.domain.calculateGroupedChapterProgress
import org.skepsun.kototoro.history.data.HistoryRepository
import org.skepsun.kototoro.history.domain.ChapterOrder
import org.skepsun.kototoro.history.domain.chapterIsAfter
import org.skepsun.kototoro.history.domain.chapterReadThrough
import org.skepsun.kototoro.parsers.model.Content
import org.skepsun.kototoro.parsers.model.ContentChapter
import org.skepsun.kototoro.parsers.util.findById
import org.skepsun.kototoro.parsers.util.runCatchingCancellable
import org.skepsun.kototoro.reader.ui.FULLY_READ_CHAPTER_ID
import org.skepsun.kototoro.tracking.domain.TrackingChapter
import org.skepsun.kototoro.tracking.domain.chapterCoveredByTrackerProgress
import org.skepsun.kototoro.tracking.domain.resolveAbsoluteChapterNumber
import javax.inject.Inject

/**
 * Keeps a title's local read state and one linked tracker entry in step, the way Mihon-style readers do: whichever
 * side has read further wins. Chapters the tracker counts as read are marked read here, through the same history
 * write the reader makes, and chapters read here are reported to the tracker. Neither side ever moves back.
 */
class TrackerProgressSync @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settings: AppSettings,
) {

    enum class Result { UNCHANGED, PULLED, PUSHED }

    /** Which side a sync may change. */
    enum class Direction {
        BOTH,

        /** Only the local read state; used after the user set the tracker's progress by hand, even lower. */
        PULL_ONLY,
    }

    /** What a sync did; [undo] restores the local read state a pull replaced. */
    class Outcome(val result: Result, val undo: ReversibleHandle? = null)

    /**
     * Reconciles [manga] with [scrobbler]'s entry. [manga] must be the stored title the entry is bound to, with its
     * chapters. With [refresh] the entry is read from the service first; [preferredBranch] picks the translation to
     * mark read when nothing has been read locally yet.
     */
    suspend fun sync(
        scrobbler: Scrobbler,
        manga: Content,
        preferredBranch: String? = null,
        refresh: Boolean = true,
        direction: Direction = Direction.BOTH,
    ): Outcome {
        if (!settings.isTrackingProgressSyncEnabled) return NO_CHANGE
        val chapters = manga.chapters?.takeIf { it.isNotEmpty() } ?: return NO_CHANGE
        if (refresh) {
            runCatchingCancellable { scrobbler.refreshRemoteEntry(manga.id) }.onFailure { it.printStackTraceDebug() }
        }
        val info = scrobbler.getScrobblingInfoOrNull(manga.id) ?: return NO_CHANGE
        val trackingChapters = chapters.map { it.toTrackingChapter() }
        val readThrough = localReadThrough(manga, chapters, preferredBranch)
        val localNumber = readThrough?.let { resolveAbsoluteChapterNumber(trackingChapters, it.toTrackingChapter()) } ?: 0
        return when {
            info.chapter > localNumber -> {
                val branch = readThrough?.branch ?: chapters.preferredBranch(preferredBranch)
                val target = chapterCoveredByTrackerProgress(trackingChapters, branch, info.chapter)
                    ?.let { chapters.findById(it.id) }
                    ?: return NO_CHANGE
                if (resolveAbsoluteChapterNumber(trackingChapters, target.toTrackingChapter()) <= localNumber) {
                    return NO_CHANGE
                }
                val progress = calculateGroupedChapterProgress(chapters, target.id, chapterPercent = 1f)
                    ?: return NO_CHANGE
                val undo = historyRepository.undoHandleFor(manga.id)
                // The same write the reader makes, so the chapter list greys out everything up to the target.
                historyRepository.addOrUpdate(
                    manga = manga,
                    chapterId = target.id,
                    page = 0,
                    scroll = 0,
                    percent = progress.percent,
                    force = true,
                )
                Outcome(Result.PULLED, undo)
            }

            direction == Direction.BOTH && readThrough != null && localNumber > info.chapter -> {
                scrobbler.scrobble(manga, readThrough.id)
                Outcome(Result.PUSHED)
            }

            else -> NO_CHANGE
        }
    }

    /** The chapter up to which [manga] counts as read here, as the chapter list shows it; null when none is. */
    private suspend fun localReadThrough(
        manga: Content,
        chapters: List<ContentChapter>,
        preferredBranch: String?,
    ): ContentChapter? {
        val history = historyRepository.getOne(manga) ?: return null
        val furthest = history.furthestChapterId.takeIf { it != 0L }?.let { chapters.findById(it) }
        if (history.chapterId == FULLY_READ_CHAPTER_ID) {
            val branch = furthest?.branch ?: chapters.preferredBranch(preferredBranch)
            return chapters.filter { it.branch == branch }.reduceOrNull { latest, chapter ->
                if (chapter.toOrder().isAfter(latest.toOrder())) chapter else latest
            }
        }
        val current = chapters.findById(history.chapterId)
        val readThrough = chapterReadThrough(current?.toOrder(), furthest?.toOrder()) ?: return null
        return chapters.findById(readThrough.id)
    }

    private companion object {
        val NO_CHANGE = Outcome(Result.UNCHANGED)
    }

    private fun List<ContentChapter>.preferredBranch(preferred: String?): String? =
        preferred?.takeIf { branch -> any { it.branch == branch } }
            ?: groupingBy { it.branch }.eachCount().maxByOrNull { it.value }?.key

    private fun ContentChapter.toTrackingChapter() = TrackingChapter(id, number, branch, title, url)

    private fun ContentChapter.toOrder() = ChapterOrder(id, number, volume)

    private fun ChapterOrder.isAfter(other: ChapterOrder) = chapterIsAfter(number, volume, other.number, other.volume)
}
