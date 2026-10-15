package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class SourceType {
    TEXT_NOTE,
    DOCUMENT,
    URL_REFERENCE,
    USER_PROFILE,
    CUSTOM_RULE
}

/**
 * Room Entity for storing user-defined knowledge base entries locally.
 */
@Entity(
    tableName = "knowledge_sources",
    indices = [
        Index(value = ["sourceType"]),
        Index(value = ["isEnabled"]),
        Index(value = ["createdAt"])
    ]
)
data class KnowledgeSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val sourceType: SourceType = SourceType.TEXT_NOTE,
    val isEnabled: Boolean = true,
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class FactCategory {
    STYLE,
    VOCABULARY,
    HUMOR,
    PREFERENCE,
    SUPPORT_MEMORY,
    GENERAL
}

data class AdaptiveStyleSummary(
    val summary: String,
    val dominantTone: String = "Bezpośredni i dynamiczny",
    val humorStyle: String = "Inteligentny dowcip i riposty",
    val pace: String = "Zwięzły i konwersacyjny",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity for storing dynamically learned user communication style facts locally.
 */
@Entity(
    tableName = "learned_facts",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isActive"]),
        Index(value = ["timestamp"])
    ]
)
data class LearnedFact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fact: String,
    val category: FactCategory = FactCategory.GENERAL,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Room Entity for storing user conversation sessions locally.
 */
@Entity(
    tableName = "chat_sessions",
    indices = [
        Index(value = ["lastMessageAt"])
    ]
)
data class ChatSessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val title: String = "Nowa rozmowa",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity for storing user conversation messages locally.
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "timestamp"]),
        Index(value = ["timestamp"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false
)
