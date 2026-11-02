package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        // Foundational Mastery Modules
        Textbook::class,
        Bookmark::class,
        Flashcard::class,
        OfflineNote::class,
        NoteFTS::class,
        FocusSession::class,
        ChatHistory::class,
        TimetablePlanEntity::class,
        StudyMetricTrackEntity::class,
        DictionaryEntryEntity::class,
        CustomQuestionEntity::class,

        // Companion Suite Entities
        NoteEntity::class,
        TaskEntity::class,
        FlashcardEntity::class,
        PdfDocumentEntity::class,
        StudyAlarmEntity::class,
        AudioTrackEntity::class,
        FocusSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Mastery Foundational DAOs
    abstract fun textbookDao(): TextbookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun iqaiFlashcardDao(): IqaiFlashcardDao
    abstract fun offlineNoteDao(): OfflineNoteDao
    abstract fun iqaiFocusSessionDao(): IqaiFocusSessionDao
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun timetablePlanDao(): TimetablePlanDao
    abstract fun studyMetricTrackDao(): StudyMetricTrackDao
    abstract fun dictionaryEntryDao(): DictionaryEntryDao
    abstract fun customQuestionDao(): CustomQuestionDao

    // Suite DAOs
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun pdfDocumentDao(): PdfDocumentDao
    abstract fun studyAlarmDao(): StudyAlarmDao
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mastery_offline_study.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
