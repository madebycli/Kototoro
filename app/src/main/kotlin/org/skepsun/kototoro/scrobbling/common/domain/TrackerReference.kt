package org.skepsun.kototoro.scrobbling.common.domain

import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerService

/** Host fragments of each tracker's website, to accept only links that belong to the tracker being searched. */
private fun ScrobblerService.siteHosts(): List<String> = when (this) {
    ScrobblerService.SHIKIMORI -> listOf("shikimori")
    ScrobblerService.ANILIST -> listOf("anilist.co")
    ScrobblerService.MAL -> listOf("myanimelist.net")
    ScrobblerService.KITSU -> listOf("kitsu.")
    ScrobblerService.BANGUMI -> listOf("bgm.tv", "bangumi.tv", "chii.in")
    ScrobblerService.MANGAUPDATES -> listOf("mangaupdates.com")
    ScrobblerService.SIMKL -> listOf("simkl.")
    ScrobblerService.MANGABAKA -> listOf("mangabaka.")
}

/** A link path segment holding an entry id: `12345`, `12345-title` or Shikimori's `z12345-title`. */
private val ID_SEGMENT = Regex("^[a-z]?(\\d+)(?:-.*)?$", RegexOption.IGNORE_CASE)

/**
 * The entry id the user typed into a tracker search instead of a title: `id:123`, or a link to the entry on
 * [service]'s own site such as `https://anilist.co/manga/30013/One-Piece`. `null` for anything else, which is then
 * searched as a title; a bare number stays a title, since some are ("86").
 */
fun parseTrackerReference(query: String, service: ScrobblerService): Long? {
    val text = query.trim()
    if (text.startsWith("id:", ignoreCase = true)) {
        return text.substring(3).trim().toLongOrNull()?.takeIf { it > 0 }
    }
    val match = Regex("^https?://([^/?#]+)(/[^?#]*)?", RegexOption.IGNORE_CASE).find(text) ?: return null
    val host = match.groupValues[1].lowercase()
    if (service.siteHosts().none { host.contains(it) }) return null
    return match.groupValues[2].split('/')
        .firstNotNullOfOrNull { segment -> ID_SEGMENT.matchEntire(segment)?.groupValues?.get(1)?.toLongOrNull() }
        ?.takeIf { it > 0 }
}
