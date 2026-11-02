package com.example.data.repository

import com.example.data.local.*
import com.example.domain.model.*
import com.example.domain.repository.*
import android.util.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

class TimetableRepositoryImpl(
    private val dao: TimetablePlanDao
) : ITimetableRepository {

    override fun getAllSlots(): Flow<List<TimetableSlot>> {
        return dao.getAllPlans().map { list -> list.map { it.toDomain() } }
    }

    override fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlot>> {
        return dao.getPlansForDay(dayOfWeek).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSlotById(id: Int): TimetableSlot? {
        return dao.getPlanById(id)?.toDomain()
    }

    override suspend fun saveSlot(slot: TimetableSlot): Long {
        return dao.insertPlan(slot.toEntity())
    }

    override suspend fun deleteSlot(id: Int) {
        dao.deletePlan(id)
    }

    override suspend fun toggleAlarm(id: Int, isEnabled: Boolean) {
        dao.setAlarmEnabled(id, isEnabled)
    }

    private fun TimetablePlanEntity.toDomain() = TimetableSlot(
        id = id,
        dayOfWeek = dayOfWeek,
        dayName = dayName,
        timeSlot = timeSlot,
        subject = subject,
        topic = topic,
        audioUri = audioUri,
        audioTitle = audioTitle,
        isAlarmEnabled = isAlarmEnabled,
        notes = notes
    )

    private fun TimetableSlot.toEntity() = TimetablePlanEntity(
        id = id,
        dayOfWeek = dayOfWeek,
        dayName = dayName,
        timeSlot = timeSlot,
        subject = subject,
        topic = topic,
        audioUri = audioUri,
        audioTitle = audioTitle,
        isAlarmEnabled = isAlarmEnabled,
        notes = notes
    )
}

class AnalyticsRepositoryImpl(
    private val dao: StudyMetricTrackDao
) : IAnalyticsRepository {

    override fun getAllMetrics(): Flow<List<StudyTrackMetric>> {
        return dao.getAllTracks().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addMetric(metric: StudyTrackMetric): Long {
        return dao.insertTrack(metric.toEntity())
    }

    override suspend fun updateMetric(metric: StudyTrackMetric) {
        dao.updateTrack(metric.toEntity())
    }

    override suspend fun deleteMetric(id: Int) {
        dao.deleteTrack(id)
    }

    override fun getMetricsBySubject(subject: String): Flow<List<StudyTrackMetric>> {
        return dao.getTracksForSubject(subject).map { list -> list.map { it.toDomain() } }
    }

    private fun StudyMetricTrackEntity.toDomain() = StudyTrackMetric(
        id = id,
        timestamp = timestamp,
        subject = subject,
        focusMinutes = focusMinutes,
        quizScorePercent = quizScorePercent,
        questionsSolved = questionsSolved,
        milestoneTitle = milestoneTitle,
        notes = notes
    )

    private fun StudyTrackMetric.toEntity() = StudyMetricTrackEntity(
        id = id,
        timestamp = timestamp,
        subject = subject,
        focusMinutes = focusMinutes,
        quizScorePercent = quizScorePercent,
        questionsSolved = questionsSolved,
        milestoneTitle = milestoneTitle,
        notes = notes
    )
}

class DictionaryRepositoryImpl(
    private val dao: DictionaryEntryDao
) : IDictionaryRepository {

    override fun getAllTerms(): Flow<List<DictionaryTerm>> {
        return dao.getAllEntries().map { list -> list.map { it.toDomain() } }
    }

    override fun searchTerms(query: String): Flow<List<DictionaryTerm>> {
        return dao.searchEntries(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getTermsBySubject(subject: String): Flow<List<DictionaryTerm>> {
        return dao.getEntriesBySubject(subject).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertTerm(term: DictionaryTerm): Long {
        return dao.insertEntry(term.toEntity())
    }

    override suspend fun insertTerms(terms: List<DictionaryTerm>): List<Long> {
        return dao.insertEntries(terms.map { it.toEntity() })
    }

    override suspend fun updateTerm(term: DictionaryTerm) {
        dao.updateEntry(term.toEntity())
    }

    override suspend fun deleteTerm(id: Int) {
        dao.deleteEntry(id)
    }

    /**
     * Accepts generic structured JSON (either array of objects or key-value object),
     * dynamically mapping fields like word, term, meaning, definition, category, subject.
     */
    override suspend fun importJsonData(rawJson: String): Int {
        val trimmed = rawJson.trim()
        val entriesToInsert = mutableListOf<DictionaryEntryEntity>()

        try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    entriesToInsert.add(parseDynamicJsonItem(item))
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                // Check if it wraps an array e.g. {"words": [...]} or {"dictionary": [...]}
                val arrayKey = root.keys().asSequence().firstOrNull { root.optJSONArray(it) != null }
                if (arrayKey != null) {
                    val array = root.getJSONArray(arrayKey)
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        entriesToInsert.add(parseDynamicJsonItem(item))
                    }
                } else {
                    // Key-value map: e.g. {"Photosynthesis": "Process by which plants...", "Mitochondria": "..."}
                    for (key in root.keys()) {
                        val value = root.get(key)
                        if (value is String) {
                            entriesToInsert.add(
                                DictionaryEntryEntity(
                                    term = key,
                                    definition = value,
                                    subject = inferSubject(key, value)
                                )
                            )
                        } else if (value is JSONObject) {
                            val entity = parseDynamicJsonItem(value, fallbackTerm = key)
                            entriesToInsert.add(entity)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return 0
        }

        if (entriesToInsert.isNotEmpty()) {
            dao.insertEntries(entriesToInsert)
        }
        return entriesToInsert.size
    }

    override suspend fun importFromStream(inputStream: java.io.InputStream): Int = withContext(Dispatchers.IO) {
        val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))
        val batch = mutableListOf<DictionaryEntryEntity>()
        var count = 0
        val batchSize = 50

        fun parseObject(): DictionaryEntryEntity {
            var term = ""
            var definition = ""
            var subject = ""
            var pos = "noun"
            var example = ""
            var phonetic = ""

            reader.beginObject()
            while (reader.hasNext()) {
                val key = reader.nextName()
                val peek = reader.peek()
                if (peek == android.util.JsonToken.STRING || peek == android.util.JsonToken.NUMBER) {
                    val value = reader.nextString()
                    when (key.lowercase()) {
                        "term", "word", "title", "name" -> term = value
                        "definition", "meaning", "desc", "description", "explanation" -> definition = value
                        "subject", "category", "topic" -> subject = value
                        "partofspeech", "pos", "type" -> pos = value
                        "example", "usage" -> example = value
                        "phonetic", "ipa" -> phonetic = value
                    }
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()

            val finalSubject = if (subject.isNotBlank()) normalizeSubject(subject) else inferSubject(term, definition)
            return DictionaryEntryEntity(
                term = term.ifBlank { "Term ${System.currentTimeMillis() % 10000}" },
                definition = definition.ifBlank { "No definition provided." },
                subject = finalSubject,
                partOfSpeech = pos,
                exampleUsage = example,
                phonetic = phonetic
            )
        }

        suspend fun parseArray() {
            reader.beginArray()
            while (reader.hasNext()) {
                if (reader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                    val entity = parseObject()
                    batch.add(entity)
                    if (batch.size >= batchSize) {
                        dao.insertEntries(batch)
                        count += batch.size
                        batch.clear()
                    }
                } else {
                    reader.skipValue()
                }
            }
            reader.endArray()
        }

        try {
            val root = reader.peek()
            if (root == android.util.JsonToken.BEGIN_ARRAY) {
                parseArray()
            } else if (root == android.util.JsonToken.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                        parseArray()
                    } else if (reader.peek() == android.util.JsonToken.STRING) {
                        val def = reader.nextString()
                        batch.add(
                            DictionaryEntryEntity(
                                term = name,
                                definition = def,
                                subject = inferSubject(name, def)
                            )
                        )
                        if (batch.size >= batchSize) {
                            dao.insertEntries(batch)
                            count += batch.size
                            batch.clear()
                        }
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
            if (batch.isNotEmpty()) {
                dao.insertEntries(batch)
                count += batch.size
                batch.clear()
            }
            reader.close()
        } catch (_: Exception) {}

        count
    }

    private fun parseDynamicJsonItem(obj: JSONObject, fallbackTerm: String = ""): DictionaryEntryEntity {
        val term = obj.optString("term").ifBlank {
            obj.optString("word").ifBlank {
                obj.optString("title").ifBlank {
                    obj.optString("name").ifBlank { fallbackTerm }
                }
            }
        }

        val definition = obj.optString("definition").ifBlank {
            obj.optString("meaning").ifBlank {
                obj.optString("desc").ifBlank {
                    obj.optString("description").ifBlank {
                        obj.optString("explanation", "No definition provided.")
                    }
                }
            }
        }

        val subjectCandidate = obj.optString("subject").ifBlank {
            obj.optString("category").ifBlank {
                obj.optString("topic", "")
            }
        }
        val subject = if (subjectCandidate.isNotBlank()) {
            normalizeSubject(subjectCandidate)
        } else {
            inferSubject(term, definition)
        }

        val pos = obj.optString("partOfSpeech").ifBlank {
            obj.optString("pos").ifBlank {
                obj.optString("type", "noun")
            }
        }
        val phonetic = obj.optString("phonetic").ifBlank { obj.optString("ipa", "") }
        val example = obj.optString("example").ifBlank {
            obj.optString("exampleUsage").ifBlank { obj.optString("usage", "") }
        }

        val syns = obj.optJSONArray("synonyms")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }.joinToString(",")
        } ?: "[]"

        return DictionaryEntryEntity(
            term = term.ifBlank { "Untitled Term" },
            definition = definition,
            subject = subject,
            partOfSpeech = pos,
            phonetic = phonetic,
            exampleUsage = example,
            synonymsJson = syns,
            customMetadataJson = obj.toString()
        )
    }

    private fun normalizeSubject(raw: String): String {
        val lower = raw.lowercase()
        return when {
            "chem" in lower -> "Chemistry"
            "phys" in lower -> "Physics"
            "bio" in lower -> "Biology"
            "math" in lower -> "Mathematics"
            "eng" in lower -> "English"
            else -> "English"
        }
    }

    private fun inferSubject(term: String, definition: String): String {
        val combined = "$term $definition".lowercase()
        return when {
            listOf("reaction", "acid", "base", "mole", "atom", "electron", "orbital", "catalyst", "equilibrium").any { it in combined } -> "Chemistry"
            listOf("force", "velocity", "acceleration", "energy", "momentum", "electric", "magnetic", "wave", "quantum", "optics").any { it in combined } -> "Physics"
            listOf("cell", "dna", "gene", "enzyme", "mitosis", "organism", "respiration", "photosynthesis", "ecology", "tissue").any { it in combined } -> "Biology"
            listOf("derivative", "integral", "matrix", "vector", "function", "polynomial", "limit", "logarithm", "trigonometry").any { it in combined } -> "Mathematics"
            else -> "English"
        }
    }

    private fun DictionaryEntryEntity.toDomain(): DictionaryTerm {
        val synList = try {
            val arr = JSONArray(synonymsJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }

        val metaMap = try {
            val obj = JSONObject(customMetadataJson)
            val map = mutableMapOf<String, String>()
            for (key in obj.keys()) {
                map[key] = obj.optString(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }

        return DictionaryTerm(
            id = id,
            term = term,
            definition = definition,
            subject = subject,
            partOfSpeech = partOfSpeech,
            phonetic = phonetic,
            exampleUsage = exampleUsage,
            synonyms = synList,
            customMetadata = metaMap
        )
    }

    private fun DictionaryTerm.toEntity(): DictionaryEntryEntity {
        return DictionaryEntryEntity(
            id = id,
            term = term,
            definition = definition,
            subject = subject,
            partOfSpeech = partOfSpeech,
            phonetic = phonetic,
            exampleUsage = exampleUsage,
            synonymsJson = JSONArray(synonyms).toString(),
            customMetadataJson = JSONObject(customMetadata).toString()
        )
    }
}

class QuestionBankRepositoryImpl(
    private val dao: CustomQuestionDao
) : IQuestionBankRepository {

    override fun getAllQuestions(): Flow<List<QuestionItem>> {
        return dao.getAllQuestions().map { list -> list.map { it.toDomain() } }
    }

    override fun getQuestionsBySubject(subject: String): Flow<List<QuestionItem>> {
        return dao.getQuestionsBySubject(subject).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveQuestion(question: QuestionItem): String {
        dao.insertQuestion(question.toEntity())
        return question.id
    }

    override suspend fun saveQuestions(questions: List<QuestionItem>) {
        dao.insertQuestions(questions.map { it.toEntity() })
    }

    override suspend fun updateQuestion(question: QuestionItem) {
        dao.updateQuestion(question.toEntity())
    }

    override suspend fun deleteQuestion(id: String) {
        dao.deleteQuestion(id)
    }

    override suspend fun importQuestionsFromJson(
        rawJson: String,
        defaultSubject: String,
        defaultGroup: String
    ): Int {
        val trimmed = rawJson.trim()
        val questionsList = mutableListOf<CustomQuestionEntity>()

        try {
            val jsonArray = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    when {
                        root.has("questions") -> root.getJSONArray("questions")
                        root.has("items") -> root.getJSONArray("items")
                        root.has("data") -> root.getJSONArray("data")
                        else -> {
                            val keys = root.keys()
                            var found: JSONArray? = null
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val candidate = root.optJSONArray(k)
                                if (candidate != null) {
                                    found = candidate
                                    break
                                }
                            }
                            found ?: JSONArray()
                        }
                    }
                }
                else -> JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val prompt = obj.optString("prompt").ifBlank {
                    obj.optString("question").ifBlank {
                        obj.optString("text", "Question #${i + 1}")
                    }
                }

                var optA = "Option A"
                var optB = "Option B"
                var optC = "Option C"
                var optD = "Option D"

                val optionsArray = obj.optJSONArray("options") ?: obj.optJSONArray("choices") ?: obj.optJSONArray("answers")
                if (optionsArray != null) {
                    if (optionsArray.length() > 0) optA = optionsArray.optString(0, "Option A")
                    if (optionsArray.length() > 1) optB = optionsArray.optString(1, "Option B")
                    if (optionsArray.length() > 2) optC = optionsArray.optString(2, "Option C")
                    if (optionsArray.length() > 3) optD = optionsArray.optString(3, "Option D")
                } else {
                    optA = obj.optString("optionA").ifBlank { obj.optString("a", "Option A") }
                    optB = obj.optString("optionB").ifBlank { obj.optString("b", "Option B") }
                    optC = obj.optString("optionC").ifBlank { obj.optString("c", "Option C") }
                    optD = obj.optString("optionD").ifBlank { obj.optString("d", "Option D") }
                }

                val correctRaw = obj.opt("correctOptionIndex") ?: obj.opt("answer") ?: obj.opt("correct")
                val correctIndex = when (correctRaw) {
                    is Number -> correctRaw.toInt().coerceIn(0, 3)
                    is String -> {
                        when (correctRaw.trim().uppercase()) {
                            "A", "0" -> 0
                            "B", "1" -> 1
                            "C", "2" -> 2
                            "D", "3" -> 3
                            else -> 0
                        }
                    }
                    else -> 0
                }

                val explanation = obj.optString("explanation").ifBlank {
                    obj.optString("solution").ifBlank {
                        obj.optString("rationale", "No detailed explanation provided.")
                    }
                }

                val subject = obj.optString("subject").ifBlank { defaultSubject }
                val chapter = obj.optString("chapter").ifBlank {
                    obj.optString("group").ifBlank { defaultGroup }
                }
                val difficulty = obj.optString("difficulty").ifBlank { "Medium" }

                val id = "imported_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

                questionsList.add(
                    CustomQuestionEntity(
                        id = id,
                        subject = subject,
                        chapter = chapter,
                        prompt = prompt,
                        optionA = optA,
                        optionB = optB,
                        optionC = optC,
                        optionD = optD,
                        correctOptionIndex = correctIndex,
                        explanation = explanation,
                        difficulty = difficulty,
                        isUserCustom = true
                    )
                )
            }

            if (questionsList.isNotEmpty()) {
                dao.insertQuestions(questionsList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return questionsList.size
    }

    override suspend fun importQuestionsFromStream(
        inputStream: InputStream,
        defaultSubject: String,
        defaultGroup: String,
        onProgress: ((Int, String) -> Unit)?
    ): Int = withContext(Dispatchers.IO) {
        var totalIngested = 0
        val batch = mutableListOf<CustomQuestionEntity>()
        val batchSize = 50

        try {
            val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))
            reader.isLenient = true

            onProgress?.invoke(0, "Initiating high-speed structured stream...")

            fun safeString(): String {
                val token = reader.peek()
                return when (token) {
                    android.util.JsonToken.STRING, android.util.JsonToken.NUMBER -> reader.nextString()
                    android.util.JsonToken.BOOLEAN -> reader.nextBoolean().toString()
                    android.util.JsonToken.NULL -> {
                        reader.nextNull()
                        ""
                    }
                    else -> {
                        reader.skipValue()
                        ""
                    }
                }
            }

            fun parseQuestionObject(): CustomQuestionEntity {
                var prompt = ""
                var optA = "Option A"
                var optB = "Option B"
                var optC = "Option C"
                var optD = "Option D"
                var correctIndex = 0
                var explanation = ""
                var subject = defaultSubject
                var chapter = defaultGroup
                var difficulty = "Medium"

                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    when (name.lowercase()) {
                        "prompt", "question", "text" -> prompt = safeString()
                        "optiona", "a" -> optA = safeString()
                        "optionb", "b" -> optB = safeString()
                        "optionc", "c" -> optC = safeString()
                        "optiond", "d" -> optD = safeString()
                        "options", "choices", "answers" -> {
                            if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                                reader.beginArray()
                                var idx = 0
                                while (reader.hasNext()) {
                                    val optText = safeString()
                                    when (idx) {
                                        0 -> optA = optText
                                        1 -> optB = optText
                                        2 -> optC = optText
                                        3 -> optD = optText
                                    }
                                    idx++
                                }
                                reader.endArray()
                            } else {
                                reader.skipValue()
                            }
                        }
                        "correctoptionindex", "correct", "answer" -> {
                            val peekToken = reader.peek()
                            if (peekToken == android.util.JsonToken.NUMBER) {
                                correctIndex = try { reader.nextInt().coerceIn(0, 3) } catch (_: Exception) { 0 }
                            } else if (peekToken == android.util.JsonToken.STRING) {
                                val str = reader.nextString().trim().uppercase()
                                correctIndex = when (str) {
                                    "A", "0" -> 0
                                    "B", "1" -> 1
                                    "C", "2" -> 2
                                    "D", "3" -> 3
                                    else -> 0
                                }
                            } else {
                                reader.skipValue()
                            }
                        }
                        "explanation", "solution", "rationale" -> explanation = safeString()
                        "subject" -> subject = safeString()
                        "chapter", "group", "topic" -> chapter = safeString()
                        "difficulty" -> difficulty = safeString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                val qId = "stream_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                return CustomQuestionEntity(
                    id = qId,
                    prompt = prompt.ifBlank { "Sample Question" },
                    optionA = optA,
                    optionB = optB,
                    optionC = optC,
                    optionD = optD,
                    correctOptionIndex = correctIndex,
                    explanation = explanation.ifBlank { "No detailed explanation." },
                    subject = subject.ifBlank { defaultSubject },
                    chapter = chapter.ifBlank { defaultGroup },
                    difficulty = difficulty,
                    isUserCustom = true
                )
            }

            suspend fun parseArrayOfQuestions() {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                        val q = parseQuestionObject()
                        batch.add(q)
                        if (batch.size >= batchSize) {
                            dao.insertQuestions(batch)
                            totalIngested += batch.size
                            batch.clear()
                            onProgress?.invoke(totalIngested, "Streamed $totalIngested questions into Room...")
                        }
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
            }

            val rootToken = reader.peek()
            if (rootToken == android.util.JsonToken.BEGIN_ARRAY) {
                parseArrayOfQuestions()
            } else if (rootToken == android.util.JsonToken.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    val fieldName = reader.nextName()
                    if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                        parseArrayOfQuestions()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }

            if (batch.isNotEmpty()) {
                dao.insertQuestions(batch)
                totalIngested += batch.size
                batch.clear()
            }

            reader.close()
            onProgress?.invoke(totalIngested, "Ingestion complete: $totalIngested questions secured in Room.")
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress?.invoke(totalIngested, "Error during stream parsing: ${e.localizedMessage}")
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }

        totalIngested
    }

    override suspend fun seedDefaultQuestionsIfEmpty() {
        if (dao.getQuestionCount() > 0) return

        val defaults = listOf(
            CustomQuestionEntity(
                id = "mastery_chem_01",
                subject = "Chemistry",
                chapter = "Chemical Equilibrium",
                prompt = "According to Le Chatelier's principle, what happens to an exothermic equilibrium system when temperature is increased?",
                optionA = "Equilibrium shifts to the right (products)",
                optionB = "Equilibrium shifts to the left (reactants)",
                optionC = "The value of Kc increases",
                optionD = "The reaction rate becomes zero",
                correctOptionIndex = 1, // Option B
                explanation = "For an exothermic reaction, heat is released as a product. Adding heat shifts equilibrium toward reactants (left) to absorb the excess energy.",
                difficulty = "Medium",
                isUserCustom = false
            ),
            CustomQuestionEntity(
                id = "mastery_phys_01",
                subject = "Physics",
                chapter = "Rotational Dynamics",
                prompt = "A solid sphere and a hollow cylinder of equal mass and radius roll down an incline without slipping. Which reaches the bottom first?",
                optionA = "The hollow cylinder",
                optionB = "The solid sphere",
                optionC = "Both reach at the exact same instant",
                optionD = "Depends on the angle of inclination",
                correctOptionIndex = 1, // Option B
                explanation = "The solid sphere has a lower moment of inertia (2/5 m r²) compared to the hollow cylinder (m r²), meaning a larger fraction of potential energy converts into translational kinetic energy.",
                difficulty = "Hard",
                isUserCustom = false
            ),
            CustomQuestionEntity(
                id = "mastery_bio_01",
                subject = "Biology",
                chapter = "Cellular Respiration",
                prompt = "During aerobic respiration in eukaryotic cells, in which cellular compartment does the Krebs Cycle occur?",
                optionA = "Cytosol",
                optionB = "Mitochondrial matrix",
                optionC = "Inner mitochondrial membrane cristae",
                optionD = "Nucleolus",
                correctOptionIndex = 1, // Option B
                explanation = "Glycolysis takes place in the cytosol, the Krebs (Citric Acid) Cycle occurs in the mitochondrial matrix, and oxidative phosphorylation occurs along the inner mitochondrial membrane.",
                difficulty = "Easy",
                isUserCustom = false
            ),
            CustomQuestionEntity(
                id = "mastery_math_01",
                subject = "Mathematics",
                chapter = "Calculus & Limits",
                prompt = "What is the derivative of f(x) = ln(3x² + 5)?",
                optionA = "f'(x) = 6x / (3x² + 5)",
                optionB = "f'(x) = 1 / (3x² + 5)",
                optionC = "f'(x) = 6x * (3x² + 5)",
                optionD = "f'(x) = 3x / (3x² + 5)",
                correctOptionIndex = 0, // Option A
                explanation = "Applying the chain rule: d/dx [ln(u)] = (1/u) * u'. Here u = 3x² + 5 and u' = 6x, so f'(x) = 6x / (3x² + 5).",
                difficulty = "Medium",
                isUserCustom = false
            ),
            CustomQuestionEntity(
                id = "mastery_eng_01",
                subject = "English",
                chapter = "Grammar & Conditionals",
                prompt = "Select the grammatically correct sentence representing a past unreal condition (Third Conditional):",
                optionA = "If she studied hard, she would pass the exam.",
                optionB = "If she had studied harder, she would have passed the exam.",
                optionC = "If she will study harder, she will pass the exam.",
                optionD = "If she would have studied, she passed the exam.",
                correctOptionIndex = 1, // Option B
                explanation = "The third conditional follows the structure: If + past perfect, would have + past participle.",
                difficulty = "Medium",
                isUserCustom = false
            )
        )
        dao.insertQuestions(defaults)
    }

    private fun CustomQuestionEntity.toDomain() = QuestionItem(
        id = id,
        prompt = prompt,
        optionA = optionA,
        optionB = optionB,
        optionC = optionC,
        optionD = optionD,
        correctOptionIndex = correctOptionIndex,
        explanation = explanation,
        subject = subject,
        chapter = chapter,
        difficulty = difficulty,
        isUserCustom = isUserCustom
    )

    private fun QuestionItem.toEntity() = CustomQuestionEntity(
        id = id,
        prompt = prompt,
        optionA = optionA,
        optionB = optionB,
        optionC = optionC,
        optionD = optionD,
        correctOptionIndex = correctOptionIndex,
        explanation = explanation,
        subject = subject,
        chapter = chapter,
        difficulty = difficulty,
        isUserCustom = isUserCustom
    )
}
