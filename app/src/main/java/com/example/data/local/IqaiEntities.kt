package com.example.data.local

import androidx.room.*

/**
 * 1. COMPREHENSIVE ROOM DATABASE BOILERPLATE & ENTITIES
 */

@Entity(tableName = "textbooks")
data class Textbook(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val localUri: String,
    val lastPageRead: Int = 0,
    val totalPages: Int = 0
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Textbook::class,
            parentColumns = ["id"],
            childColumns = ["textbookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["textbookId"])]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val textbookId: Int,
    val pageNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "iqai_flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deckName: String,
    val frontText: String,
    val backText: String,
    val localImageUri: String? = null,
    val interval: Int = 1, // review interval in days
    val easeFactor: Double = 2.5, // SM-2 ease factor
    val dueDate: Long = System.currentTimeMillis() // epoch millisecond timestamp
)

@Entity(tableName = "offline_notes")
data class OfflineNote(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    val id: Int = 0,
    val title: String,
    val content: String,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = OfflineNote::class)
data class NoteFTS(
    val title: String,
    val content: String
)

@Entity(tableName = "iqai_focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val wasSuccessful: Boolean
)

@Entity(tableName = "chat_history")
data class ChatHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val contextSnapshot: String? = null
)

@Entity(tableName = "timetable_plans")
data class TimetablePlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dayOfWeek: Int, // 0 = Mon, 1 = Tue, ..., 6 = Sun
    val dayName: String,
    val timeSlot: String,
    val subject: String,
    val topic: String,
    val audioUri: String? = null,
    val audioTitle: String? = null,
    val isAlarmEnabled: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "study_metric_tracks")
data class StudyMetricTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val subject: String,
    val focusMinutes: Int,
    val quizScorePercent: Int,
    val questionsSolved: Int,
    val milestoneTitle: String,
    val notes: String = ""
)

@Entity(tableName = "dictionary_entries")
data class DictionaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val term: String,
    val definition: String,
    val subject: String, // "English", "Chemistry", "Physics", "Biology", "Mathematics"
    val partOfSpeech: String = "noun",
    val phonetic: String = "",
    val exampleUsage: String = "",
    val synonymsJson: String = "[]",
    val customMetadataJson: String = "{}"
)

@Entity(tableName = "custom_mcq_questions")
data class CustomQuestionEntity(
    @PrimaryKey
    val id: String,
    val prompt: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int, // 0..3
    val explanation: String,
    val subject: String = "General",
    val chapter: String = "",
    val difficulty: String = "medium",
    val isUserCustom: Boolean = true
)
