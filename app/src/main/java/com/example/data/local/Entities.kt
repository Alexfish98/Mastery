package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val body: String,
    val tags: String = "", // comma-separated
    val isFavorite: Boolean = false,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val due: String,
    val priority: String, // "High", "Normal", "Low"
    val done: Boolean = false,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val front: String,
    val back: String,
    val subject: String,
    val created: Long = System.currentTimeMillis(),
    val reviews: Int = 0,
    val known: Int = 0,
    val again: Int = 0
)

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uriString: String,
    val fileName: String,
    val subject: String = "General",
    val grade: String = "12",
    val pageCount: Int = 0,
    val lastPage: Int = 0,
    val lastOpenedTime: Long = System.currentTimeMillis(),
    val fileSizeFormatted: String = "",
    val bookmarksJson: String = "[]" // JSON array of bookmarked page numbers
)

@Entity(tableName = "study_alarms")
data class StudyAlarmEntity(
    @PrimaryKey val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String = "1,2,3,4,5,6,7", // 1=Mon..7=Sun
    val isEnabled: Boolean = true,
    val isExamCountdown: Boolean = false,
    val examDate: String = "",
    val vibrate: Boolean = true
)

@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String = "Study Ambient",
    val uriString: String = "",
    val durationMs: Long = 0L,
    val playlist: String = "My Music", // "Lo-fi", "Instrumental", "Classical", "Nature", "My Music"
    val isBuiltIn: Boolean = false
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val durationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val distractions: String = "", // comma/newline-separated notes logged during session
    val mode: String = "pomodoro" // "pomodoro", "custom", "sprint"
)
