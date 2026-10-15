package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChatDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ChatDao
    private lateinit var repository: ChatRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.chatDao()
        repository = ChatRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertSessionAndRetrieve() = runBlocking {
        val session = ChatSessionEntity(
            sessionId = "session_123",
            title = "Rozmowa o architekturze Androida"
        )
        repository.insertSession(session)

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)
        assertEquals("session_123", sessions[0].sessionId)
        assertEquals("Rozmowa o architekturze Androida", sessions[0].title)
    }

    @Test
    fun insertMessagesAndRetrieveForSession() = runBlocking {
        val sessionId = "session_messages_test"
        repository.insertSession(ChatSessionEntity(sessionId = sessionId, title = "Test sesji"))

        val msg1 = ChatMessageEntity(
            sessionId = sessionId,
            role = "user",
            content = "Cześć Aurora!",
            timestamp = 1000L
        )
        val msg2 = ChatMessageEntity(
            sessionId = sessionId,
            role = "model",
            content = "Cześć! W czym mogę ci pomóc?",
            timestamp = 2000L
        )

        val id1 = repository.insertMessage(msg1)
        val id2 = repository.insertMessage(msg2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        val messages = repository.getMessagesForSession(sessionId).first()
        assertEquals(2, messages.size)
        assertEquals("user", messages[0].role)
        assertEquals("Cześć Aurora!", messages[0].content)
        assertEquals("model", messages[1].role)
        assertEquals("Cześć! W czym mogę ci pomóc?", messages[1].content)
    }

    @Test
    fun getRecentMessagesReturnsLimitedCount() = runBlocking {
        val sessionId = "recent_test"
        repository.insertSession(ChatSessionEntity(sessionId = sessionId))

        for (i in 1..10) {
            repository.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = if (i % 2 == 1) "user" else "model",
                    content = "Wiadomość $i",
                    timestamp = 1000L * i
                )
            )
        }

        val recent5 = repository.getRecentMessages(sessionId, limit = 5)
        assertEquals(5, recent5.size)
        // Most recent first in getRecentMessages
        assertEquals("Wiadomość 10", recent5[0].content)
    }

    @Test
    fun deleteMessageById() = runBlocking {
        val sessionId = "del_msg_test"
        repository.insertSession(ChatSessionEntity(sessionId = sessionId))

        val msgId = repository.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = "Usuń mnie"
            )
        )

        repository.deleteMessageById(msgId)
        val remaining = repository.getMessagesForSession(sessionId).first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun clearMessagesForSession() = runBlocking {
        val sessionId = "clear_test"
        repository.insertSession(ChatSessionEntity(sessionId = sessionId))

        repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "user", content = "A"))
        repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "model", content = "B"))

        repository.clearMessagesForSession(sessionId)
        val remaining = repository.getMessagesForSession(sessionId).first()
        assertTrue(remaining.isEmpty())

        // Session itself still exists
        val session = repository.getSession(sessionId)
        assertNotNull(session)
    }

    @Test
    fun deleteSessionWithMessagesTransaction() = runBlocking {
        val sessionId = "cascade_test"
        repository.insertSession(ChatSessionEntity(sessionId = sessionId))
        repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "user", content = "Test"))

        repository.deleteSessionWithMessages(sessionId)

        val session = repository.getSession(sessionId)
        assertNull(session)

        val messages = repository.getMessagesForSession(sessionId).first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun searchMessages() = runBlocking {
        val sessionId = "search_test"
        repository.insertSession(ChatSessionEntity(sessionId = sessionId))

        repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "user", content = "Kotlin Coroutines Flow"))
        repository.insertMessage(ChatMessageEntity(sessionId = sessionId, role = "model", content = "Przepis na kawę"))

        val results = repository.searchMessages("Coroutines").first()
        assertEquals(1, results.size)
        assertEquals("Kotlin Coroutines Flow", results[0].content)
    }
}
