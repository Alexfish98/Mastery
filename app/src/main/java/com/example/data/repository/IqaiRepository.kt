package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow

/**
 * Clean Architecture Repository providing unified local data access for Textbooks,
 * Bookmarks, Wiki Offline Notes (with FTS), Strict Focus Sessions, and Chat History.
 */
class IqaiRepository(
    private val textbookDao: TextbookDao,
    private val bookmarkDao: BookmarkDao,
    private val noteDao: OfflineNoteDao,
    private val focusDao: IqaiFocusSessionDao,
    private val chatDao: ChatHistoryDao
) {
    // ---- Textbooks & Bookmarks ----
    val allTextbooks: Flow<List<Textbook>> = textbookDao.getAllTextbooks()

    fun getTextbookById(id: Int): Flow<Textbook?> = textbookDao.getTextbookById(id)

    suspend fun getTextbookByIdSync(id: Int): Textbook? = textbookDao.getTextbookByIdSync(id)

    suspend fun insertTextbook(textbook: Textbook): Long = textbookDao.insertTextbook(textbook)

    suspend fun updateLastPage(id: Int, lastPage: Int) = textbookDao.updateLastPage(id, lastPage)

    suspend fun deleteTextbook(id: Int) = textbookDao.deleteTextbook(id)

    fun getBookmarksForTextbook(textbookId: Int): Flow<List<Bookmark>> = bookmarkDao.getBookmarksForTextbook(textbookId)

    suspend fun addBookmark(textbookId: Int, pageNumber: Int, note: String): Long {
        return bookmarkDao.insertBookmark(
            Bookmark(
                textbookId = textbookId,
                pageNumber = pageNumber,
                note = note,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmark(id: Int) = bookmarkDao.deleteBookmarkById(id)

    // ---- Offline Notes & FTS Search ----
    val allNotes: Flow<List<OfflineNote>> = noteDao.getAllNotes()

    fun getNoteById(id: Int): Flow<OfflineNote?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdSync(id: Int): OfflineNote? = noteDao.getNoteByIdSync(id)

    suspend fun getNoteByTitle(title: String): OfflineNote? = noteDao.getNoteByTitle(title)

    fun searchNotes(query: String): Flow<List<OfflineNote>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return noteDao.getAllNotes()

        return try {
            // Format query for SQLite FTS4 (prefix search wildcard)
            val ftsQuery = cleanQuery.replace("\"", "").split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
            noteDao.searchNotesFTS(ftsQuery)
        } catch (e: Exception) {
            noteDao.searchNotesFallback(cleanQuery)
        }
    }

    suspend fun saveNote(id: Int, title: String, content: String): Long {
        val note = OfflineNote(
            id = id,
            title = title.trim(),
            content = content.trim(),
            lastModified = System.currentTimeMillis()
        )
        return if (id == 0) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note)
            id.toLong()
        }
    }

    suspend fun deleteNote(id: Int) = noteDao.deleteNoteById(id)

    // ---- Focus Sessions ----
    val allFocusSessions: Flow<List<FocusSession>> = focusDao.getAllSessions()
    val successfulSessions: Flow<List<FocusSession>> = focusDao.getSuccessfulSessions()
    val failedSessions: Flow<List<FocusSession>> = focusDao.getFailedSessions()
    val successfulCount: Flow<Int> = focusDao.getSuccessfulCount()
    val failedCount: Flow<Int> = focusDao.getFailedCount()

    suspend fun logFocusSession(durationMinutes: Int, wasSuccessful: Boolean): Long {
        return focusDao.insertSession(
            FocusSession(
                durationMinutes = durationMinutes,
                timestamp = System.currentTimeMillis(),
                wasSuccessful = wasSuccessful
            )
        )
    }

    // ---- Chat History ----
    val chatHistory: Flow<List<ChatHistory>> = chatDao.getAllMessages()

    suspend fun getChatHistorySync(): List<ChatHistory> = chatDao.getAllMessagesSync()

    suspend fun logChatMessage(role: String, content: String, contextSnapshot: String? = null): Long {
        return chatDao.insertMessage(
            ChatHistory(
                role = role,
                content = content,
                timestamp = System.currentTimeMillis(),
                contextSnapshot = contextSnapshot
            )
        )
    }

    suspend fun clearChat() = chatDao.clearHistory()

    /**
     * Seeds initial notes with cross-referenced wiki-links
     */
    suspend fun seedInitialNotesIfEmpty() {
        val starterNotes = listOf(
            OfflineNote(
                title = "EUEE Chemistry Strategy",
                content = """
# EUEE Chemistry High-Yield Strategy
Chemistry is one of the highest-scoring subjects in the Ethiopian Natural Science track.

## Core Pillars
1. Study equilibrium shifts and [[Le Chatelier's Principle]].
2. Review [[Buffer Solutions]] and pH calculations.
3. Master organic reaction pathways: alkanes, alkenes, alcohols, and carboxylic acids.

> Tip: Practice active recall using the Mastery Quiz module daily!
                """.trimIndent()
            ),
            OfflineNote(
                title = "Le Chatelier's Principle",
                content = """
# Le Chatelier's Principle
When a system at chemical equilibrium is disturbed by changes in:
- Concentration of reactants or products
- Temperature of the system
- Total pressure or volume (for gaseous systems)

The system responds by shifting the equilibrium position in the direction that counteracts the disturbance.

See related topics:
- [[Buffer Solutions]]
- [[Equilibrium Constant]]
                """.trimIndent()
            ),
            OfflineNote(
                title = "Buffer Solutions",
                content = """
# Buffer Solutions & The Henderson-Hasselbalch Equation
A buffer solution resists changes in pH when small amounts of an acid or base are added.

## Types of Buffers
- **Acidic Buffer**: Weak acid + its conjugate base salt (e.g., CH₃COOH + CH₃COONa)
- **Basic Buffer**: Weak base + its conjugate acid salt (e.g., NH₃ + NH₄Cl)

## Henderson-Hasselbalch Formula
`pH = pKa + log([Conjugate Base] / [Weak Acid])`

Linked from [[EUEE Chemistry Strategy]] and relates to [[Le Chatelier's Principle]].
                """.trimIndent()
            )
        )
        for (note in starterNotes) {
            val existing = noteDao.getNoteByTitle(note.title)
            if (existing == null) {
                noteDao.insertNote(note)
            }
        }
    }
}
