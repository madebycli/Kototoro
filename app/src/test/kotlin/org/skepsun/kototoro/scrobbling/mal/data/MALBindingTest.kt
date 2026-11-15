package org.skepsun.kototoro.scrobbling.mal.data

import android.content.Context
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.skepsun.kototoro.core.db.MangaDatabase
import org.skepsun.kototoro.scrobbling.common.data.ScrobblerStorage
import org.skepsun.kototoro.scrobbling.common.data.ScrobblingDao
import org.skepsun.kototoro.scrobbling.common.domain.model.ScrobblerContent

class MALBindingTest {
    private val content = ScrobblerContent(377, "Title", null, null, "", "manga")

    @Test
    fun `existing entry is imported without a write`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"id":377,"my_list_status":{
                "status":"reading","score":8,"num_chapters_read":12,"comments":"Keep"
            }}"""))
            val dao = mockk<ScrobblingDao>(relaxed = true)
            repository(server, dao).createRate(55, content)
            assertEquals(1, server.requestCount)
            assertEquals("GET", server.takeRequest().method)
            coVerify { dao.upsert(match {
                it.mangaId == 55L && it.targetId == 377L && it.chapter == 12 &&
                    it.rating == 0.8f && it.comment == "Keep"
            }) }
        }
    }

    @Test
    fun `failed lookup does not create or replace an entry`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))
            val dao = mockk<ScrobblingDao>(relaxed = true)
            assertThrows(Exception::class.java) { runBlocking { repository(server, dao).createRate(55, content) } }
            assertEquals(1, server.requestCount)
            coVerify(exactly = 0) { dao.upsert(any()) }
        }
    }

    private fun repository(server: MockWebServer, dao: ScrobblingDao): MALRepository {
        val db = mockk<MangaDatabase> { every { getScrobblingDao() } returns dao }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().url(server.url("/")).build())
        }.build()
        return MALRepository(mockk<Context>(relaxed = true), client, mockk<ScrobblerStorage>(relaxed = true), db, mockk())
    }
}
