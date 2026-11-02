package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.ChemistryRepository
import com.example.data.local.StudyAlarmEntity
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipRestorePreview(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val exportedAt: Long = 0L,
    val noteCount: Int = 0,
    val taskCount: Int = 0,
    val flashcardCount: Int = 0,
    val bankCount: Int = 0,
    val questionCount: Int = 0,
    val alarmCount: Int = 0,
    val rawJsonPayloads: Map<String, String> = emptyMap()
)

object ZipBackupHelper {

    suspend fun createZipBackup(
        context: Context,
        repository: ChemistryRepository,
        alarms: List<StudyAlarmEntity>,
        includeNotes: Boolean = true,
        includeTasks: Boolean = true,
        includeCards: Boolean = true,
        includeBanks: Boolean = true,
        includeProgress: Boolean = true,
        includeAlarms: Boolean = true
    ): File = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val zipFile = File(backupDir, "euee_mastery_backup_$timeStamp.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            val manifest = JSONObject().apply {
                put("app", "EUEE Mastery")
                put("version", 2)
                put("createdAt", System.currentTimeMillis())
                put("date", timeStamp)
                put("notesIncluded", includeNotes)
                put("tasksIncluded", includeTasks)
                put("cardsIncluded", includeCards)
                put("banksIncluded", includeBanks)
                put("progressIncluded", includeProgress)
                put("alarmsIncluded", includeAlarms)
            }

            // 1. Manifest
            writeEntry(zos, "manifest.json", manifest.toString(2))

            // 2. Notes
            if (includeNotes) {
                val notesArr = JSONArray()
                repository.notes.value.forEach { notesArr.put(it.toJson()) }
                writeEntry(zos, "notes.json", notesArr.toString(2))
            }

            // 3. Tasks
            if (includeTasks) {
                val tasksArr = JSONArray()
                repository.plannerTasks.value.forEach { tasksArr.put(it.toJson()) }
                writeEntry(zos, "tasks.json", tasksArr.toString(2))
            }

            // 4. Flashcards
            if (includeCards) {
                val cardsArr = JSONArray()
                repository.flashcards.value.forEach { cardsArr.put(it.toJson()) }
                writeEntry(zos, "flashcards.json", cardsArr.toString(2))
            }

            // 5. Question Banks
            if (includeBanks) {
                val banksArr = JSONArray()
                repository.banks.value.forEach { banksArr.put(it.toJson()) }
                writeEntry(zos, "question_banks.json", banksArr.toString(2))
            }

            // 6. Progress and SRS
            if (includeProgress) {
                val progressObj = JSONObject().apply {
                    val progObj = JSONObject()
                    repository.progress.value.forEach { (k, v) -> progObj.put(k, v.toJson()) }
                    put("progress", progObj)

                    val srsObj = JSONObject()
                    repository.srs.value.forEach { (k, v) -> srsObj.put(k, v.toJson()) }
                    put("srs", srsObj)

                    val bmArr = JSONArray()
                    repository.bookmarks.value.forEach { bmArr.put(it) }
                    put("bookmarks", bmArr)

                    val attArr = JSONArray()
                    repository.attempts.value.forEach { attArr.put(it.toJson()) }
                    put("attempts", attArr)

                    val examArr = JSONArray()
                    repository.exams.value.forEach { examArr.put(it.toJson()) }
                    put("exams", examArr)
                }
                writeEntry(zos, "progress_and_srs.json", progressObj.toString(2))
            }

            // 7. Alarms
            if (includeAlarms) {
                val alarmsArr = JSONArray()
                alarms.forEach { a ->
                    val obj = JSONObject().apply {
                        put("id", a.id)
                        put("title", a.title)
                        put("hour", a.hour)
                        put("minute", a.minute)
                        put("daysOfWeek", a.daysOfWeek)
                        put("isEnabled", a.isEnabled)
                        put("isExamCountdown", a.isExamCountdown)
                        put("examDate", a.examDate)
                        put("vibrate", a.vibrate)
                    }
                    alarmsArr.put(obj)
                }
                writeEntry(zos, "alarms.json", alarmsArr.toString(2))
            }

            // 8. User preferences
            writeEntry(zos, "user_preferences.json", repository.prefs.value.toJson().toString(2))

