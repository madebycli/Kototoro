package org.skepsun.kototoro.scrobbling.common.data

import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerContent
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerContentInfo
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerUser

interface ScrobblerRepository {

    val oauthUrl: String

    val isAuthorized: Boolean

    val cachedUser: ScrobblerUser?

    suspend fun authorize(code: String?)

    suspend fun loadUser(): ScrobblerUser

    fun logout()

    suspend fun unregister(mangaId: Long)

    suspend fun findContent(query: String, offset: Int, isAnime: Boolean = false): List<ScrobblerContent>

    suspend fun getContentInfo(id: Long): ScrobblerContentInfo

    /**
     * Binds [mangaId] to [content] on the service and stores the binding locally.
     *
     * @return `true` when this call created the entry on the user's remote list, `false` when the list already had one
     * and it was imported unchanged. Callers initialize a created entry from the local reading state, never an imported
     * one.
     */
    suspend fun createRate(mangaId: Long, content: ScrobblerContent): Boolean

    /**
     * Reads the user's current list entry for [targetId] from the service into the local binding of [mangaId], so
     * changes made on the service's own site show up here.
     *
     * @return `false` when the service does not support it or the entry is no longer on the user's list; the local
     * binding is then left as it was.
     */
    suspend fun refreshRate(mangaId: Long, targetId: Long, mediaType: String): Boolean = false

    suspend fun updateRate(rateId: Int, mangaId: Long, chapter: Int)

    suspend fun updateRate(rateId: Int, mangaId: Long, rating: Float, status: String?, comment: String?)
}
