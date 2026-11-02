package com.example.data

import android.content.Context
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChemistryRepository(private val context: Context) {

    private val dataFile: File = File(context.filesDir, "cml_database_v4.json")

    private val _banks = MutableStateFlow<List<QuestionBank>>(emptyList())
    val banks: StateFlow<List<QuestionBank>> = _banks.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, QuestionProgress>>(emptyMap())
    val progress: StateFlow<Map<String, QuestionProgress>> = _progress.asStateFlow()

    private val _bookmarks = MutableStateFlow<Set<String>>(emptySet())
    val bookmarks: StateFlow<Set<String>> = _bookmarks.asStateFlow()

    private val _srs = MutableStateFlow<Map<String, SrsSchedule>>(emptyMap())
    val srs: StateFlow<Map<String, SrsSchedule>> = _srs.asStateFlow()

    private val _attempts = MutableStateFlow<List<AttemptRecord>>(emptyList())
    val attempts: StateFlow<List<AttemptRecord>> = _attempts.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionLog>>(emptyList())
    val sessions: StateFlow<List<SessionLog>> = _sessions.asStateFlow()

    private val _exams = MutableStateFlow<List<ExamResult>>(emptyList())
    val exams: StateFlow<List<ExamResult>> = _exams.asStateFlow()

    private val _prefs = MutableStateFlow(UserPreferences())
    val prefs: StateFlow<UserPreferences> = _prefs.asStateFlow()

    private val _trash = MutableStateFlow<List<Pair<Question, String>>>(emptyList())
    val trash: StateFlow<List<Pair<Question, String>>> = _trash.asStateFlow()

    // Workspace stores (Notes, Tasks, Standalone Flashcards)
    private val _notes = MutableStateFlow<List<StudyNote>>(emptyList())
    val notes: StateFlow<List<StudyNote>> = _notes.asStateFlow()

    private val _plannerTasks = MutableStateFlow<List<PlannerTask>>(emptyList())
    val plannerTasks: StateFlow<List<PlannerTask>> = _plannerTasks.asStateFlow()

    private val _flashcards = MutableStateFlow<List<StandaloneFlashcard>>(emptyList())
    val flashcards: StateFlow<List<StandaloneFlashcard>> = _flashcards.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        if (!dataFile.exists()) {
            val allDefaults = EUEEQuestionBanks.createAllDefaultBanks()
            _banks.value = allDefaults
            _prefs.value = UserPreferences(bank = allDefaults[0].id)
            save()
            return@withContext
        }

        try {
            val content = dataFile.readText()
            val root = JSONObject(content)

            val bankList = mutableListOf<QuestionBank>()
            root.optJSONArray("banks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { bankList.add(QuestionBank.fromJson(it)) }
                }
            }

            val progMap = mutableMapOf<String, QuestionProgress>()
            root.optJSONObject("progress")?.let { obj ->
                obj.keys().forEach { k ->
                    obj.optJSONObject(k)?.let { progMap[k] = QuestionProgress.fromJson(it) }
                }
            }

            val bmSet = mutableSetOf<String>()
            root.optJSONArray("bookmarks")?.let { arr ->
                for (i in 0 until arr.length()) bmSet.add(arr.optString(i))
            }

            val srsMap = mutableMapOf<String, SrsSchedule>()
            root.optJSONObject("srs")?.let { obj ->
                obj.keys().forEach { k ->
                    obj.optJSONObject(k)?.let { srsMap[k] = SrsSchedule.fromJson(it) }
                }
            }

            val attList = mutableListOf<AttemptRecord>()
            root.optJSONArray("attempts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { attList.add(AttemptRecord.fromJson(it)) }
                }
            }

            val sessList = mutableListOf<SessionLog>()
            root.optJSONArray("sessions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { sessList.add(SessionLog.fromJson(it)) }
                }
            }

            val examList = mutableListOf<ExamResult>()
            root.optJSONArray("exams")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { examList.add(ExamResult.fromJson(it)) }
                }
            }

            val userPref = root.optJSONObject("prefs")?.let { UserPreferences.fromJson(it) }
                ?: UserPreferences()

            val noteList = mutableListOf<StudyNote>()
            root.optJSONArray("notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { noteList.add(StudyNote.fromJson(it)) }
                }
            }

            val taskList = mutableListOf<PlannerTask>()
            root.optJSONArray("plannerTasks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { taskList.add(PlannerTask.fromJson(it)) }
                }
            }

            val cardList = mutableListOf<StandaloneFlashcard>()
            root.optJSONArray("flashcards")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { cardList.add(StandaloneFlashcard.fromJson(it)) }
                }
            }

            val finalBanks = if (bankList.isEmpty()) listOf(SampleData.createSampleBank()) else bankList
            if (userPref.bank == null || !finalBanks.any { it.id == userPref.bank }) {
                userPref.bank = finalBanks[0].id
            }

            _banks.value = finalBanks
            _progress.value = progMap
            _bookmarks.value = bmSet
            _srs.value = srsMap
            _attempts.value = attList
            _sessions.value = sessList
            _exams.value = examList
            _prefs.value = userPref
            _notes.value = noteList
            _plannerTasks.value = taskList
            _flashcards.value = cardList
        } catch (e: Exception) {
            val sampleBank = SampleData.createSampleBank()
            _banks.value = listOf(sampleBank)
            _prefs.value = UserPreferences(bank = sampleBank.id)
            save()
        }
    }

    suspend fun save() = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()

            val banksArr = JSONArray()
            _banks.value.forEach { banksArr.put(it.toJson()) }
            root.put("banks", banksArr)

            val progObj = JSONObject()
            _progress.value.forEach { (k, v) -> progObj.put(k, v.toJson()) }
            root.put("progress", progObj)

            val bmArr = JSONArray()
            _bookmarks.value.forEach { bmArr.put(it) }
            root.put("bookmarks", bmArr)

            val srsObj = JSONObject()
            _srs.value.forEach { (k, v) -> srsObj.put(k, v.toJson()) }
            root.put("srs", srsObj)

            val attArr = JSONArray()
            _attempts.value.takeLast(5000).forEach { attArr.put(it.toJson()) }
            root.put("attempts", attArr)

            val sessArr = JSONArray()
            _sessions.value.take(50).forEach { sessArr.put(it.toJson()) }
            root.put("sessions", sessArr)

            val examArr = JSONArray()
            _exams.value.take(30).forEach { examArr.put(it.toJson()) }
            root.put("exams", examArr)

            val notesArr = JSONArray()
            _notes.value.forEach { notesArr.put(it.toJson()) }
            root.put("notes", notesArr)

            val tasksArr = JSONArray()
            _plannerTasks.value.forEach { tasksArr.put(it.toJson()) }
            root.put("plannerTasks", tasksArr)

            val cardsArr = JSONArray()
            _flashcards.value.forEach { cardsArr.put(it.toJson()) }
            root.put("flashcards", cardsArr)

            root.put("prefs", _prefs.value.toJson())
            root.put("saved", System.currentTimeMillis())

            val tmp = File(context.filesDir, "cml_database_v4.json.tmp")
            tmp.writeText(root.toString(2))
            tmp.renameTo(dataFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getActiveBank(): QuestionBank {
        val currentBankId = _prefs.value.bank
        return _banks.value.find { it.id == currentBankId } ?: _banks.value.firstOrNull() ?: SampleData.createSampleBank()
    }

    suspend fun selectBank(bankId: String) {
        val updated = _prefs.value.copy(bank = bankId)
        _prefs.value = updated
        save()
    }

    suspend fun updatePreferences(newPrefs: UserPreferences) {
        _prefs.value = newPrefs
        save()
    }

    suspend fun toggleBookmark(questionId: String) {
        val current = _bookmarks.value.toMutableSet()
        if (current.contains(questionId)) current.remove(questionId) else current.add(questionId)
        _bookmarks.value = current
        save()
    }

    suspend fun recordAttempt(
        question: Question,
        ok: Boolean,
        userAns: String,
        confidence: Int,
        mode: String,
        hintsUsed: Int,
        secondsTaken: Int,
        diagnosis: String? = null
    ) {
        val now = System.currentTimeMillis()
        val progMap = _progress.value.toMutableMap()
        val p = progMap[question.id]?.copy() ?: QuestionProgress()

        p.att += 1
        if (ok) p.ok += 1 else p.bad += 1
        p.ans = userAns
        p.at = now
        p.lo = if (ok) 1 else 0
        p.conf = confidence

        if (!ok) {
            p.nb = true
            p.state = "learning"
            p.man = 0
            if (p.fm == 0L) p.fm = now
        }

        val srsMap = _srs.value.toMutableMap()
        val currentSrs = srsMap[question.id]
        val nextSrs = com.example.logic.ChemistryLogic.srsNext(currentSrs, ok, confidence, now)
        srsMap[question.id] = nextSrs

        if (p.man == 0) {
            p.state = if (com.example.logic.ChemistryLogic.isMastered(p, nextSrs)) "mastered" else "learning"
        }

        progMap[question.id] = p
        _progress.value = progMap
        _srs.value = srsMap

        val attList = _attempts.value.toMutableList()
        attList.add(
            AttemptRecord(
                q = question.id,
                b = getActiveBank().id,
                ok = if (ok) 1 else 0,
                ans = userAns,
                at = now,
                c = confidence,
                m = mode,
                h = hintsUsed,
                s = secondsTaken,
                d = diagnosis
            )
        )
        if (attList.size > 5000) attList.removeAt(0)
        _attempts.value = attList

        save()
    }

    suspend fun setNotebookFlag(questionId: String, inNotebook: Boolean) {
        val progMap = _progress.value.toMutableMap()
        val p = progMap[questionId]?.copy() ?: QuestionProgress()
        p.nb = inNotebook
        progMap[questionId] = p
        _progress.value = progMap
        save()
    }

    suspend fun setMasteryState(questionId: String, state: String) {
        val progMap = _progress.value.toMutableMap()
        val p = progMap[questionId]?.copy() ?: QuestionProgress()
        p.state = state
        p.man = 1
        progMap[questionId] = p
        _progress.value = progMap
        save()
    }

    suspend fun flagQuestion(questionId: String, reason: String?) {
        val progMap = _progress.value.toMutableMap()
        val p = progMap[questionId]?.copy() ?: QuestionProgress()
        p.rep = reason
        progMap[questionId] = p
        _progress.value = progMap
        save()
    }

    suspend fun recordSession(log: SessionLog) {
        val sessList = _sessions.value.toMutableList()
        sessList.add(0, log)
        if (sessList.size > 50) sessList.removeAt(sessList.size - 1)
        _sessions.value = sessList
        save()
    }

    suspend fun recordExam(exam: ExamResult) {
        val examList = _exams.value.toMutableList()
        examList.add(0, exam)
        if (examList.size > 30) examList.removeAt(examList.size - 1)
        _exams.value = examList
        save()
    }

    suspend fun saveQuestion(question: Question, targetBankId: String) {
        val currentBanks = _banks.value.map { bank ->
            if (bank.id == targetBankId) {
                val questions = bank.questions.toMutableList()
                val idx = questions.indexOfFirst { it.id == question.id }
                if (idx >= 0) questions[idx] = question else questions.add(question)
                bank.copy(questions = questions)
            } else {
                bank
            }
        }
        _banks.value = currentBanks
        save()
    }

    suspend fun deleteQuestion(question: Question, bankId: String) {
        val trashList = _trash.value.toMutableList()
        trashList.add(Pair(question, bankId))
        if (trashList.size > 20) trashList.removeAt(0)
        _trash.value = trashList

        val currentBanks = _banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(questions = bank.questions.filter { it.id != question.id })
            } else {
                bank
            }
        }
        _banks.value = currentBanks
        save()
    }

    suspend fun restoreLastDeleted(): Boolean {
        val trashList = _trash.value.toMutableList()
        if (trashList.isEmpty()) return false
        val (q, bankId) = trashList.removeAt(trashList.size - 1)
        _trash.value = trashList

        val currentBanks = _banks.value.map { bank ->
            if (bank.id == bankId && !bank.questions.any { it.id == q.id }) {
                bank.copy(questions = bank.questions + q)
            } else bank
        }
        _banks.value = currentBanks
        save()
        return true
    }

    suspend fun addBank(newBank: QuestionBank) {
        _banks.value = _banks.value + newBank
        _prefs.value = _prefs.value.copy(bank = newBank.id)
        save()
    }

    suspend fun deleteBank(bankId: String): Boolean {
        if (_banks.value.size <= 1) return false
        val remaining = _banks.value.filter { it.id != bankId }
        _banks.value = remaining
        if (_prefs.value.bank == bankId) {
            _prefs.value = _prefs.value.copy(bank = remaining[0].id)
        }
        save()
        return true
    }

    suspend fun mergeBanks(sourceBankId: String, targetBankId: String) {
        val source = _banks.value.find { it.id == sourceBankId } ?: return
        val target = _banks.value.find { it.id == targetBankId } ?: return
        val plan = com.example.logic.ChemistryLogic.plan(source.questions, target.questions)

        val updatedTarget = target.copy(questions = target.questions + plan.add)
        val remaining = _banks.value.filter { it.id != sourceBankId }.map {
            if (it.id == targetBankId) updatedTarget else it
        }
        _banks.value = remaining
        save()
    }

    suspend fun ensureEueeSubjectBanksLoaded() {
        val current = _banks.value.toMutableList()
        val allDefaults = EUEEQuestionBanks.createAllDefaultBanks()
        var added = false
        allDefaults.forEach { defaultBank ->
            if (!current.any { it.id == defaultBank.id }) {
                current.add(defaultBank)
                added = true
            }
        }
        if (added) {
            _banks.value = current
            save()
        }
    }

    // Workspace CRUD Methods
    suspend fun saveNote(note: StudyNote) {
        val list = _notes.value.toMutableList()
        val idx = list.indexOfFirst { it.id == note.id }
        if (idx >= 0) list[idx] = note else list.add(0, note)
        _notes.value = list
        save()
    }

    suspend fun deleteNote(noteId: String) {
        _notes.value = _notes.value.filter { it.id != noteId }
        save()
    }

    suspend fun saveTask(task: PlannerTask) {
        val list = _plannerTasks.value.toMutableList()
        val idx = list.indexOfFirst { it.id == task.id }
        if (idx >= 0) list[idx] = task else list.add(task)
        _plannerTasks.value = list
        save()
    }

    suspend fun toggleTask(taskId: String) {
        _plannerTasks.value = _plannerTasks.value.map {
            if (it.id == taskId) it.copy(done = !it.done) else it
        }
        save()
    }

    suspend fun deleteTask(taskId: String) {
        _plannerTasks.value = _plannerTasks.value.filter { it.id != taskId }
        save()
    }

    suspend fun addFlashcard(card: StandaloneFlashcard) {
        val list = _flashcards.value.toMutableList()
        val idx = list.indexOfFirst { it.id == card.id }
        if (idx >= 0) list[idx] = card else list.add(0, card)
        _flashcards.value = list
        save()
    }

    suspend fun recordFlashcardReview(cardId: String, knewIt: Boolean) {
        _flashcards.value = _flashcards.value.map {
            if (it.id == cardId) {
                it.copy(
                    reviews = it.reviews + 1,
                    known = if (knewIt) it.known + 1 else it.known,
                    again = if (!knewIt) it.again + 1 else it.again
                )
            } else it
        }
        save()
    }

    suspend fun deleteFlashcard(cardId: String) {
        _flashcards.value = _flashcards.value.filter { it.id != cardId }
        save()
    }

    suspend fun resetProgressOnly() {
        _progress.value = emptyMap()
        _srs.value = emptyMap()
        _attempts.value = emptyList()
        _sessions.value = emptyList()
        _exams.value = emptyList()
        _bookmarks.value = emptySet()
        save()
    }

    suspend fun resetAll() {
        val sampleBank = SampleData.createSampleBank()
        _banks.value = listOf(sampleBank)
        _progress.value = emptyMap()
        _srs.value = emptyMap()
        _attempts.value = emptyList()
        _sessions.value = emptyList()
        _exams.value = emptyList()
        _bookmarks.value = emptySet()
        _notes.value = emptyList()
        _plannerTasks.value = emptyList()
        _flashcards.value = emptyList()
        _prefs.value = UserPreferences(bank = sampleBank.id)
        save()
    }

    fun exportWorkspaceJson(): String {
        val root = JSONObject()
        root.put("app", "EUEE Mastery")
        root.put("kind", "workspace")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val notesArr = JSONArray()
        _notes.value.forEach { notesArr.put(it.toJson()) }
        root.put("notes", notesArr)

        val tasksArr = JSONArray()
        _plannerTasks.value.forEach { tasksArr.put(it.toJson()) }
        root.put("tasks", tasksArr)

        val cardsArr = JSONArray()
        _flashcards.value.forEach { cardsArr.put(it.toJson()) }
        root.put("flashcards", cardsArr)

        return root.toString(2)
    }

    suspend fun importWorkspaceJson(jsonString: String, mode: String = "merge"): Boolean {
        try {
            val root = JSONObject(jsonString)
            val notesArr = root.optJSONArray("notes")
            val tasksArr = root.optJSONArray("tasks")
            val cardsArr = root.optJSONArray("flashcards")

            val importedNotes = mutableListOf<StudyNote>()
            notesArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { importedNotes.add(StudyNote.fromJson(it)) }
                }
            }

            val importedTasks = mutableListOf<PlannerTask>()
            tasksArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { importedTasks.add(PlannerTask.fromJson(it)) }
                }
            }

            val importedCards = mutableListOf<StandaloneFlashcard>()
            cardsArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { importedCards.add(StandaloneFlashcard.fromJson(it)) }
                }
            }

            if (mode == "replace") {
                _notes.value = importedNotes
                _plannerTasks.value = importedTasks
                _flashcards.value = importedCards
            } else {
                val currentNotes = _notes.value.toMutableList()
                importedNotes.forEach { n ->
                    if (!currentNotes.any { it.id == n.id || (it.title == n.title && it.body == n.body) }) {
                        currentNotes.add(n)
                    }
                }
                _notes.value = currentNotes

                val currentTasks = _plannerTasks.value.toMutableList()
                importedTasks.forEach { t ->
                    if (!currentTasks.any { it.id == t.id || it.title == t.title }) {
                        currentTasks.add(t)
                    }
                }
                _plannerTasks.value = currentTasks

                val currentCards = _flashcards.value.toMutableList()
                importedCards.forEach { c ->
                    if (!currentCards.any { it.id == c.id || (it.front == c.front && it.back == c.back) }) {
                        currentCards.add(c)
                    }
                }
                _flashcards.value = currentCards
            }
            save()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun exportFullBackupJson(): String {
        val root = JSONObject()
        root.put("app", "cml")
        root.put("v", 4)
        root.put("at", System.currentTimeMillis())

        val data = JSONObject()
        val banksArr = JSONArray()
        _banks.value.forEach { banksArr.put(it.toJson()) }
        data.put("banks", banksArr)

        val progObj = JSONObject()
        _progress.value.forEach { (k, v) -> progObj.put(k, v.toJson()) }
        data.put("progress", progObj)

        val bmArr = JSONArray()
        _bookmarks.value.forEach { bmArr.put(it) }
        data.put("bookmarks", bmArr)

        val srsObj = JSONObject()
        _srs.value.forEach { (k, v) -> srsObj.put(k, v.toJson()) }
        data.put("srs", srsObj)

        val attArr = JSONArray()
        _attempts.value.forEach { attArr.put(it.toJson()) }
        data.put("attempts", attArr)

        val sessArr = JSONArray()
        _sessions.value.forEach { sessArr.put(it.toJson()) }
        data.put("sessions", sessArr)

        val examArr = JSONArray()
        _exams.value.forEach { examArr.put(it.toJson()) }
        data.put("exams", examArr)

        data.put("prefs", _prefs.value.toJson())
        root.put("data", data)

        // Workspace payload
        val workspace = JSONObject()
        val notesArr = JSONArray()
        _notes.value.forEach { notesArr.put(it.toJson()) }
        workspace.put("notes", notesArr)

        val tasksArr = JSONArray()
        _plannerTasks.value.forEach { tasksArr.put(it.toJson()) }
        workspace.put("tasks", tasksArr)

        val cardsArr = JSONArray()
        _flashcards.value.forEach { cardsArr.put(it.toJson()) }
        workspace.put("flashcards", cardsArr)

        root.put("workspace", workspace)
        return root.toString(2)
    }

    suspend fun restoreFullBackupJson(jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: root

            val bankList = mutableListOf<QuestionBank>()
            data.optJSONArray("banks")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { bankList.add(QuestionBank.fromJson(it)) }
                }
            }

            val progMap = mutableMapOf<String, QuestionProgress>()
            data.optJSONObject("progress")?.let { obj ->
                obj.keys().forEach { k ->
                    obj.optJSONObject(k)?.let { progMap[k] = QuestionProgress.fromJson(it) }
                }
            }

            val bmSet = mutableSetOf<String>()
            data.optJSONArray("bookmarks")?.let { arr ->
                for (i in 0 until arr.length()) bmSet.add(arr.optString(i))
            }

            val srsMap = mutableMapOf<String, SrsSchedule>()
            data.optJSONObject("srs")?.let { obj ->
                obj.keys().forEach { k ->
                    obj.optJSONObject(k)?.let { srsMap[k] = SrsSchedule.fromJson(it) }
                }
            }

            val attList = mutableListOf<AttemptRecord>()
            data.optJSONArray("attempts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { attList.add(AttemptRecord.fromJson(it)) }
                }
            }

            val sessList = mutableListOf<SessionLog>()
            data.optJSONArray("sessions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { sessList.add(SessionLog.fromJson(it)) }
                }
            }

            val examList = mutableListOf<ExamResult>()
            data.optJSONArray("exams")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { examList.add(ExamResult.fromJson(it)) }
                }
            }

            val userPref = data.optJSONObject("prefs")?.let { UserPreferences.fromJson(it) }
                ?: UserPreferences()

            val finalBanks = if (bankList.isEmpty()) listOf(SampleData.createSampleBank()) else bankList
            _banks.value = finalBanks
            _progress.value = progMap
            _bookmarks.value = bmSet
            _srs.value = srsMap
            _attempts.value = attList
            _sessions.value = sessList
            _exams.value = examList
            _prefs.value = userPref.copy(bank = userPref.bank ?: finalBanks[0].id)

            // Restore workspace if present
            root.optJSONObject("workspace")?.let { ws ->
                importWorkspaceJson(ws.toString(), mode = "replace")
            }

            save()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun exportQuestionBankJson(includeProgress: Boolean = true): String {
        val currentBank = getActiveBank()
        val root = JSONObject()
        root.put("app", "cml")
        root.put("v", 4)
        root.put("kind", "questions")
        root.put("bank", currentBank.name)
        root.put("at", System.currentTimeMillis())

        val qArr = JSONArray()
        currentBank.questions.forEach { qArr.put(it.toJson()) }
        root.put("questions", qArr)

        if (includeProgress) {
            val learnObj = JSONObject()
            val progMap = _progress.value
            val srsMap = _srs.value
            val bmSet = _bookmarks.value
            currentBank.questions.forEach { q ->
                val p = progMap[q.id]
                if (p != null) {
                    val item = JSONObject()
                    item.put("progress", p.toJson())
                    srsMap[q.id]?.let { item.put("schedule", it.toJson()) }
                    item.put("bookmarked", bmSet.contains(q.id))
                    learnObj.put(q.id, item)
                }
            }
            root.put("learning", learnObj)
        }
        return root.toString(2)
    }

    fun createTransferCode(): String {
        val json = exportFullBackupJson()
        val bos = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(bos).use { gzip ->
            gzip.write(json.toByteArray(Charsets.UTF_8))
        }
        return android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
    }

    suspend fun restoreFromTransferCode(code: String): Boolean {
        return try {
            val bytes = android.util.Base64.decode(code.trim(), android.util.Base64.DEFAULT)
            val bis = java.io.ByteArrayInputStream(bytes)
            val json = java.util.zip.GZIPInputStream(bis).bufferedReader(Charsets.UTF_8).use { it.readText() }
            restoreFullBackupJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
