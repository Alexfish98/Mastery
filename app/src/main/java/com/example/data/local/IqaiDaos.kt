package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TextbookDao {
    @Query("SELECT * FROM textbooks ORDER BY id DESC")
    fun getAllTextbooks(): Flow<List<Textbook>>

    @Query("SELECT * FROM textbooks WHERE id = :id")
    fun getTextbookById(id: Int): Flow<Textbook?>

    @Query("SELECT * FROM textbooks WHERE id = :id")
    suspend fun getTextbookByIdSync(id: Int): Textbook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextbook(textbook: Textbook): Long

    @Update
    suspend fun updateTextbook(textbook: Textbook)

    @Query("UPDATE textbooks SET lastPageRead = :lastPage WHERE id = :id")
    suspend fun updateLastPage(id: Int, lastPage: Int)

    @Query("DELETE FROM textbooks WHERE id = :id")
    suspend fun deleteTextbook(id: Int)

    @Query("DELETE FROM textbooks")
    suspend fun clearAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE textbookId = :textbookId ORDER BY pageNumber ASC")
    fun getBookmarksForTextbook(textbookId: Int): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()
}

@Dao
interface IqaiFlashcardDao {
    @Query("SELECT * FROM iqai_flashcards ORDER BY dueDate ASC")
    fun getAllCards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM iqai_flashcards WHERE deckName = :deck ORDER BY dueDate ASC")
    fun getCardsForDeck(deck: String): Flow<List<Flashcard>>

    @Query("SELECT DISTINCT deckName FROM iqai_flashcards")
    fun getAllDecks(): Flow<List<String>>

    @Query("SELECT * FROM iqai_flashcards WHERE dueDate <= :now ORDER BY dueDate ASC")
    fun getDueCards(now: Long = System.currentTimeMillis()): Flow<List<Flashcard>>

    @Query("SELECT * FROM iqai_flashcards WHERE id = :id")
    suspend fun getCardById(id: Int): Flashcard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Flashcard>)

    @Update
    suspend fun updateCard(card: Flashcard)

    @Query("DELETE FROM iqai_flashcards WHERE id = :id")
    suspend fun deleteCardById(id: Int)

    @Query("DELETE FROM iqai_flashcards")
    suspend fun clearAll()
}

@Dao
interface OfflineNoteDao {
    @Query("SELECT * FROM offline_notes ORDER BY lastModified DESC")
    fun getAllNotes(): Flow<List<OfflineNote>>

    @Query("SELECT * FROM offline_notes WHERE rowid = :id")
    fun getNoteById(id: Int): Flow<OfflineNote?>

    @Query("SELECT * FROM offline_notes WHERE rowid = :id")
    suspend fun getNoteByIdSync(id: Int): OfflineNote?

    @Query("SELECT * FROM offline_notes WHERE title = :title LIMIT 1")
    suspend fun getNoteByTitle(title: String): OfflineNote?

    @Query("""
        SELECT offline_notes.* FROM offline_notes
        JOIN notes_fts ON offline_notes.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query
        ORDER BY offline_notes.lastModified DESC
    """)
    fun searchNotesFTS(query: String): Flow<List<OfflineNote>>

    @Query("SELECT * FROM offline_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchNotesFallback(query: String): Flow<List<OfflineNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: OfflineNote): Long

    @Update
    suspend fun updateNote(note: OfflineNote)

    @Delete
    suspend fun deleteNote(note: OfflineNote)

    @Query("DELETE FROM offline_notes WHERE rowid = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("DELETE FROM offline_notes")
    suspend fun clearAll()
}

@Dao
interface IqaiFocusSessionDao {
    @Query("SELECT * FROM iqai_focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM iqai_focus_sessions WHERE wasSuccessful = 1 ORDER BY timestamp DESC")
    fun getSuccessfulSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM iqai_focus_sessions WHERE wasSuccessful = 0 ORDER BY timestamp DESC")
    fun getFailedSessions(): Flow<List<FocusSession>>

    @Query("SELECT COUNT(*) FROM iqai_focus_sessions WHERE wasSuccessful = 1")
    fun getSuccessfulCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM iqai_focus_sessions WHERE wasSuccessful = 0")
    fun getFailedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Query("DELETE FROM iqai_focus_sessions")
    suspend fun clearAll()
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatHistory>>

    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<ChatHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatHistory): Long

    @Query("DELETE FROM chat_history")
    suspend fun clearHistory()
}

@Dao
interface TimetablePlanDao {
    @Query("SELECT * FROM timetable_plans ORDER BY dayOfWeek ASC, timeSlot ASC")
    fun getAllPlans(): Flow<List<TimetablePlanEntity>>