            // 9. Human-readable revision readme
            val summaryText = buildString {
                appendLine("========================================")
                appendLine("EUEE MASTERY - STUDY DATA BACKUP ARCHIVE")
                appendLine("Generated: $timeStamp")
                appendLine("========================================")
                appendLine("Notes: ${repository.notes.value.size}")
                appendLine("Tasks: ${repository.plannerTasks.value.size}")
                appendLine("Flashcards: ${repository.flashcards.value.size}")
                appendLine("Question Banks: ${repository.banks.value.size}")
                appendLine("Active Bank: ${repository.getActiveBank().name}")
                appendLine("Scheduled Alarms: ${alarms.size}")
                appendLine()
                appendLine("Notice: This is a user data archive. To export the full Android Studio Kotlin source code project, use the AI Studio project settings menu.")
            }
            writeEntry(zos, "README_BACKUP.txt", summaryText)
        }

        zipFile
    }

    suspend fun inspectZip(context: Context, uri: Uri): ZipRestorePreview = withContext(Dispatchers.IO) {
        val payloads = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(BufferedInputStream(stream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && (name.endsWith(".json") || name.endsWith(".txt"))) {
                            val content = zis.bufferedReader().readText()
                            payloads[name] = content
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val manifestContent = payloads["manifest.json"]
            if (manifestContent == null) {
                return@withContext ZipRestorePreview(
                    isValid = false,
                    errorMessage = "Missing manifest.json inside ZIP. Ensure this is an official EUEE Mastery backup archive."
                )
            }

            val manifest = JSONObject(manifestContent)
            val noteCount = payloads["notes.json"]?.let { JSONArray(it).length() } ?: 0
            val taskCount = payloads["tasks.json"]?.let { JSONArray(it).length() } ?: 0
            val cardCount = payloads["flashcards.json"]?.let { JSONArray(it).length() } ?: 0

            var questionCount = 0
            val bankCount = payloads["question_banks.json"]?.let {
                val arr = JSONArray(it)
                for (i in 0 until arr.length()) {
                    questionCount += arr.optJSONObject(i)?.optJSONArray("questions")?.length() ?: 0
                }
                arr.length()
            } ?: 0

            val alarmCount = payloads["alarms.json"]?.let { JSONArray(it).length() } ?: 0

            ZipRestorePreview(
                isValid = true,
                exportedAt = manifest.optLong("createdAt", 0L),
                noteCount = noteCount,
                taskCount = taskCount,
                flashcardCount = cardCount,
                bankCount = bankCount,
                questionCount = questionCount,
                alarmCount = alarmCount,
                rawJsonPayloads = payloads
            )
        } catch (e: Exception) {
            ZipRestorePreview(
                isValid = false,
                errorMessage = "Failed to parse ZIP archive: ${e.localizedMessage}"
            )
        }
    }

    suspend fun applyRestore(
        repository: ChemistryRepository,
        preview: ZipRestorePreview,
        mode: String = "merge" // "merge" or "replace"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payloads = preview.rawJsonPayloads

            // 1. Notes
            payloads["notes.json"]?.let { notesJson ->
                val arr = JSONArray(notesJson)
                val newNotes = mutableListOf<StudyNote>()
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { newNotes.add(StudyNote.fromJson(it)) }
                }
                if (mode == "replace") {
                    newNotes.forEach { repository.saveNote(it) }
                } else {
                    val existing = repository.notes.value
                    newNotes.forEach { n ->
                        if (!existing.any { it.id == n.id }) repository.saveNote(n)
                    }
                }
            }

            // 2. Tasks
            payloads["tasks.json"]?.let { tasksJson ->
                val arr = JSONArray(tasksJson)
                val newTasks = mutableListOf<PlannerTask>()
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { newTasks.add(PlannerTask.fromJson(it)) }
                }
                if (mode == "replace") {
                    newTasks.forEach { repository.saveTask(it) }
                } else {
                    val existing = repository.plannerTasks.value
                    newTasks.forEach { t ->
                        if (!existing.any { it.id == t.id }) repository.saveTask(t)
                    }
                }
            }

            // 3. Flashcards
            payloads["flashcards.json"]?.let { cardsJson ->
                val arr = JSONArray(cardsJson)
                val newCards = mutableListOf<StandaloneFlashcard>()
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { newCards.add(StandaloneFlashcard.fromJson(it)) }
                }
                if (mode == "replace") {
                    newCards.forEach { repository.addFlashcard(it) }
                } else {
                    val existing = repository.flashcards.value
                    newCards.forEach { c ->
                        if (!existing.any { it.id == c.id }) repository.addFlashcard(c)
                    }
                }
            }

            // 4. Question Banks
            payloads["question_banks.json"]?.let { banksJson ->
                val arr = JSONArray(banksJson)
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { bankObj ->
                        val b = QuestionBank.fromJson(bankObj)
                        val existingBank = repository.banks.value.find { it.id == b.id }
                        if (existingBank == null) {
                            repository.addBank(b)
                        } else if (mode == "replace") {
                            b.questions.forEach { q -> repository.saveQuestion(q, b.id) }
                        } else {
                            val plan = com.example.logic.ChemistryLogic.plan(b.questions, existingBank.questions)
                            plan.add.forEach { q -> repository.saveQuestion(q, b.id) }
                        }
                    }
                }
            }

            repository.save()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(Charsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }
}
