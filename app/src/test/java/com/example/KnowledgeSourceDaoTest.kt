package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.KnowledgeSourceDao
import com.example.data.model.KnowledgeSource
import com.example.data.model.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class KnowledgeSourceDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: KnowledgeSourceDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.knowledgeSourceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetSourceById() = runBlocking {
        val source = KnowledgeSource(
            title = "Polityka firmy",
            content = "Zawsze odpowiadaj profesjonalnie.",
            sourceType = SourceType.CUSTOM_RULE,
            isEnabled = true
        )

        val id = dao.insertSource(source)
        assertTrue(id > 0)

        val retrieved = dao.getSourceById(id)
        assertNotNull(retrieved)
        assertEquals("Polityka firmy", retrieved?.title)
        assertEquals("Zawsze odpowiadaj profesjonalnie.", retrieved?.content)
        assertEquals(SourceType.CUSTOM_RULE, retrieved?.sourceType)
        assertTrue(retrieved?.isEnabled == true)
    }

    @Test
    fun updateSourceContent() = runBlocking {
        val source = KnowledgeSource(
            title = "Stary tytuł",
            content = "Stara treść",
            sourceType = SourceType.TEXT_NOTE
        )
        val id = dao.insertSource(source)

        dao.updateSourceContent(
            id = id,
            title = "Nowy tytuł",
            content = "Zaktualizowana treść",
            sourceType = SourceType.DOCUMENT
        )

        val updated = dao.getSourceById(id)
        assertNotNull(updated)
        assertEquals("Nowy tytuł", updated?.title)
        assertEquals("Zaktualizowana treść", updated?.content)
        assertEquals(SourceType.DOCUMENT, updated?.sourceType)
    }

    @Test
    fun toggleSourceEnabled() = runBlocking {
        val source = KnowledgeSource(
            title = "Aktywny dokument",
            content = "Treść",
            isEnabled = true
        )
        val id = dao.insertSource(source)

        dao.setSourceEnabled(id, false)
        val disabledSource = dao.getSourceById(id)
        assertFalse(disabledSource!!.isEnabled)

        val activeList = dao.getActiveSources()
        assertTrue(activeList.none { it.id == id })
    }

    @Test
    fun deleteSourceById() = runBlocking {
        val source = KnowledgeSource(
            title = "Do usunięcia",
            content = "Tymczasowa notatka"
        )
        val id = dao.insertSource(source)
        assertNotNull(dao.getSourceById(id))

        dao.deleteSourceById(id)
        assertNull(dao.getSourceById(id))
    }

    @Test
    fun searchSources() = runBlocking {
        dao.insertSource(
            KnowledgeSource(
                title = "Android Jetpack Compose",
                content = "Nowoczesny toolkit do tworzenia UI w Androidzie."
            )
        )
        dao.insertSource(
            KnowledgeSource(
                title = "Przepisy kulinarne",
                content = "Przepis na szarlotkę z cynamonem."
            )
        )

        val searchResults = dao.searchSources("Compose").first()
        assertEquals(1, searchResults.size)
        assertEquals("Android Jetpack Compose", searchResults[0].title)
    }

    @Test
    fun getSourcesByType() = runBlocking {
        dao.insertSource(
            KnowledgeSource(
                title = "Reguła 1",
                content = "Treść reguły",
                sourceType = SourceType.CUSTOM_RULE
            )
        )
        dao.insertSource(
            KnowledgeSource(
                title = "Notatka 1",
                content = "Treść notatki",
                sourceType = SourceType.TEXT_NOTE
            )
        )

        val rules = dao.getSourcesByType(SourceType.CUSTOM_RULE).first()
        assertEquals(1, rules.size)
        assertEquals("Reguła 1", rules[0].title)
    }

    @Test
    fun deleteAllSources() = runBlocking {
        dao.insertSource(KnowledgeSource(title = "S1", content = "C1"))
        dao.insertSource(KnowledgeSource(title = "S2", content = "C2"))

        dao.deleteAllSources()
        val all = dao.getAllSources().first()
        assertTrue(all.isEmpty())
    }
}
