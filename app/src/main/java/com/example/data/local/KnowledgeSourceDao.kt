package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.KnowledgeSource
import com.example.data.model.SourceType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing CRUD operations on user-defined [KnowledgeSource] items.
 */
@Dao
interface KnowledgeSourceDao {

    // --- CREATE ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: KnowledgeSource): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<KnowledgeSource>): List<Long>

    // --- READ ---

    @Query("SELECT * FROM knowledge_sources ORDER BY createdAt DESC")
    fun getAllSources(): Flow<List<KnowledgeSource>>

    @Query("SELECT * FROM knowledge_sources WHERE id = :id")
    suspend fun getSourceById(id: Long): KnowledgeSource?

    @Query("SELECT * FROM knowledge_sources WHERE id = :id")
    fun getSourceByIdFlow(id: Long): Flow<KnowledgeSource?>

    @Query("SELECT * FROM knowledge_sources WHERE isEnabled = 1 ORDER BY createdAt DESC")
    suspend fun getActiveSources(): List<KnowledgeSource>

    @Query("SELECT * FROM knowledge_sources WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getActiveSourcesFlow(): Flow<List<KnowledgeSource>>

    @Query("SELECT * FROM knowledge_sources WHERE sourceType = :type ORDER BY createdAt DESC")
    fun getSourcesByType(type: SourceType): Flow<List<KnowledgeSource>>

    @Query("SELECT * FROM knowledge_sources WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchSources(query: String): Flow<List<KnowledgeSource>>

    @Query("SELECT COUNT(*) FROM knowledge_sources")
    fun getSourceCount(): Flow<Int>

    // --- UPDATE ---

    @Update
    suspend fun updateSource(source: KnowledgeSource)

    @Query("UPDATE knowledge_sources SET isEnabled = :enabled WHERE id = :id")
    suspend fun setSourceEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE knowledge_sources SET title = :title, content = :content, sourceType = :sourceType, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSourceContent(
        id: Long,
        title: String,
        content: String,
        sourceType: SourceType,
        updatedAt: Long = System.currentTimeMillis()
    )

    // --- DELETE ---

    @Delete
    suspend fun deleteSource(source: KnowledgeSource)

    @Query("DELETE FROM knowledge_sources WHERE id = :id")
    suspend fun deleteSourceById(id: Long)

    @Query("DELETE FROM knowledge_sources")
    suspend fun deleteAllSources()
}
