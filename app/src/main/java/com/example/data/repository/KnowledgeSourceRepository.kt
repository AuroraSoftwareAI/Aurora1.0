package com.example.data.repository

import com.example.data.local.KnowledgeSourceDao
import com.example.data.model.KnowledgeSource
import com.example.data.model.SourceType
import kotlinx.coroutines.flow.Flow

/**
 * Repository providing an abstracted data layer for [KnowledgeSource] items.
 * Adheres to the Android Architecture Guidelines and Room integration best practices.
 */
class KnowledgeSourceRepository(
    private val dao: KnowledgeSourceDao
) {
    /**
     * Reactive stream of all user-defined knowledge sources, sorted by newest first.
     */
    val allSources: Flow<List<KnowledgeSource>> = dao.getAllSources()

    /**
     * Reactive stream of currently active (enabled) knowledge sources used by the AI assistant.
     */
    val activeSourcesFlow: Flow<List<KnowledgeSource>> = dao.getActiveSourcesFlow()

    /**
     * Total number of knowledge sources.
     */
    val sourceCount: Flow<Int> = dao.getSourceCount()

    /**
     * Retrieves all active sources for a one-shot query.
     */
    suspend fun getActiveSources(): List<KnowledgeSource> = dao.getActiveSources()

    /**
     * Finds a knowledge source by its primary key.
     */
    suspend fun getSourceById(id: Long): KnowledgeSource? = dao.getSourceById(id)

    /**
     * Observes a specific knowledge source by its ID.
     */
    fun getSourceByIdFlow(id: Long): Flow<KnowledgeSource?> = dao.getSourceByIdFlow(id)

    /**
     * Streams knowledge sources filtered by their specific [SourceType].
     */
    fun getSourcesByType(type: SourceType): Flow<List<KnowledgeSource>> = dao.getSourcesByType(type)

    /**
     * Searches knowledge sources by query matching title or content.
     */
    fun searchSources(query: String): Flow<List<KnowledgeSource>> = dao.searchSources(query)

    /**
     * Inserts a new user-defined knowledge source.
     */
    suspend fun insertSource(source: KnowledgeSource): Long = dao.insertSource(source)

    /**
     * Inserts multiple knowledge sources.
     */
    suspend fun insertSources(sources: List<KnowledgeSource>): List<Long> = dao.insertSources(sources)

    /**
     * Updates an existing knowledge source.
     */
    suspend fun updateSource(source: KnowledgeSource) = dao.updateSource(source)

    /**
     * Updates the content and title of a knowledge source.
     */
    suspend fun updateSourceContent(
        id: Long,
        title: String,
        content: String,
        sourceType: SourceType
    ) = dao.updateSourceContent(id, title, content, sourceType)

    /**
     * Enables or disables a knowledge source.
     */
    suspend fun setSourceEnabled(id: Long, enabled: Boolean) = dao.setSourceEnabled(id, enabled)

    /**
     * Deletes a knowledge source.
     */
    suspend fun deleteSource(source: KnowledgeSource) = dao.deleteSource(source)

    /**
     * Deletes a knowledge source by ID.
     */
    suspend fun deleteSourceById(id: Long) = dao.deleteSourceById(id)

    /**
     * Deletes all knowledge sources.
     */
    suspend fun deleteAllSources() = dao.deleteAllSources()
}
