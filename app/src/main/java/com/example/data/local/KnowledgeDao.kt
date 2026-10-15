package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_sources ORDER BY createdAt DESC")
    fun getAllSources(): Flow<List<KnowledgeSource>>

    @Query("SELECT * FROM knowledge_sources WHERE isEnabled = 1")
    suspend fun getActiveSources(): List<KnowledgeSource>

    @Query("SELECT * FROM knowledge_sources WHERE isEnabled = 1")
    fun getActiveSourcesFlow(): Flow<List<KnowledgeSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: KnowledgeSource): Long

    @Update
    suspend fun updateSource(source: KnowledgeSource)

    @Delete
    suspend fun deleteSource(source: KnowledgeSource)

    @Query("UPDATE knowledge_sources SET isEnabled = :enabled WHERE id = :id")
    suspend fun setSourceEnabled(id: Long, enabled: Boolean)

    // Learned Facts / User communication style
    @Query("SELECT * FROM learned_facts ORDER BY timestamp DESC")
    fun getAllFacts(): Flow<List<LearnedFact>>

    @Query("SELECT * FROM learned_facts WHERE isActive = 1")
    suspend fun getActiveFacts(): List<LearnedFact>

    @Query("SELECT * FROM learned_facts WHERE isActive = 1")
    fun getActiveFactsFlow(): Flow<List<LearnedFact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: LearnedFact): Long

    @Update
    suspend fun updateFact(fact: LearnedFact)

    @Delete
    suspend fun deleteFact(fact: LearnedFact)

    @Query("UPDATE learned_facts SET isActive = :active WHERE id = :id")
    suspend fun setFactActive(id: Long, active: Boolean)

    @Query("SELECT * FROM learned_facts WHERE category = :category AND isActive = 1")
    suspend fun getFactsByCategory(category: FactCategory): List<LearnedFact>

    @Query("SELECT * FROM learned_facts WHERE category != :category AND isActive = 1")
    suspend fun getFactsExcludingCategory(category: FactCategory): List<LearnedFact>

    @Query("SELECT * FROM learned_facts WHERE category = :category ORDER BY timestamp DESC")
    fun getFactsByCategoryFlow(category: FactCategory): Flow<List<LearnedFact>>

    @Query("DELETE FROM learned_facts WHERE category = :category")
    suspend fun clearFactsByCategory(category: FactCategory)
}
