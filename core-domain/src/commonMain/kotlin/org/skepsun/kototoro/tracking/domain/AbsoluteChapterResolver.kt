package org.skepsun.kototoro.tracking.domain

data class TrackingChapter(
    val id: Long,
    val number: Float,
    val branch: String?,
    val title: String?,
    val url: String,
)

fun resolveAbsoluteChapterNumber(
    chapters: List<TrackingChapter>,
    targetChapter: TrackingChapter
): Int {
    val branchChapters = chapters.filter { it.branch == targetChapter.branch }
    if (branchChapters.isEmpty()) return 1
    val absolute = absoluteNumbersInReadingOrder(branchChapters)
        .firstOrNull { (chapter, _) -> chapter.id == targetChapter.id }
        ?.second
        ?.toInt()
        ?: -1
    return absolute.takeIf { it > 0 } ?: fallbackChapterNumber(branchChapters, targetChapter)
}

/**
 * The tracker number of every chapter in [branch], as [resolveAbsoluteChapterNumber] would give it one by one, in a
 * single pass. Keyed by chapter id; the first chapter wins when a source repeats an id.
 */
fun resolveAbsoluteChapterNumbers(chapters: List<TrackingChapter>, branch: String?): Map<Long, Int> {
    val branchChapters = chapters.filter { it.branch == branch }
    if (branchChapters.isEmpty()) return emptyMap()
    val result = LinkedHashMap<Long, Int>(branchChapters.size)
    for ((chapter, absolute) in absoluteNumbersInReadingOrder(branchChapters)) {
        if (chapter.id in result) continue
        result[chapter.id] = absolute.toInt().takeIf { it > 0 } ?: fallbackChapterNumber(branchChapters, chapter)
    }
    return result
}

/**
 * The last chapter of [branch], in reading order, that a tracker progress of [progress] chapters covers: the one with
 * the highest tracker number not above [progress]. `null` when [progress] covers none of them.
 */
fun chapterCoveredByTrackerProgress(chapters: List<TrackingChapter>, branch: String?, progress: Int): TrackingChapter? {
    if (progress <= 0) return null
    val numbers = resolveAbsoluteChapterNumbers(chapters, branch)
    var covered: TrackingChapter? = null
    var coveredNumber = 0
    for ((chapter, _) in absoluteNumbersInReadingOrder(chapters.filter { it.branch == branch })) {
        val number = numbers[chapter.id] ?: continue
        if (number in coveredNumber..progress) {
            covered = chapter
            coveredNumber = number
        }
    }
    return covered
}

private fun fallbackChapterNumber(branchChapters: List<TrackingChapter>, chapter: TrackingChapter): Int =
    if (chapter.number > 0f) chapter.number.toInt() else branchChapters.indexOf(chapter) + 1

/** Each chapter of one branch with its season-adjusted number, oldest first. */
private fun absoluteNumbersInReadingOrder(branchChapters: List<TrackingChapter>): List<Pair<TrackingChapter, Float>> {
    // Detect chronology before accumulating season offsets.
    var ascendingCount = 0
    var descendingCount = 0
    var prevNumForDir: Float? = null
    for (ch in branchChapters) {
        val num = ch.number
        if (num > 0f) {
            if (prevNumForDir != null) {
                if (num > prevNumForDir) {
                    ascendingCount++
                } else if (num < prevNumForDir) {
                    descendingCount++
                }
            }
            prevNumForDir = num
        }
    }
    val isNewestFirst = descendingCount > ascendingCount
    val chronologicalChapters = if (isNewestFirst) branchChapters.reversed() else branchChapters

    // Numbering can restart at a season boundary.
    var seasonOffset = 0f
    var prevValidNumber = 0f
    val result = ArrayList<Pair<TrackingChapter, Float>>(chronologicalChapters.size)

    for (i in chronologicalChapters.indices) {
        val ch = chronologicalChapters[i]
        val chNum = if (ch.number > 0f) ch.number else (prevValidNumber + 1f)

        if (i > 0) {
            val prevCh = chronologicalChapters[i - 1]
            val prevChNum = if (prevCh.number > 0f) prevCh.number else prevValidNumber

            val prevSeason = extractSeason(prevCh.title, prevCh.url)
            val currSeason = extractSeason(ch.title, ch.url)

            val hasExplicitSeasonChange = currSeason != null && prevSeason != null && currSeason > prevSeason && chNum <= prevChNum
            val hasNumberReset = chNum < prevChNum && chNum <= 5f && (
                i == chronologicalChapters.size - 1 ||
                (i + 1 < chronologicalChapters.size &&
                    (chronologicalChapters[i + 1].number.takeIf { it > 0f } ?: (chNum + 1f)) <= prevChNum)
            )

            if (hasExplicitSeasonChange || hasNumberReset) {
                seasonOffset += prevChNum
            }
        }

        result += ch to (chNum + seasonOffset)
        prevValidNumber = if (ch.number > 0f) ch.number else chNum
    }
    return result
}

private val SEASON_PATTERNS = listOf(
    Regex("""\bS(?:eason)?\s*(\d+)\b""", RegexOption.IGNORE_CASE),
    Regex("""\bPart\s*(\d+)\b""", RegexOption.IGNORE_CASE),
    Regex("""/s(\d+)(?:[/-]|$)""", RegexOption.IGNORE_CASE),
    Regex("""[/-]s(\d+)(?:[/-]|$)""", RegexOption.IGNORE_CASE)
)

private fun extractSeason(title: String?, url: String?): Int? {
    title?.let { t ->
        for (pattern in SEASON_PATTERNS) {
            pattern.find(t)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        }
    }
    url?.let { u ->
        for (pattern in SEASON_PATTERNS) {
            pattern.find(u)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        }
    }
    return null
}
