package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.Bookmark
import com.example.data.local.ChatHistory
import com.example.data.local.Flashcard
import com.example.data.local.OfflineNote
import com.example.data.local.Textbook
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.IqaiRepository
import com.example.logic.IqaiMatrixEngine
import com.example.data.repository.*
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main ViewModel coordinating Mastery Modules:
 * - Textbooks & Bookmarks
 * - SM-2 Active Recall Flashcards
 * - Wiki-style Markdown Notes (with FTS)
 * - Offline Intelligence Matrix & Chatbot
 * - Decoupled Domain Timetable & Custom Song Alarms
 * - Real Analytics Progress Tracking with Full CRUD
 * - Offline Dictionary with JSON Import Pipeline
 * - Question Bank Manager with Full CRUD
 */
class IqaiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    val flashcardRepository = FlashcardRepository(db.iqaiFlashcardDao())
    val iqaiRepository = IqaiRepository(
        textbookDao = db.textbookDao(),
        bookmarkDao = db.bookmarkDao(),
        noteDao = db.offlineNoteDao(),
        focusDao = db.iqaiFocusSessionDao(),
        chatDao = db.chatHistoryDao()
    )

    // Decoupled Domain Repositories
    val timetableRepository: ITimetableRepository = TimetableRepositoryImpl(db.timetablePlanDao())
    val analyticsRepository: IAnalyticsRepository = AnalyticsRepositoryImpl(db.studyMetricTrackDao())
    val dictionaryRepository: IDictionaryRepository = DictionaryRepositoryImpl(db.dictionaryEntryDao())
    val questionBankRepository: IQuestionBankRepository = QuestionBankRepositoryImpl(db.customQuestionDao())

    // ---- Textbooks & Bookmarks ----
    val allTextbooks: StateFlow<List<Textbook>> = iqaiRepository.allTextbooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTextbookId = MutableStateFlow<Int?>(null)
    val selectedTextbookId: StateFlow<Int?> = _selectedTextbookId.asStateFlow()

    fun selectTextbook(id: Int?) {
        _selectedTextbookId.value = id
    }

    fun getBookmarks(textbookId: Int): Flow<List<Bookmark>> {
        return iqaiRepository.getBookmarksForTextbook(textbookId)
    }

    fun registerTextbook(title: String, uriString: String, totalPages: Int) {
        viewModelScope.launch {
            iqaiRepository.insertTextbook(
                Textbook(
                    title = title,
                    localUri = uriString,
                    lastPageRead = 0,
                    totalPages = totalPages
                )
            )
        }
    }

    fun updateTextbookProgress(id: Int, page: Int) {
        viewModelScope.launch {
            iqaiRepository.updateLastPage(id, page)
        }
    }

    fun addBookmark(textbookId: Int, page: Int, note: String) {
        viewModelScope.launch {
            iqaiRepository.addBookmark(textbookId, page, note)
        }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch {
            iqaiRepository.deleteBookmark(id)
        }
    }

    fun deleteTextbook(id: Int) {
        viewModelScope.launch {
            iqaiRepository.deleteTextbook(id)
        }
    }

    // ---- Flashcards & SM-2 Engine ----
    val allCards: StateFlow<List<Flashcard>> = flashcardRepository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDecks: StateFlow<List<String>> = flashcardRepository.allDecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDeck = MutableStateFlow<String?>(null)
    val selectedDeck: StateFlow<String?> = _selectedDeck.asStateFlow()

    fun selectDeck(deck: String?) {
        _selectedDeck.value = deck
    }

    fun reviewCard(cardId: Int, score: Int) {
        viewModelScope.launch {
            flashcardRepository.updateCardReview(cardId, score)
        }
    }

    fun addFlashcard(deck: String, front: String, back: String, imageUri: String? = null) {
        viewModelScope.launch {
            flashcardRepository.insertCard(
                Flashcard(
                    deckName = deck.trim().ifBlank { "General" },
                    frontText = front.trim(),
                    backText = back.trim(),
                    localImageUri = imageUri
                )
            )
        }
    }

    fun deleteFlashcard(id: Int) {
        viewModelScope.launch {
            flashcardRepository.deleteCard(id)
        }
    }

    // ---- Wiki Notes & FTS Search ----
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredNotes: StateFlow<List<OfflineNote>> = _searchQuery
        .flatMapLatest { query -> iqaiRepository.searchNotes(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeNoteId = MutableStateFlow<Int?>(null)
    val activeNoteId: StateFlow<Int?> = _activeNoteId.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openNote(id: Int?) {
        _activeNoteId.value = id
    }

    fun openNoteByTitle(title: String) {
        viewModelScope.launch {
            val note = iqaiRepository.getNoteByTitle(title)
            if (note != null) {
                _activeNoteId.value = note.id
            } else {
                // Auto-create new wiki note if not found
                val newId = iqaiRepository.saveNote(
                    id = 0,
                    title = title,
                    content = "# $title\n\nStart authoring offline notes for this concept..."
                )
                _activeNoteId.value = newId.toInt()
            }
        }
    }

    fun saveNote(id: Int, title: String, content: String) {
        viewModelScope.launch {
            val savedId = iqaiRepository.saveNote(id, title, content)
            _activeNoteId.value = savedId.toInt()
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            iqaiRepository.deleteNote(id)
            if (_activeNoteId.value == id) {
                _activeNoteId.value = null
            }
        }
    }

    // ---- Mastery Chatbot & Intelligence Matrix ----
    val chatHistory: StateFlow<List<ChatHistory>> = iqaiRepository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contextMatrix = MutableStateFlow("")
    val contextMatrix: StateFlow<String> = _contextMatrix.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun refreshContextMatrix() {
        viewModelScope.launch {
            val history = iqaiRepository.getChatHistorySync()
            val notes = filteredNotes.value
            val books = allTextbooks.value
            val cards = allCards.value
            _contextMatrix.value = IqaiMatrixEngine.feedContextToMastery(history, notes, books, cards)
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val text = userText.trim()

        viewModelScope.launch {
            // 1. Log user message
            iqaiRepository.logChatMessage("user", text)

            _isGenerating.value = true

            // 2. Fetch current context snapshot
            val notes = filteredNotes.value
            val books = allTextbooks.value
            val cards = allCards.value

            // 3. Process with local offline intelligence engine
            val response = IqaiMatrixEngine.processOfflineQuery(text, notes, books, cards)

            // 4. Log assistant message
            iqaiRepository.logChatMessage(
                role = "assistant",
                content = response,
                contextSnapshot = "Context: ${notes.size} notes, ${cards.size} cards"
            )

            _isGenerating.value = false
            refreshContextMatrix()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            iqaiRepository.clearChat()
            refreshContextMatrix()
        }
    }

    // ==========================================
    // 2. TIMETABLE PLANNER & CUSTOM SONG ALARMS
    // ==========================================
    val allTimetableSlots: StateFlow<List<TimetableSlot>> = timetableRepository.getAllSlots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTimetableDay = MutableStateFlow(0) // 0 = Mon, 6 = Sun
    val selectedTimetableDay: StateFlow<Int> = _selectedTimetableDay.asStateFlow()

    fun selectTimetableDay(dayIndex: Int) {
        _selectedTimetableDay.value = dayIndex.coerceIn(0, 6)
    }

    fun saveTimetableSlot(slot: TimetableSlot) {
        viewModelScope.launch {
            timetableRepository.saveSlot(slot)
        }
    }

    fun deleteTimetableSlot(id: Int) {
        viewModelScope.launch {
            timetableRepository.deleteSlot(id)
        }
    }

    fun toggleTimetableAlarm(id: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            timetableRepository.toggleAlarm(id, isEnabled)
        }
    }

    // ==========================================
    // 2. ANALYTICS & PROGRESS TRACKING (CRUD)
    // ==========================================
    val allStudyMetrics: StateFlow<List<StudyTrackMetric>> = analyticsRepository.getAllMetrics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logStudyMetric(
        subject: String,
        focusMinutes: Int,
        quizScorePercent: Int,
        questionsSolved: Int,
        milestoneTitle: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            analyticsRepository.addMetric(
                StudyTrackMetric(
                    subject = subject,
                    focusMinutes = focusMinutes,
                    quizScorePercent = quizScorePercent,
                    questionsSolved = questionsSolved,
                    milestoneTitle = milestoneTitle,
                    notes = notes
                )
            )
        }
    }

    fun updateStudyMetric(metric: StudyTrackMetric) {
        viewModelScope.launch {
            analyticsRepository.updateMetric(metric)
        }
    }

    fun deleteStudyMetric(id: Int) {
        viewModelScope.launch {
            analyticsRepository.deleteMetric(id)
        }
    }

    // ==========================================
    // 3. OFFLINE DICTIONARY & JSON IMPORTER
    // ==========================================
    val allDictionaryTerms: StateFlow<List<DictionaryTerm>> = dictionaryRepository.getAllTerms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dictionaryQuery = MutableStateFlow("")
    val dictionaryQuery: StateFlow<String> = _dictionaryQuery.asStateFlow()

    private val _selectedDictionarySubject = MutableStateFlow("All")
    val selectedDictionarySubject: StateFlow<String> = _selectedDictionarySubject.asStateFlow()

    fun setDictionaryQuery(query: String) {
        _dictionaryQuery.value = query
    }

    fun setDictionarySubject(subject: String) {
        _selectedDictionarySubject.value = subject
    }

    fun saveDictionaryTerm(term: DictionaryTerm) {
        viewModelScope.launch {
            if (term.id == 0) {
                dictionaryRepository.insertTerm(term)
            } else {
                dictionaryRepository.updateTerm(term)
            }
        }
    }

    fun deleteDictionaryTerm(id: Int) {
        viewModelScope.launch {
            dictionaryRepository.deleteTerm(id)
        }
    }

    suspend fun importDictionaryJson(rawJson: String): Int {
        return dictionaryRepository.importJsonData(rawJson)
    }

    suspend fun importDictionaryFromStream(inputStream: java.io.InputStream): Int {
        return dictionaryRepository.importFromStream(inputStream)
    }

    // ==========================================
    // 4. QUESTION BANK MANAGEMENT (FULL CRUD)
    // ==========================================
    val allQuestions: StateFlow<List<QuestionItem>> = questionBankRepository.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveQuestion(question: QuestionItem) {
        viewModelScope.launch {
            questionBankRepository.saveQuestion(question)
        }
    }

    fun updateQuestion(question: QuestionItem) {
        viewModelScope.launch {
            questionBankRepository.updateQuestion(question)
        }
    }

    fun deleteQuestion(id: String) {
        viewModelScope.launch {
            questionBankRepository.deleteQuestion(id)
        }
    }

    suspend fun importQuestionsFromJson(
        rawJson: String,
        defaultSubject: String = "Chemistry",
        defaultGroup: String = "EUEE Questions"
    ): Int {
        return questionBankRepository.importQuestionsFromJson(rawJson, defaultSubject, defaultGroup)
    }

    suspend fun importQuestionsFromStream(
        inputStream: java.io.InputStream,
        defaultSubject: String = "Chemistry",
        defaultGroup: String = "EUEE Questions",
        onProgress: ((Int, String) -> Unit)? = null
    ): Int {
        return questionBankRepository.importQuestionsFromStream(inputStream, defaultSubject, defaultGroup, onProgress)
    }

    init {
        // Seed initial data if tables are empty
        viewModelScope.launch {
            flashcardRepository.seedInitialDecksIfEmpty()
            iqaiRepository.seedInitialNotesIfEmpty()
            questionBankRepository.seedDefaultQuestionsIfEmpty()
            seedInitialTimetableAndDictionaryIfEmpty()
            refreshContextMatrix()
        }
    }

    private suspend fun seedInitialTimetableAndDictionaryIfEmpty() {
        // Seed default timetable slots if empty
        val existingSlots = allTimetableSlots.value
        if (existingSlots.isEmpty()) {
            val defaultSlots = listOf(
                TimetableSlot(
                    dayOfWeek = 0,
                    dayName = "Monday",
                    timeSlot = "06:00 - 07:30",
                    subject = "Chemistry",
                    topic = "Chemical Kinetics & Rates",
                    notes = "Cover reaction rate laws and Arrhenius equation."
                ),
                TimetableSlot(
                    dayOfWeek = 0,
                    dayName = "Monday",
                    timeSlot = "08:00 - 09:30",
                    subject = "Physics",
                    topic = "Electromagnetism & Induction",
                    notes = "Faraday's Law, Lenz's Law, and generator calculations."
                ),
                TimetableSlot(
                    dayOfWeek = 1,
                    dayName = "Tuesday",
                    timeSlot = "06:30 - 08:00",
                    subject = "Biology",
                    topic = "Genetics & DNA Replication",
                    notes = "Mendelian ratios, Punnett squares, and transcription."
                ),
                TimetableSlot(
                    dayOfWeek = 2,
                    dayName = "Wednesday",
                    timeSlot = "07:00 - 08:30",
                    subject = "Mathematics",
                    topic = "Integration & Differential Equations",
                    notes = "Definite integrals, substitution, and parts."
                ),
                TimetableSlot(
                    dayOfWeek = 3,
                    dayName = "Thursday",
                    timeSlot = "06:00 - 07:30",
                    subject = "English",
                    topic = "Reading Comprehension & Lexis",
                    notes = "Contextual vocabulary, thesis analysis, inference."
                ),
                TimetableSlot(
                    dayOfWeek = 4,
                    dayName = "Friday",
                    timeSlot = "06:30 - 08:00",
                    subject = "Chemistry",
                    topic = "Electrochemistry & Nernst Equation",
                    notes = "Galvanic cells, standard reduction potentials."
                ),
                TimetableSlot(
                    dayOfWeek = 5,
                    dayName = "Saturday",
                    timeSlot = "08:00 - 11:00",
                    subject = "Mock Exam",
                    topic = "Full EUEE Natural Science Mock Simulation",
                    notes = "Timed session under standard entrance exam conditions."
                ),
                TimetableSlot(
                    dayOfWeek = 6,
                    dayName = "Sunday",
                    timeSlot = "09:00 - 10:30",
                    subject = "Active Recall",
                    topic = "Weekly SRS Review & Weak Concept Remediation",
                    notes = "Review all lapse cards and missed questions."
                )
            )
            defaultSlots.forEach { timetableRepository.saveSlot(it) }
        }

        // Seed initial dictionary terms if empty
        val existingTerms = allDictionaryTerms.value
        if (existingTerms.isEmpty()) {
            val defaultTerms = listOf(
                DictionaryTerm(
                    term = "Entropy (S)",
                    definition = "A thermodynamic measure of the molecular disorder, randomness, or thermal energy unavailable for useful work in a system.",
                    subject = "Chemistry",
                    partOfSpeech = "noun",
                    phonetic = "/ˈɛntrəpi/",
                    exampleUsage = "The second law of thermodynamics asserts that the total entropy of an isolated system always increases.",
                    synonyms = listOf("Disorder", "Randomness", "Thermal dissipation")
                ),
                DictionaryTerm(
                    term = "Faraday's Law",
                    definition = "The magnitude of the induced electromotive force (EMF) in a circuit is directly proportional to the time rate of change of magnetic flux through the circuit.",
                    subject = "Physics",
                    partOfSpeech = "noun / principle",
                    phonetic = "/ˈfærəˌdeɪz lɔː/",
                    exampleUsage = "Generators and transformers operate on the fundamental principles of Faraday's Law of induction.",
                    synonyms = listOf("Electromagnetic induction", "Flux linkage")
                ),
                DictionaryTerm(
                    term = "Allosteric Regulation",
                    definition = "The modification of an enzyme's catalytic activity by the binding of an effector molecule at a site distinct from the active site.",
                    subject = "Biology",
                    partOfSpeech = "noun",
                    phonetic = "/ˌæləˈstɛrɪk/",
                    exampleUsage = "Feedback inhibition often operates through allosteric regulation of the initial pathway enzyme.",
                    synonyms = listOf("Non-competitive modulation", "Effector binding")
                ),
                DictionaryTerm(
                    term = "Eigenvalue",
                    definition = "A scalar lambda (λ) associated with a square matrix A such that there exists a non-zero vector v with Av = λv.",
                    subject = "Mathematics",
                    partOfSpeech = "noun",
                    phonetic = "/ˈaɪɡənˌvæljuː/",
                    exampleUsage = "Eigenvalues determine stability in systems of linear differential equations.",
                    synonyms = listOf("Characteristic value", "Latent root")
                ),
                DictionaryTerm(
                    term = "Paradox",
                    definition = "A seemingly contradictory or absurd statement that when investigated or explained may prove to be well founded or true.",
                    subject = "English",
                    partOfSpeech = "noun",
                    phonetic = "/ˈpærədɒks/",
                    exampleUsage = "The speaker employed irony and paradox to highlight the underlying moral ambiguity.",
                    synonyms = listOf("Contradiction", "Enigma", "Antinomy")
                )
            )
            dictionaryRepository.insertTerms(defaultTerms)
        }

        // Seed initial study metric track if empty
        val existingMetrics = allStudyMetrics.value
        if (existingMetrics.isEmpty()) {
            analyticsRepository.addMetric(
                StudyTrackMetric(
                    subject = "Chemistry",
                    focusMinutes = 90,
                    quizScorePercent = 95,
                    questionsSolved = 30,
                    milestoneTitle = "Equilibrium & Kinetics Mastery",
                    notes = "Finished all 2012-2016 EUEE questions with zero errors on Le Chatelier."
                )
            )
            analyticsRepository.addMetric(
                StudyTrackMetric(
                    subject = "Physics",
                    focusMinutes = 75,
                    quizScorePercent = 88,
                    questionsSolved = 25,
                    milestoneTitle = "Electromagnetism Circuit Analysis",
                    notes = "Calculated induced EMF across rotating coils accurately."
                )
            )
            analyticsRepository.addMetric(
                StudyTrackMetric(
                    subject = "Mathematics",
                    focusMinutes = 110,
                    quizScorePercent = 92,
                    questionsSolved = 40,
                    milestoneTitle = "Calculus Derivatives & Integrals",
                    notes = "Completed chain rule and substitution problems."
                )
            )
        }
    }
}
