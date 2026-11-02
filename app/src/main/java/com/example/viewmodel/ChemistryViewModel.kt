package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChemistryRepository
import com.example.data.PeriodicTable
import com.example.data.SampleData
import com.example.data.local.*
import com.example.logic.ChemistryLogic
import com.example.model.*
import com.example.player.MusicPlayerManager
import com.example.util.AlarmHelper
import com.example.util.AudioHapticHelper
import com.example.util.DndHelper
import com.example.util.ZipBackupHelper
import com.example.util.ZipRestorePreview
import org.json.JSONArray
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserQuestionAnswer(
    var ans: String = "",
    var done: Boolean = false,
    var ok: Boolean = false,
    var c: Int = 2, // 1: Unsure, 2: Fairly sure, 3: Sure
    var h: Int = 0, // hints count
    var shown: Boolean = false, // for flashcard / active recall flip
    var hx: Boolean = false, // show explanation toggle
    var t0: Long = System.currentTimeMillis(),
    var s: Int = 0, // time in seconds
    var dx: String? = null // diagnosis reason
)

data class StudySessionSummary(
    val at: Long = System.currentTimeMillis(),
    val ok: Int,
    val answered: Int,
    val total: Int,
    val secs: Int,
    val missedIds: List<String>
)

data class ActiveStudySession(
    val mode: String, // "practice", "recall", "flash", "quiz", "exam", "review", "sprint"
    val title: String,
    val bankId: String,
    val questionIds: List<String>,
    var currentIndex: Int = 0,
    val answers: MutableMap<String, UserQuestionAnswer> = mutableMapOf(),
    val optionsCache: MutableMap<String, List<String>> = mutableMapOf(),
    var endTimestamp: Long = 0L,
    val startTimestamp: Long = System.currentTimeMillis(),
    var isFinished: Boolean = false,
    var summary: StudySessionSummary? = null,
    val isSprint: Boolean = false
)

class ChemistryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChemistryRepository(application)
    private val audioHaptic = AudioHapticHelper(application)
    private val db = AppDatabase.getDatabase(application)
    val musicPlayerManager = MusicPlayerManager(application)

    val banks = repository.banks
    val progress = repository.progress
    val bookmarks = repository.bookmarks
    val srs = repository.srs
    val attempts = repository.attempts
    val sessions = repository.sessions
    val exams = repository.exams
    val prefs = repository.prefs
    val trash = repository.trash
    val notes = repository.notes
    val plannerTasks = repository.plannerTasks
    val flashcards = repository.flashcards

    // Room Database flows
    val pdfDocuments: StateFlow<List<PdfDocumentEntity>> = db.pdfDocumentDao().getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyAlarms: StateFlow<List<StudyAlarmEntity>> = db.studyAlarmDao().getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioTracks: StateFlow<List<AudioTrackEntity>> = db.audioTrackDao().getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusHistory: StateFlow<List<FocusSessionEntity>> = db.focusSessionDao().getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Foundational Mastery Room flows for Master Data Dashboard
    val allTextbooks: StateFlow<List<Textbook>> = db.textbookDao().getAllTextbooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<Bookmark>> = db.bookmarkDao().getBookmarksForTextbook(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIqaiCards: StateFlow<List<Flashcard>> = db.iqaiFlashcardDao().getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOfflineNotes: StateFlow<List<OfflineNote>> = db.offlineNoteDao().getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIqaiSessions: StateFlow<List<FocusSession>> = db.iqaiFocusSessionDao().getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimetablePlans: StateFlow<List<TimetablePlanEntity>> = db.timetablePlanDao().getAllPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMetricTracks: StateFlow<List<StudyMetricTrackEntity>> = db.studyMetricTrackDao().getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDictEntries: StateFlow<List<DictionaryEntryEntity>> = db.dictionaryEntryDao().getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomQuestions: StateFlow<List<CustomQuestionEntity>> = db.customQuestionDao().getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun purgeTable(tableName: String) {
        viewModelScope.launch {
            when (tableName) {
                "textbooks" -> db.textbookDao().clearAll()
                "bookmarks" -> db.bookmarkDao().clearAll()
                "iqai_flashcards" -> db.iqaiFlashcardDao().clearAll()
                "offline_notes" -> db.offlineNoteDao().clearAll()
                "iqai_focus_sessions" -> db.iqaiFocusSessionDao().clearAll()
                "timetable_plans" -> db.timetablePlanDao().clearAll()
                "study_metric_tracks" -> db.studyMetricTrackDao().clearAll()
                "dictionary_entries" -> db.dictionaryEntryDao().clearAll()
                "custom_mcq_questions" -> db.customQuestionDao().clearAll()
            }
        }
    }

    fun exportAllTablesJson(): String {
        val root = org.json.JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("app", "Mastery")
        root.put("developer", "Alexander Fiseha")

        val booksArr = org.json.JSONArray()
        allTextbooks.value.forEach { b ->
            val o = org.json.JSONObject()
            o.put("id", b.id)
            o.put("title", b.title)
            o.put("localUri", b.localUri)
            o.put("lastPageRead", b.lastPageRead)
            booksArr.put(o)
        }
        root.put("textbooks", booksArr)

        val flashcardsArr = org.json.JSONArray()
        allIqaiCards.value.forEach { c ->
            val o = org.json.JSONObject()
            o.put("id", c.id)
            o.put("deckName", c.deckName)
            o.put("frontText", c.frontText)
            o.put("backText", c.backText)
            o.put("dueDate", c.dueDate)
            flashcardsArr.put(o)
        }
        root.put("flashcards", flashcardsArr)

        val notesArr = org.json.JSONArray()
        allOfflineNotes.value.forEach { n ->
            val o = org.json.JSONObject()
            o.put("title", n.title)
            o.put("content", n.content)
            o.put("lastModified", n.lastModified)
            notesArr.put(o)
        }
        root.put("notes", notesArr)

        val timetableArr = org.json.JSONArray()
        allTimetablePlans.value.forEach { t ->
            val o = org.json.JSONObject()
            o.put("id", t.id)
            o.put("dayOfWeek", t.dayOfWeek)
            o.put("dayName", t.dayName)
            o.put("subject", t.subject)
            o.put("timeSlot", t.timeSlot)
            o.put("topic", t.topic)
            timetableArr.put(o)
        }
        root.put("timetable_plans", timetableArr)

        val metricsArr = org.json.JSONArray()
        allMetricTracks.value.forEach { m ->
            val o = org.json.JSONObject()
            o.put("id", m.id)
            o.put("subject", m.subject)
            o.put("quizScorePercent", m.quizScorePercent)
            o.put("focusMinutes", m.focusMinutes)
            o.put("notes", m.notes)
            metricsArr.put(o)
        }
        root.put("study_metrics", metricsArr)

        val dictArr = org.json.JSONArray()
        allDictEntries.value.forEach { d ->
            val o = org.json.JSONObject()
            o.put("term", d.term)
            o.put("definition", d.definition)
            o.put("subject", d.subject)
            dictArr.put(o)
        }
        root.put("dictionary_entries", dictArr)

        val qArr = org.json.JSONArray()
        allCustomQuestions.value.forEach { q ->
            val o = org.json.JSONObject()
            o.put("id", q.id)
            o.put("subject", q.subject)
            o.put("chapter", q.chapter)
            o.put("prompt", q.prompt)
            val options = org.json.JSONArray().apply {
                put(q.optionA)
                put(q.optionB)
                put(q.optionC)
                put(q.optionD)
            }
            o.put("options", options)
            o.put("correctOptionIndex", q.correctOptionIndex)
            o.put("explanation", q.explanation)
            qArr.put(o)
        }
        root.put("custom_questions", qArr)

        return root.toString(2)
    }

    val activeBank: StateFlow<QuestionBank> = combine(banks, prefs) { bankList, pref ->
        bankList.find { it.id == pref.bank } ?: bankList.firstOrNull() ?: SampleData.createSampleBank()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.createSampleBank())

    private val _currentNav = MutableStateFlow("dash")
    val currentNav: StateFlow<String> = _currentNav.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveStudySession?>(null)
    val activeSession: StateFlow<ActiveStudySession?> = _activeSession.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    // Pomodoro & Focus state
    private val _pomodoroSeconds = MutableStateFlow(1500)
    val pomodoroSeconds: StateFlow<Int> = _pomodoroSeconds.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    private val _isPomodoroBreak = MutableStateFlow(false)
    val isPomodoroBreak: StateFlow<Boolean> = _isPomodoroBreak.asStateFlow()

    private val _dailyFocusGoalMinutes = MutableStateFlow(120)
    val dailyFocusGoalMinutes: StateFlow<Int> = _dailyFocusGoalMinutes.asStateFlow()

    private val _todayCompletedFocusMinutes = MutableStateFlow(0)
    val todayCompletedFocusMinutes: StateFlow<Int> = _todayCompletedFocusMinutes.asStateFlow()

    private val _distractionLogs = MutableStateFlow<List<String>>(emptyList())
    val distractionLogs: StateFlow<List<String>> = _distractionLogs.asStateFlow()

    private var previousDndFilter: Int = -1
    private var pomodoroJob: Job? = null
    private var examTimerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.load()
            AlarmHelper.initNotificationChannels(application)
        }
    }

    fun showToast(msg: String) {
        _toast.value = msg
    }

    fun clearToast() {
        _toast.value = null
    }

    fun navigateTo(navId: String) {
        _currentNav.value = navId
    }

    // Focus & Pomodoro controls
    fun togglePomodoro() {
        if (_isPomodoroRunning.value) {
            _isPomodoroRunning.value = false
            pomodoroJob?.cancel()
        } else {
            _isPomodoroRunning.value = true
            pomodoroJob = viewModelScope.launch {
                while (_pomodoroSeconds.value > 0 && _isPomodoroRunning.value) {
                    delay(1000)
                    _pomodoroSeconds.value -= 1
                }
                if (_pomodoroSeconds.value <= 0) {
                    _isPomodoroRunning.value = false
                    audioHaptic.playFeedback(isCorrect = true)
                    audioHaptic.vibrateAlert()
                    if (!_isPomodoroBreak.value) {
                        // Focus completed -> switch to break
                        _todayCompletedFocusMinutes.value += 25
                        _isPomodoroBreak.value = true
                        _pomodoroSeconds.value = 300 // 5 min break
                        showToast("🎉 Great focus session! Time for a 5-minute break.")
                        // Record focus session to Room
                        db.focusSessionDao().insert(
                            FocusSessionEntity(
                                durationMinutes = 25,
                                distractions = _distractionLogs.value.joinToString("\n")
                            )
                        )
                        deactivateDnd()
                    } else {
                        // Break completed -> switch to focus
                        _isPomodoroBreak.value = false
                        _pomodoroSeconds.value = 1500
                        showToast("Break over. Ready for your next focus session?")
                    }
                }
            }
        }
    }

    fun resetPomodoro() {
        _isPomodoroRunning.value = false
        pomodoroJob?.cancel()
        _isPomodoroBreak.value = false
        _pomodoroSeconds.value = 1500
    }

    fun logDistraction(note: String) {
        _distractionLogs.value = _distractionLogs.value + note
    }

    fun clearDistractions() {
        _distractionLogs.value = emptyList()
    }

    fun activateDnd() {
        previousDndFilter = DndHelper.enableDnd(getApplication())
    }

    fun deactivateDnd() {
        if (previousDndFilter != -1) {
            DndHelper.restoreDnd(getApplication(), previousDndFilter)
            previousDndFilter = -1
        }
    }

    // Question Filtering Helpers
    fun getFilteredQuestions(filterType: String): List<Question> {
        val qList = activeBank.value.questions
        val now = System.currentTimeMillis()
        val srsMap = srs.value
        val progMap = progress.value
        val bmSet = bookmarks.value

        return when (filterType) {
            "due" -> qList.filter { q -> srsMap[q.id]?.due?.let { it <= now } ?: false }
            "new" -> qList.filter { q -> (progMap[q.id]?.att ?: 0) == 0 }
            "marked" -> qList.filter { q -> bmSet.contains(q.id) }
            "wrong" -> qList.filter { q ->
                val p = progMap[q.id]
                p != null && p.nb && p.state != "mastered"
            }
            "r10" -> qList.shuffled().take(10)
            "leech" -> qList.filter { q ->
                val p = progMap[q.id]
                p != null && p.bad >= 3 && p.state != "mastered"
            }
            "weak" -> {
                val topicAcc = qList.groupBy { it.topic }.mapValues { (_, list) ->
                    var att = 0
                    var ok = 0
                    list.forEach { q ->
                        val p = progMap[q.id]
                        if (p != null) {
                            att += p.att
                            ok += p.ok
                        }
                    }
                    if (att > 0) ok.toDouble() / att else 1.0
                }
                val weakest = topicAcc.entries.sortedBy { it.value }.take(3).map { it.key }.toSet()
                qList.filter { weakest.contains(it.topic) && progMap[it.id]?.state != "mastered" }
            }
            "smart" -> {
                val cap = prefs.value.ncap
                val due = qList.filter { q -> srsMap[q.id]?.due?.let { it <= now } ?: false }
                    .sortedBy { srsMap[it.id]?.due ?: 0L }
                val wrong = qList.filter { q -> progMap[q.id]?.let { it.nb && it.state != "mastered" } ?: false }
                val newQ = qList.filter { q -> (progMap[q.id]?.att ?: 0) == 0 }.shuffled().take(cap)

                val result = LinkedHashSet<Question>()
                result.addAll(due)
                result.addAll(wrong)
                result.addAll(newQ)
                if (result.isEmpty()) qList.shuffled().take(10) else result.take(20).toList()
            }
            else -> qList
        }
    }

    // Study Workspace actions
    fun startStudy(mode: String, questions: List<Question>, title: String, isOrdered: Boolean = false, secondsPerQ: Int = 60) {
        if (questions.isEmpty()) {
            showToast("No questions match this selection yet.")
            return
        }

        examTimerJob?.cancel()

        val list = if (isOrdered) questions else questions.shuffled()
        val isExam = mode == "exam"
        val isSprint = mode == "sprint"
        val end = when {
            isSprint -> System.currentTimeMillis() + 60000L
            isExam -> System.currentTimeMillis() + (list.size * secondsPerQ * 1000L)
            else -> 0L
        }

        val session = ActiveStudySession(
            mode = if (isSprint) "quiz" else mode,
            title = title,
            bankId = activeBank.value.id,
            questionIds = list.map { it.id },
            endTimestamp = end,
            isSprint = isSprint
        )
        _activeSession.value = session

        if (end > 0L) {
            examTimerJob = viewModelScope.launch {
                while (_activeSession.value != null && !_activeSession.value!!.isFinished) {
                    val remaining = _activeSession.value!!.endTimestamp - System.currentTimeMillis()
                    if (remaining <= 0) {
                        finishStudySession()
                        break
                    }
                    delay(1000)
                }
            }
        }
    }

    fun exitStudySession() {
        examTimerJob?.cancel()
        _activeSession.value = null
    }

    fun currentQuestion(): Question? {
        val s = _activeSession.value ?: return null
        if (s.currentIndex !in s.questionIds.indices) return null
        val qid = s.questionIds[s.currentIndex]
        return activeBank.value.questions.find { it.id == qid }
    }

    fun currentAnswer(): UserQuestionAnswer {
        val s = _activeSession.value ?: return UserQuestionAnswer()
        val q = currentQuestion() ?: return UserQuestionAnswer()
        return s.answers.getOrPut(q.id) { UserQuestionAnswer() }
    }

    fun getOptionsForQuestion(q: Question): List<String> {
        val s = _activeSession.value ?: return ChemistryLogic.buildOpts(q, activeBank.value.questions)
        return s.optionsCache.getOrPut(q.id) {
            ChemistryLogic.buildOpts(q, activeBank.value.questions)
        }
    }

    fun selectOption(optionText: String) {
        val s = _activeSession.value ?: return
        val q = currentQuestion() ?: return
        val a = currentAnswer()
        if (a.done && s.mode != "exam") return
        a.ans = optionText
        _activeSession.value = s.copy()

        if (s.mode == "quiz" && !s.isSprint) {
            checkCurrentAnswer()
        } else if (s.isSprint) {
            checkCurrentAnswer()
            viewModelScope.launch {
                delay(650)
                nextQuestion()
            }
        }
    }

    fun updateTypedAnswer(text: String) {
        val a = currentAnswer()
        if (a.done && _activeSession.value?.mode != "exam") return
        a.ans = text
        _activeSession.value = _activeSession.value?.copy()
    }

    fun setConfidence(conf: Int) {
        val a = currentAnswer()
        a.c = conf
        _activeSession.value = _activeSession.value?.copy()
    }

    fun toggleFlip() {
        val a = currentAnswer()
        a.shown = !a.shown
        _activeSession.value = _activeSession.value?.copy()
    }

    fun toggleExplanation() {
        val a = currentAnswer()
        a.hx = !a.hx
        _activeSession.value = _activeSession.value?.copy()
    }

    fun useHint() {
        val q = currentQuestion() ?: return
        val a = currentAnswer()
        if (a.h < q.hints.size) {
            a.h += 1
            _activeSession.value = _activeSession.value?.copy()
        } else {
            showToast("No more hints for this question.")
        }
    }

    fun checkCurrentAnswer() {
        val s = _activeSession.value ?: return
        val q = currentQuestion() ?: return
        val a = currentAnswer()
        if (a.done && s.mode != "exam") return

        val text = a.ans.trim()
        if (text.isEmpty()) {
            showToast("Type or choose an answer first.")
            return
        }

        val isOk = ChemistryLogic.okOf(q, text)
        a.done = true
        a.ok = isOk
        a.s = ((System.currentTimeMillis() - a.t0) / 1000).toInt().coerceAtMost(600)

        if (prefs.value.snd) {
            audioHaptic.playSound(isOk)
            audioHaptic.vibrate(isOk)
        }

        viewModelScope.launch {
            repository.recordAttempt(
                question = q,
                ok = isOk,
                userAns = text,
                confidence = a.c,
                mode = s.mode,
                hintsUsed = a.h,
                secondsTaken = a.s
            )
        }

        _activeSession.value = s.copy()
    }

    fun revealAnswer() {
        val s = _activeSession.value ?: return
        val q = currentQuestion() ?: return
        val a = currentAnswer()
        if (a.done) {
            a.hx = !a.hx
            _activeSession.value = s.copy()
            return
        }

        a.done = true
        a.ok = false
        a.ans = "(answer revealed)"
        a.shown = true
        a.s = ((System.currentTimeMillis() - a.t0) / 1000).toInt().coerceAtMost(600)

        if (prefs.value.snd) {
            audioHaptic.playSound(false)
            audioHaptic.vibrate(false)
        }

        viewModelScope.launch {
            repository.recordAttempt(
                question = q,
                ok = false,
                userAns = a.ans,
                confidence = a.c,
                mode = s.mode,
                hintsUsed = a.h,
                secondsTaken = a.s
            )
        }

        _activeSession.value = s.copy()
    }

    fun rateRecall(score: Int) {
        // 0: Missed it, 1: Got it unsure, 2: Got it sure
        val s = _activeSession.value ?: return
        val q = currentQuestion() ?: return
        val a = currentAnswer()

        val isOk = score > 0
        a.c = if (score == 2) 3 else 1
        a.done = true
        a.ok = isOk
        a.ans = if (isOk) "(self-rated correct)" else "(self-rated missed)"
        a.s = ((System.currentTimeMillis() - a.t0) / 1000).toInt().coerceAtMost(600)

        if (prefs.value.snd) {
            audioHaptic.playSound(isOk)
            audioHaptic.vibrate(isOk)
        }

        viewModelScope.launch {
            repository.recordAttempt(
                question = q,
                ok = isOk,
                userAns = a.ans,
                confidence = a.c,
                mode = s.mode,
                hintsUsed = a.h,
                secondsTaken = a.s
            )
        }

        _activeSession.value = s.copy()
    }

    fun recordDiagnosis(diagKey: String) {
        val s = _activeSession.value ?: return
        val q = currentQuestion() ?: return
        val a = currentAnswer()
        a.dx = diagKey
        _activeSession.value = s.copy()
    }

    fun retryCurrentQuestion() {
        val s = _activeSession.value ?: return
        val q = currentQuestion() ?: return
        s.answers.remove(q.id)
        _activeSession.value = s.copy()
    }

    fun nextQuestion() {
        val s = _activeSession.value ?: return
        if (s.currentIndex < s.questionIds.size - 1) {
            s.currentIndex += 1
            val a = currentAnswer()
            a.t0 = System.currentTimeMillis()
            _activeSession.value = s.copy()
        } else {
            finishStudySession()
        }
    }

    fun prevQuestion() {
        val s = _activeSession.value ?: return
        if (s.currentIndex > 0) {
            s.currentIndex -= 1
            _activeSession.value = s.copy()
        }
    }

    fun finishStudySession() {
        val s = _activeSession.value ?: return
        if (s.isFinished) return
        examTimerJob?.cancel()

        val qMap = activeBank.value.questions.associateBy { it.id }
        var correctCount = 0
        var answeredCount = 0
        val missedList = mutableListOf<String>()

        val examRows = mutableListOf<ExamResult.ExamRow>()

        s.questionIds.forEach { id ->
            val a = s.answers[id]
            val q = qMap[id]

            if (s.mode == "exam" && a != null && !a.done && a.ans.isNotBlank() && q != null) {
                a.done = true
                a.ok = ChemistryLogic.okOf(q, a.ans)
                viewModelScope.launch {
                    repository.recordAttempt(
                        question = q,
                        ok = a.ok,
                        userAns = a.ans,
                        confidence = a.c,
                        mode = "exam",
                        hintsUsed = 0,
                        secondsTaken = a.s
                    )
                }
            }

            if (a != null && a.done) {
                answeredCount++
                if (a.ok) correctCount++ else missedList.add(id)
            } else {
                missedList.add(id)
            }

            if (s.mode == "exam") {
                examRows.add(ExamResult.ExamRow(id = id, ok = if (a?.ok == true) 1 else 0, ans = a?.ans ?: ""))
            }
        }

        val totalSecs = ((System.currentTimeMillis() - s.startTimestamp) / 1000).toInt()
        val sum = StudySessionSummary(
            ok = correctCount,
            answered = answeredCount,
            total = s.questionIds.size,
            secs = totalSecs,
            missedIds = missedList
        )

        s.isFinished = true
        s.summary = sum

        viewModelScope.launch {
            repository.recordSession(
                SessionLog(
                    mode = s.mode,
                    title = s.title,
                    bank = s.bankId,
                    ok = correctCount,
                    ans = answeredCount,
                    total = s.questionIds.size,
                    secs = totalSecs
                )
            )

            if (s.mode == "exam") {
                repository.recordExam(
                    ExamResult(
                        bank = s.bankId,
                        total = s.questionIds.size,
                        ok = correctCount,
                        ans = answeredCount,
                        secs = totalSecs,
                        rows = examRows
                    )
                )
            }
        }

        _activeSession.value = s.copy()
    }

    fun retryMissedInSession() {
        val s = _activeSession.value ?: return
        val missed = s.summary?.missedIds ?: return
        val qList = activeBank.value.questions.filter { missed.contains(it.id) }
        exitStudySession()
        startStudy("practice", qList, "Retry Missed (${qList.size})")
    }

    // Question & Bank CRUD
    fun toggleBookmark(questionId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(questionId)
        }
    }

    fun saveQuestion(
        existing: Question?,
        prompt: String,
        answers: List<String>,
        options: List<String>,
        topic: String,
        difficulty: String,
        type: String,
        tol: Double?,
        explanation: String?,
        memoryTip: String?,
        hints: List<String>,
        img: String?,
        targetBankId: String,
        alsoReverse: Boolean
    ) {
        viewModelScope.launch {
            val uid = "u_" + java.lang.Long.toString(System.currentTimeMillis(), 36)
            // Check cloze syntax [[word]]
            val clozeMatch = Regex("\\[\\[(.+?)\\]\\]").findAll(prompt).toList()

            val questionsToSave = mutableListOf<Question>()

            if (clozeMatch.isNotEmpty()) {
                clozeMatch.forEachIndexed { i, match ->
                    var k = -1
                    val blankedPrompt = prompt.replace(Regex("\\[\\[(.+?)\\]\\]")) { m ->
                        k++
                        if (k == i) "_____" else m.groupValues[1]
                    }
                    val ansWord = match.groupValues[1]
                    questionsToSave.add(
                        Question(
                            id = if (existing != null && clozeMatch.size == 1) existing.id else "${uid}_$i",
                            prompt = blankedPrompt,
                            answers = listOf(ansWord),
                            options = emptyList(),
                            topic = topic.ifBlank { "General" },
                            difficulty = difficulty,
                            type = type,
                            tol = tol,
                            explanation = explanation,
                            memoryTip = memoryTip,
                            hints = hints,
                            img = img
                        )
                    )
                }
            } else {
                questionsToSave.add(
                    Question(
                        id = existing?.id ?: uid,
                        prompt = prompt,
                        answers = answers,
                        options = options,
                        topic = topic.ifBlank { "General" },
                        difficulty = difficulty,
                        type = type,
                        tol = tol,
                        explanation = explanation,
                        memoryTip = memoryTip,
                        hints = hints,
                        img = img
                    )
                )
            }

            if (alsoReverse && clozeMatch.isEmpty() && answers.isNotEmpty()) {
                questionsToSave.add(
                    Question(
                        id = "${uid}_rev",
                        prompt = answers[0],
                        answers = listOf(prompt),
                        options = emptyList(),
                        topic = topic.ifBlank { "General" },
                        difficulty = difficulty,
                        type = "text",
                        explanation = explanation,
                        memoryTip = memoryTip,
                        hints = hints
                    )
                )
            }

            questionsToSave.forEach { q ->
                repository.saveQuestion(q, targetBankId)
            }
            showToast("Saved ${questionsToSave.size} question${if (questionsToSave.size > 1) "s" else ""}.")
        }
    }

    fun deleteQuestion(q: Question) {
        viewModelScope.launch {
            repository.deleteQuestion(q, activeBank.value.id)
            showToast("Deleted question. Tap 'Restore' to undo.")
        }
    }

    fun restoreLastDeleted() {
        viewModelScope.launch {
            val restored = repository.restoreLastDeleted()
            if (restored) showToast("Question restored.") else showToast("Nothing to restore.")
        }
    }

    fun selectBank(bankId: String) {
        viewModelScope.launch {
            repository.selectBank(bankId)
        }
    }

    fun createBank(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newBank = QuestionBank(
                id = "bank_" + java.lang.Long.toString(System.currentTimeMillis(), 36),
                name = name.trim(),
                at = System.currentTimeMillis(),
                questions = emptyList()
            )
            repository.addBank(newBank)
            showToast("Created bank '$name'.")
        }
    }

    fun deleteCurrentBank() {
        viewModelScope.launch {
            val current = activeBank.value
            val ok = repository.deleteBank(current.id)
            if (ok) showToast("Bank '${current.name}' deleted.") else showToast("Cannot delete the only bank.")
        }
    }

    fun mergeBanks(sourceBankId: String) {
        viewModelScope.launch {
            repository.mergeBanks(sourceBankId, activeBank.value.id)
            showToast("Banks merged successfully.")
        }
    }

    // Notebook actions
    fun toggleMastered(questionId: String) {
        val current = progress.value[questionId]
        val newState = if (current?.state == "mastered") "learning" else "mastered"
        viewModelScope.launch {
            repository.setMasteryState(questionId, newState)
        }
    }

    fun removeFromNotebook(questionId: String) {
        viewModelScope.launch {
            repository.setNotebookFlag(questionId, false)
            showToast("Removed from notebook.")
        }
    }

    fun clearNotebook() {
        viewModelScope.launch {
            progress.value.forEach { (qid, p) ->
                if (p.nb) repository.setNotebookFlag(qid, false)
            }
            showToast("Notebook cleared.")
        }
    }

    // Preferences
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(theme = theme))
        }
    }

    fun updateAccent(accent: String) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(accent = accent))
        }
    }

    fun updateGoal(goal: Int) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(goal = goal))
        }
    }

    fun updateExamDate(dateStr: String) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(exd = dateStr))
        }
    }

    fun updateExamSeconds(secs: Int) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(exs = secs))
        }
    }

    fun updateSound(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(snd = enabled))
        }
    }

    fun updateDailyMinutes(mins: Int) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(mins = mins))
        }
    }

    fun updateNewCap(ncap: Int) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(ncap = ncap))
        }
    }

    fun updateFont(font: String) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(selectedFont = font))
            showToast("Font updated to $font")
        }
    }

    fun updateDashboardConfig(configJson: String) {
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(dashboardConfigJson = configJson))
        }
    }

    fun updatePriority(bankId: String, pri: Int) {
        val newMap = prefs.value.pri.toMutableMap()
        newMap[bankId] = pri
        viewModelScope.launch {
            repository.updatePreferences(prefs.value.copy(pri = newMap))
        }
    }

    fun updateStudentProfile(name: String, grade: String, phone: String, email: String) {
        viewModelScope.launch {
            repository.updatePreferences(
                prefs.value.copy(
                    studentName = name,
                    studentGrade = grade,
                    studentPhone = phone,
                    studentEmail = email
                )
            )
            showToast("Student profile saved!")
        }
    }

    // Import & Export
    fun applyImport(result: ChemistryLogic.AnalyzeResult, mode: String, newBankName: String) {
        viewModelScope.launch {
            when (mode) {
                "merge" -> {
                    val currentQ = activeBank.value.questions
                    val plan = ChemistryLogic.plan(result.ok, currentQ)
                    val map = plan.rep.associateBy { it.id }
                    val merged = currentQ.map { map[it.id] ?: it } + plan.add
                    repository.saveQuestion(merged[0], activeBank.value.id) // update bank questions
                    activeBank.value.copy(questions = merged).let { updated ->
                        repository.addBank(updated) // updates or replaces bank
                    }
                    showToast("Merged ${plan.add.size} added, ${plan.rep.size} replaced, ${plan.skip.size} skipped.")
                }
                "replace" -> {
                    val updated = activeBank.value.copy(questions = result.ok)
                    repository.addBank(updated)
                    showToast("Replaced with ${result.ok.size} questions.")
                }
                "new" -> {
                    val bName = newBankName.ifBlank { "Imported " + SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                    val newBank = QuestionBank(
                        id = "bank_" + java.lang.Long.toString(System.currentTimeMillis(), 36),
                        name = bName,
                        at = System.currentTimeMillis(),
                        questions = result.ok
                    )
                    repository.addBank(newBank)
                    showToast("Created bank '$bName' with ${result.ok.size} questions.")
                }
            }
            navigateTo("lib")
        }
    }

    // Workspace actions
    fun saveNote(note: StudyNote) {
        viewModelScope.launch {
            repository.saveNote(note)
            showToast("Note saved.")
        }
    }

    fun saveStudyNote(title: String, subject: String, body: String) {
        val note = StudyNote(
            id = "n_" + java.lang.Long.toString(System.currentTimeMillis(), 36),
            title = title,
            subject = subject,
            body = body
        )
        saveNote(note)
    }

    suspend fun createZipArchive(
        incNotes: Boolean,
        incTasks: Boolean,
        incCards: Boolean,
        incBanks: Boolean,
        incProgress: Boolean,
        incAlarms: Boolean
    ): File {
        return ZipBackupHelper.createZipBackup(
            context = getApplication(),
            repository = repository,
            alarms = studyAlarms.value,
            includeNotes = incNotes,
            includeTasks = incTasks,
            includeCards = incCards,
            includeBanks = incBanks,
            includeProgress = incProgress,
            includeAlarms = incAlarms
        )
    }

    suspend fun applyZipRestore(preview: ZipRestorePreview, mode: String): Boolean {
        return ZipBackupHelper.applyRestore(repository, preview, mode)
    }

    fun addStudyAlarm(alarm: StudyAlarmEntity) {
        viewModelScope.launch {
            db.studyAlarmDao().insert(alarm)
            AlarmHelper.scheduleAlarm(getApplication(), alarm)
            showToast(String.format(Locale.US, "Alarm set for %02d:%02d", alarm.hour, alarm.minute))
        }
    }

    fun toggleAlarmEnabled(alarmId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val alarms = studyAlarms.value
            val existing = alarms.find { it.id == alarmId } ?: return@launch
            val updated = existing.copy(isEnabled = isEnabled)
            db.studyAlarmDao().insert(updated)
            if (isEnabled) {
                AlarmHelper.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmHelper.cancelAlarm(getApplication(), alarmId)
            }
        }
    }

    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch {
            AlarmHelper.cancelAlarm(getApplication(), alarmId)
            db.studyAlarmDao().deleteById(alarmId)
            showToast("Alarm deleted")
        }
    }

    fun ensureEueeSubjectBanksLoaded() {
        viewModelScope.launch {
            repository.ensureEueeSubjectBanksLoaded()
            showToast("EUEE Subject Banks Loaded!")
        }
    }

    fun addAudioTrack(
        title: String,
        artist: String,
        uriString: String,
        durationMs: Long,
        playlist: String = "My Music"
    ) {
        viewModelScope.launch {
            val track = AudioTrackEntity(
                id = "track_" + java.lang.Long.toString(System.currentTimeMillis(), 36),
                title = title,
                artist = artist,
                uriString = uriString,
                durationMs = durationMs,
                playlist = playlist,
                isBuiltIn = false
            )
            db.audioTrackDao().insert(track)
        }
    }

    fun deleteAudioTrack(trackId: String) {
        viewModelScope.launch {
            db.audioTrackDao().deleteById(trackId)
        }
    }

    fun updateAudioTrackPlaylist(trackId: String, newPlaylist: String) {
        viewModelScope.launch {
            val track = audioTracks.value.find { it.id == trackId }
            if (track != null) {
                db.audioTrackDao().insert(track.copy(playlist = newPlaylist))
            }
        }
    }

    fun addPdfDocument(
        uri: Uri,
        title: String,
        subject: String,
        grade: String
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            var fileName = "Document.pdf"
            var fileSizeFormatted = ""
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) {
                            val str = cursor.getString(nameIdx)
                            if (!str.isNullOrBlank()) fileName = str
                        }
                        if (sizeIdx >= 0) {
                            val sizeBytes = cursor.getLong(sizeIdx)
                            fileSizeFormatted = if (sizeBytes > 1024 * 1024) {
                                "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
                            } else {
                                "${sizeBytes / 1024} KB"
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            var pageCount = 0
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                    }
                }
            } catch (_: Exception) {}

            val doc = PdfDocumentEntity(
                id = "pdf_" + java.lang.Long.toString(System.currentTimeMillis(), 36),
                title = title,
                uriString = uri.toString(),
                fileName = fileName,
                subject = subject,
                grade = grade,
                pageCount = pageCount,
                lastPage = 0,
                lastOpenedTime = System.currentTimeMillis(),
                fileSizeFormatted = fileSizeFormatted,
                bookmarksJson = "[]"
            )
            db.pdfDocumentDao().insert(doc)
        }
    }

    fun deletePdfDocument(id: String) {
        viewModelScope.launch {
            db.pdfDocumentDao().deleteById(id)
        }
    }

    fun updatePdfPage(id: String, page: Int) {
        viewModelScope.launch {
            val doc = pdfDocuments.value.find { it.id == id }
            if (doc != null) {
                db.pdfDocumentDao().insert(doc.copy(lastPage = page, lastOpenedTime = System.currentTimeMillis()))
            }
        }
    }

    fun updatePdfBookmarks(id: String, bookmarks: Set<Int>) {
        viewModelScope.launch {
            val doc = pdfDocuments.value.find { it.id == id }
            if (doc != null) {
                val jsonArray = JSONArray(bookmarks.toList())
                db.pdfDocumentDao().insert(doc.copy(bookmarksJson = jsonArray.toString()))
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            showToast("Note deleted.")
        }
    }

    fun saveTask(task: PlannerTask) {
        viewModelScope.launch {
            repository.saveTask(task)
            showToast("Task saved.")
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            repository.toggleTask(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            showToast("Task deleted.")
        }
    }

    fun addFlashcard(front: String, back: String, subject: String = "General") {
        viewModelScope.launch {
            val card = StandaloneFlashcard(
                id = "f_" + java.lang.Long.toString(System.currentTimeMillis(), 36),
                front = front,
                back = back,
                subject = subject
            )
            repository.addFlashcard(card)
            showToast("Flashcard added.")
        }
    }

    fun reviewFlashcard(cardId: String, knewIt: Boolean) {
        viewModelScope.launch {
            repository.recordFlashcardReview(cardId, knewIt)
            showToast(if (knewIt) "Marked as known!" else "Queued for practice.")
        }
    }

    fun deleteFlashcard(cardId: String) {
        viewModelScope.launch {
            repository.deleteFlashcard(cardId)
            showToast("Flashcard deleted.")
        }
    }

    fun exportWorkspaceJson(): String = repository.exportWorkspaceJson()

    fun importWorkspaceJson(json: String, mode: String = "merge") {
        viewModelScope.launch {
            val ok = repository.importWorkspaceJson(json, mode)
            if (ok) showToast("Workspace imported successfully.") else showToast("Failed to import workspace.")
        }
    }

    fun exportBackupJson(): String = repository.exportFullBackupJson()

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            val ok = repository.restoreFullBackupJson(json)
            if (ok) showToast("Full backup restored successfully!") else showToast("Failed to restore backup.")
        }
    }

    fun exportQuestionBankJson(includeProgress: Boolean = true): String {
        return repository.exportQuestionBankJson(includeProgress)
    }

    fun createTransferCode(): String = repository.createTransferCode()

    fun restoreTransferCode(code: String) {
        viewModelScope.launch {
            val ok = repository.restoreFromTransferCode(code)
            if (ok) showToast("Transferred data restored successfully!") else showToast("Invalid transfer code.")
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetProgressOnly()
            showToast("Learning progress reset.")
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.resetAll()
            showToast("All data reset to initial state.")
        }
    }

    // Statistics calculations
    fun calculateStats(): DashboardStats {
        val qList = activeBank.value.questions
        val progMap = progress.value
        val srsMap = srs.value
        val now = System.currentTimeMillis()

        var seen = 0
        var att = 0
        var ok = 0
        var bad = 0
        var mastered = 0
        var due = 0
        var openInNotebook = 0

        qList.forEach { q ->
            val p = progMap[q.id]
            if (p != null) {
                if (p.att > 0) seen++
                att += p.att
                ok += p.ok
                bad += p.bad
                if (p.state == "mastered") mastered++
                if (p.nb && p.state != "mastered") openInNotebook++
            }
            if (srsMap[q.id]?.due?.let { it <= now } == true) due++
        }

        val accuracyStr = if (att > 0) "${((ok.toDouble() / att) * 100).toInt()}%" else "—"

        // Fast epoch day calculation (zero object allocation overhead)
        val zoneOffset = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
        val dayMs = 86_400_000L
        val todayEpochDay = (System.currentTimeMillis() + zoneOffset) / dayMs

        val attemptEpochDays = attempts.value.map { (it.at + zoneOffset) / dayMs }.toSet()
        var streak = 0
        var currentDayCheck = todayEpochDay
        if (!attemptEpochDays.contains(currentDayCheck)) {
            currentDayCheck -= 1
        }
        while (attemptEpochDays.contains(currentDayCheck)) {
            streak++
            currentDayCheck -= 1
        }

        val todayAttempts = attempts.value.count { ((it.at + zoneOffset) / dayMs) == todayEpochDay }

        val totalXp = attempts.value.sumOf { if (it.ok == 1) 10 else 2 } + (mastered * 50)
        val level = 1 + kotlin.math.sqrt(totalXp / 40.0).toInt()

        return DashboardStats(
            totalQuestions = qList.size,
            attempted = seen,
            totalAttempts = att,
            correctAttempts = ok,
            incorrectAttempts = bad,
            accuracy = accuracyStr,
            mastered = mastered,
            learning = maxOf(0, seen - mastered),
            dueForReview = due,
            openInNotebook = openInNotebook,
            streakDays = streak,
            todayCount = todayAttempts,
            totalXp = totalXp,
            level = level
        )
    }
}

data class DashboardStats(
    val totalQuestions: Int,
    val attempted: Int,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val incorrectAttempts: Int,
    val accuracy: String,
    val mastered: Int,
    val learning: Int,
    val dueForReview: Int,
    val openInNotebook: Int,
    val streakDays: Int,
    val todayCount: Int,
    val totalXp: Int,
    val level: Int
)
