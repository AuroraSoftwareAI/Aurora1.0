package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository providing an abstracted data layer for user conversation history in the local Room database.
 * Adheres to Android Architecture Guidelines and Room integration best practices.
 */
class ChatRepository(
    private val chatDao: ChatDao
) {
    /**
     * Observable stream of all conversation sessions sorted by most recent first.
     */
    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    /**
     * Observable stream of total conversation session count.
     */
    val sessionCount: Flow<Int> = chatDao.getSessionCount()

    /**
     * Observable stream of total messages stored across all sessions.
     */
    val totalMessageCount: Flow<Int> = chatDao.getTotalMessageCount()

    /**
     * Returns a reactive flow of messages for a specific conversation session, ordered chronologically.
     */
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    /**
     * Returns a reactive flow observing a specific session by ID.
     */
    fun getSessionById(sessionId: String): Flow<ChatSessionEntity?> =
        chatDao.getSessionById(sessionId)

    /**
     * One-shot retrieval of a session by ID.
     */
    suspend fun getSession(sessionId: String): ChatSessionEntity? =
        chatDao.getSession(sessionId)

    /**
     * Retrieves recent messages for context generation.
     */
    suspend fun getRecentMessages(sessionId: String, limit: Int = 20): List<ChatMessageEntity> =
        chatDao.getRecentMessages(sessionId, limit)

    /**
     * Inserts or updates a conversation session.
     */
    suspend fun insertSession(session: ChatSessionEntity) =
        chatDao.insertSession(session)

    /**
     * Inserts a single message into conversation history.
     */
    suspend fun insertMessage(message: ChatMessageEntity): Long =
        chatDao.insertMessage(message)

    /**
     * Batch inserts messages into conversation history.
     */
    suspend fun insertMessages(messages: List<ChatMessageEntity>): List<Long> =
        chatDao.insertMessages(messages)

    /**
     * Updates the title of an existing conversation session.
     */
    suspend fun updateSessionTitle(sessionId: String, title: String) =
        chatDao.updateSessionTitle(sessionId, title)

    /**
     * Updates the timestamp of the last message in a session.
     */
    suspend fun updateSessionTimestamp(sessionId: String, timestamp: Long) =
        chatDao.updateSessionTimestamp(sessionId, timestamp)

    /**
     * Deletes a specific message by its primary key ID.
     */
    suspend fun deleteMessageById(id: Long) =
        chatDao.deleteMessageById(id)

    /**
     * Clears all messages belonging to a given session while keeping the session record.
     */
    suspend fun clearMessagesForSession(sessionId: String) =
        chatDao.clearMessagesForSession(sessionId)

    /**
     * Atomically deletes a session and all its messages.
     */
    suspend fun deleteSessionWithMessages(sessionId: String) =
        chatDao.deleteSessionWithMessages(sessionId)

    /**
     * Completely purges all conversation history.
     */
    suspend fun clearAllConversationHistory() =
        chatDao.clearAllConversationHistory()

    /**
     * Searches message content across all conversation history.
     */
    fun searchMessages(query: String): Flow<List<ChatMessageEntity>> =
        chatDao.searchMessages(query)
}
