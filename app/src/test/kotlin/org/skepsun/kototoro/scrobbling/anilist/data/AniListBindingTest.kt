package org.skepsun.kototoro.scrobbling.anilist.data

import android.content.Context
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.core.db.MangaDatabase
import org.skepsun.kototoro.scrobbling.common.data.ScrobblerStorage
import org.skepsun.kototoro.scrobbling.common.data.ScrobblingDao
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerContent

class AniListBindingTest {
    @Test
    fun `existing entry is imported without a mutation`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"data":{"Media":{"mediaListEntry":{
                "id":91,"mediaId":377,"status":"CURRENT","notes":"Keep","score":8.5,"progress":12
            }}}}"""))
            val dao = mockk<ScrobblingDao>(relaxed = true)
            val db = mockk<MangaDatabase> { every { getScrobblingDao() } returns dao }
            val storage = mockk<ScrobblerStorage>(relaxed = true)
            val context = mockk<Context>(relaxed = true)
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().url(server.url("/")).build())
            }.build()
            AniListRepository(context, client, storage, db).createRate(
                55, ScrobblerContent(377, "Title", null, null, ""),
            )
            assertEquals(1, server.requestCount)
            assertTrue(server.takeRequest().body.readUtf8().contains("mediaListEntry"))
            coVerify { dao.upsert(match {
                it.mangaId == 55L && it.targetId == 377L && it.chapter == 12 &&
                    it.rating == 0.85f && it.status == "CURRENT" && it.comment == "Keep"
            }) }
        }
    }
}
