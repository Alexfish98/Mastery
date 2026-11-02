package com.example.domain.model

/**
 * Domain representations decoupled from Room SQLite entities and UI frameworks.
 */

data class TimetableSlot(
    val id: Int = 0,
    val dayOfWeek: Int, // 0 = Monday, 1 = Tuesday, ..., 6 = Sunday
    val dayName: String,
    val timeSlot: String, // e.g. "08:00 - 09:30"
    val subject: String,
    val topic: String,
    val audioUri: String? = null,
    val audioTitle: String? = null,
    val isAlarmEnabled: Boolean = true,
    val notes: String = ""
)

data class StudyTrackMetric(
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val subject: String,
    val focusMinutes: Int,
    val quizScorePercent: Int,
    val questionsSolved: Int,
    val milestoneTitle: String,
    val notes: String = ""
)

data class DictionaryTerm(
    val id: Int = 0,
    val term: String,
    val definition: String,
    val subject: String, // "English", "Chemistry", "Physics", "Biology", "Mathematics"
    val partOfSpeech: String = "noun",
    val phonetic: String = "",
    val exampleUsage: String = "",
    val synonyms: List<String> = emptyList(),
    val customMetadata: Map<String, String> = emptyMap()
)

data class QuestionItem(
    val id: String,
    val prompt: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int, // 0..3 (A..D)
    val explanation: String,
    val subject: String = "General",
    val chapter: String = "",
    val difficulty: String = "medium",
    val isUserCustom: Boolean = false
)

data class DashboardCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val isVisible: Boolean = true,
    val orderIndex: Int = 0
)

enum class AppFontFamily(val displayName: String, val amharicSample: String) {
    MODERN_SANS("Modern Sans", "ዘመናዊ ሳንስ"),
    ETHIOPIC_SERIF("Ethiopic Serif", "ኢትዮጵያዊ ሰሪፍ"),
    SYSTEM_SERIF("Classic Serif", "ክላሲክ ሰሪፍ"),
    MONOSPACE("Scientific Mono", "ሞኖስፔስ ቀመሮች"),
    SYSTEM_DEFAULT("System Default", "ነባሪ ፎንት")
}
