package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ChatSessionEntity
import com.example.data.model.FactCategory
import com.example.data.model.KnowledgeSource
import com.example.data.model.LearnedFact
import com.example.data.model.SourceType

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = try {
        SourceType.valueOf(value)
    } catch (e: Exception) {
        SourceType.TEXT_NOTE
    }

    @TypeConverter
    fun fromFactCategory(value: FactCategory): String = value.name

    @TypeConverter
    fun toFactCategory(value: String): FactCategory = try {
        FactCategory.valueOf(value)
    } catch (e: Exception) {
        FactCategory.GENERAL
    }
}

@Database(
    entities = [
        KnowledgeSource::class,
        LearnedFact::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeSourceDao(): KnowledgeSourceDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aurora_ai_database"
                    )
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                } catch (e: Exception) {
                    context.deleteDatabase("aurora_ai_database")
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aurora_ai_database"
                    )
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                }
                INSTANCE = db
                db
            }
        }
    }
}