    @Query("SELECT * FROM timetable_plans WHERE dayOfWeek = :dayOfWeek ORDER BY timeSlot ASC")
    fun getPlansForDay(dayOfWeek: Int): Flow<List<TimetablePlanEntity>>

    @Query("SELECT * FROM timetable_plans WHERE id = :id")
    suspend fun getPlanById(id: Int): TimetablePlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TimetablePlanEntity): Long

    @Update
    suspend fun updatePlan(plan: TimetablePlanEntity)

    @Query("DELETE FROM timetable_plans WHERE id = :id")
    suspend fun deletePlan(id: Int)

    @Query("UPDATE timetable_plans SET isAlarmEnabled = :isEnabled WHERE id = :id")
    suspend fun setAlarmEnabled(id: Int, isEnabled: Boolean)

    @Query("DELETE FROM timetable_plans")
    suspend fun clearAll()
}

@Dao
interface StudyMetricTrackDao {
    @Query("SELECT * FROM study_metric_tracks ORDER BY timestamp DESC")
    fun getAllTracks(): Flow<List<StudyMetricTrackEntity>>

    @Query("SELECT * FROM study_metric_tracks WHERE subject = :subject ORDER BY timestamp DESC")
    fun getTracksForSubject(subject: String): Flow<List<StudyMetricTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: StudyMetricTrackEntity): Long

    @Update
    suspend fun updateTrack(track: StudyMetricTrackEntity)

    @Query("DELETE FROM study_metric_tracks WHERE id = :id")
    suspend fun deleteTrack(id: Int)

    @Query("SELECT SUM(focusMinutes) FROM study_metric_tracks")
    fun getTotalFocusMinutes(): Flow<Int?>

    @Query("DELETE FROM study_metric_tracks")
    suspend fun clearAll()
}

@Dao
interface DictionaryEntryDao {
    @Query("SELECT * FROM dictionary_entries ORDER BY term COLLATE NOCASE ASC")
    fun getAllEntries(): Flow<List<DictionaryEntryEntity>>

    @Query("SELECT * FROM dictionary_entries WHERE term LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%' ORDER BY term COLLATE NOCASE ASC")
    fun searchEntries(query: String): Flow<List<DictionaryEntryEntity>>

    @Query("SELECT * FROM dictionary_entries WHERE subject = :subject ORDER BY term COLLATE NOCASE ASC")
    fun getEntriesBySubject(subject: String): Flow<List<DictionaryEntryEntity>>

    @Query("SELECT * FROM dictionary_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): DictionaryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DictionaryEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DictionaryEntryEntity>): List<Long>

    @Update
    suspend fun updateEntry(entry: DictionaryEntryEntity)

    @Query("DELETE FROM dictionary_entries WHERE id = :id")
    suspend fun deleteEntry(id: Int)

    @Query("SELECT COUNT(*) FROM dictionary_entries")
    suspend fun getCount(): Int

    @Query("DELETE FROM dictionary_entries")
    suspend fun clearAll()
}

@Dao
interface CustomQuestionDao {
    @Query("SELECT * FROM custom_mcq_questions ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<CustomQuestionEntity>>

    @Query("SELECT * FROM custom_mcq_questions WHERE subject = :subject ORDER BY id ASC")
    fun getQuestionsBySubject(subject: String): Flow<List<CustomQuestionEntity>>

    @Query("SELECT * FROM custom_mcq_questions WHERE id = :id")
    suspend fun getQuestionById(id: String): CustomQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: CustomQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<CustomQuestionEntity>)

    @Update
    suspend fun updateQuestion(question: CustomQuestionEntity)

    @Query("DELETE FROM custom_mcq_questions WHERE id = :id")
    suspend fun deleteQuestion(id: String)

    @Query("SELECT COUNT(*) FROM custom_mcq_questions")
    suspend fun getQuestionCount(): Int

    @Query("DELETE FROM custom_mcq_questions")
    suspend fun clearAll()
}
