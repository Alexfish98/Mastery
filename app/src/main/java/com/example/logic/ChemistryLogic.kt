package com.example.logic

import com.example.data.PeriodicTable
import com.example.model.Question
import com.example.model.QuestionProgress
import com.example.model.SrsSchedule
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import kotlin.math.abs

object ChemistryLogic {

    const val DAY_MS: Long = 86400000L
    val INTERVAL_DAYS: DoubleArray = doubleArrayOf(0.007, 1.0, 3.0, 7.0, 14.0, 30.0)

    fun norm(s: String?): String {
        if (s == null) return ""
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFKC)
        var str = normalized.lowercase()
        // Replace unicode subscripts
        val subs = "₀₁₂₃₄₅₆₇₈₉"
        subs.forEachIndexed { i, ch ->
            str = str.replace(ch, '0' + i)
        }
        return str
            .replace(Regex("[−–—]"), "-")
            .replace(Regex("\\b(what is|the answer is|answer|approximately|about)\\b"), " ")
            .replace(Regex("[%°]"), " ")
            .replace(Regex("[^\\p{L}\\p{N}+\\-./]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun hash(s: String): String {
        var x = 2166136261L
        for (ch in s) {
            x = x xor ch.code.toLong()
            x = (x * 16777619L) and 0xFFFFFFFFL
        }
        return java.lang.Long.toString(x, 36)
    }

    private fun parseNumber(s: String): Double? {
        val clean = s.replace(",", "")
        val match = Regex("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").find(clean)
        return match?.value?.toDoubleOrNull()
    }

    fun okOf(q: Question, userVal: String): Boolean {
        val raw = userVal.trim()
        if (raw.isEmpty() || q.answers.isEmpty()) return false

        if (q.type == "number") {
            val userNum = parseNumber(raw) ?: return false
            val tol = q.tol ?: 0.01
            return q.answers.any { ans ->
                val expectedNum = parseNumber(ans)
                if (expectedNum == null) {
                    norm(ans) == norm(raw)
                } else {
                    abs(userNum - expectedNum) <= maxOf(tol, abs(expectedNum) * tol, 1e-8)
                }
            }
        }

        val a = norm(raw)
        if (a.isEmpty()) return false

        val isFormulaType = q.type == "formula" ||
            (q.type == "text" && Regex("\\b(symbol|formula)\\b", RegexOption.IGNORE_CASE).containsMatchIn(q.prompt) &&
                q.answers.all { Regex("^[A-Z(\\[][A-Za-z0-9₀-₉()⁺⁻²³⁴+\\-\\[\\]]*$").matches(it) })

        if (isFormulaType) {
            val cleanUser = Normalizer.normalize(raw, Normalizer.Form.NFKC).replace("\\s+".toRegex(), "")
            return q.answers.any { ans ->
                val cleanAns = Normalizer.normalize(ans, Normalizer.Form.NFKC).replace("\\s+".toRegex(), "")
                cleanAns.equals(cleanUser, ignoreCase = false)
            }
        }

        return q.answers.any { ans ->
            val b = norm(ans)
            if (a == b) return@any true
            if (b.length >= 4 && a.length >= 4 && (a.startsWith("$b ") || a.endsWith(" $b"))) return@any true
            false
        }
    }

    fun srsNext(schedule: SrsSchedule?, ok: Boolean, conf: Int, t: Long = System.currentTimeMillis()): SrsSchedule {
        val r = schedule ?: SrsSchedule(box = 0, due = 0L, ease = 2.5)
        val wasDue = r.due <= t

        var e = r.ease
        e = if (ok) {
            minOf(3.2, e + (if (conf >= 3) 0.1 else if (conf < 2) -0.1 else 0.0))
        } else {
            maxOf(1.3, e - 0.25)
        }
        r.ease = e

        if (!ok) {
            r.box = 0
            r.due = t + (INTERVAL_DAYS[0] * DAY_MS).toLong()
        } else if (wasDue) {
            if (conf >= 2) {
                r.box = minOf(5, r.box + 1)
            }
            val multiplier = if (r.box >= 2) e / 2.5 else 1.0
            r.due = t + (INTERVAL_DAYS[r.box] * DAY_MS * multiplier).toLong()
        }
        return r
    }

    fun isMastered(p: QuestionProgress?, r: SrsSchedule?): Boolean {
        return p != null && r != null && r.box >= 4 && p.ok >= 3 && p.lo == 1 && p.conf >= 2
    }

    fun buildOpts(q: Question, pool: List<Question>): List<String> {
        val acceptedNorm = q.answers.map { norm(it) }.toSet()
        val seen = acceptedNorm.toMutableSet()
        val wrong = mutableListOf<String>()

        val right = q.options.firstOrNull { acceptedNorm.contains(norm(it)) } ?: q.answers.firstOrNull() ?: ""
        seen.add(norm(right))

        fun addDistractor(o: String) {
            val n = norm(o)
            if (n.isNotEmpty() && !seen.contains(n)) {
                seen.add(n)
                wrong.add(o)
            }
        }

        q.options.forEach { addDistractor(it) }

        // From same topic
        pool.filter { it.id != q.id && it.topic == q.topic }.shuffled().forEach { other ->
            other.answers.firstOrNull()?.let { addDistractor(it) }
        }

        // From any topic
        pool.filter { it.id != q.id }.shuffled().forEach { other ->
            other.answers.firstOrNull()?.let { addDistractor(it) }
        }

        val finalChoices = (listOf(right) + wrong.take(3)).shuffled()
        return finalChoices
    }

    fun exp(q: Question): String {
        val first = q.answers.firstOrNull() ?: ""
        return if (q.answers.size > 1) {
            val rest = q.answers.drop(1).take(3).joinToString(", ")
            val more = if (q.answers.size > 4) ", …" else ""
            "$first (also accepted: $rest$more)"
        } else {
            first
        }
    }

    data class AnalyzeResult(
        val ok: List<Question> = emptyList(),
        val rej: List<Pair<Int, String>> = emptyList(),
        val total: Int = 0,
        val err: String? = null
    )

    fun analyze(jsonText: String): AnalyzeResult {
        try {
            var trimmed = jsonText.trim()
            if (trimmed.startsWith("```json")) trimmed = trimmed.removePrefix("```json").trim()
            if (trimmed.startsWith("```")) trimmed = trimmed.removePrefix("```").trim()
            if (trimmed.endsWith("```")) trimmed = trimmed.removeSuffix("```").trim()

            var array: JSONArray? = null
            if (trimmed.startsWith("[")) {
                array = JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                array = root.optJSONArray("questions")
            }

            if (array == null) {
                return AnalyzeResult(err = "Expected a JSON array of questions or {\"questions\": [...]}")
            }
            if (array.length() == 0) {
                return AnalyzeResult(err = "The JSON contains no questions.")
            }
            if (array.length() > 5000) {
                return AnalyzeResult(err = "Too many questions (${array.length()}). Limit is 5000.")
            }

            val okList = mutableListOf<Question>()
            val rejList = mutableListOf<Pair<Int, String>>()
            val seenIds = mutableSetOf<String>()
            val seenPrompts = mutableSetOf<String>()

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                if (item == null) {
                    rejList.add(Pair(i + 1, "Item is not a valid JSON object"))
                    continue
                }
                try {
                    val q = Question.fromJson(item)
                    if (q.prompt.isBlank()) throw IllegalArgumentException("Missing prompt")
                    if (q.answers.isEmpty()) throw IllegalArgumentException("Missing answer")
                    val normPrompt = norm(q.prompt)
                    if (seenPrompts.contains(normPrompt)) {
                        throw IllegalArgumentException("Duplicate question prompt in import")
                    }
                    if (seenIds.contains(q.id)) {
                        throw IllegalArgumentException("Duplicate ID in import: ${q.id}")
                    }
                    seenIds.add(q.id)
                    seenPrompts.add(normPrompt)
                    okList.add(q)
                } catch (e: Exception) {
                    rejList.add(Pair(i + 1, e.message ?: "Invalid question record"))
                }
            }

            return AnalyzeResult(ok = okList, rej = rejList, total = array.length())
        } catch (e: Exception) {
            return AnalyzeResult(err = "Invalid JSON: ${e.message}")
        }
    }

    fun analyzeStream(inputStream: java.io.InputStream): AnalyzeResult {
        return try {
            val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))
            val okList = mutableListOf<Question>()
            val rejList = mutableListOf<Pair<Int, String>>()
            val seenIds = mutableSetOf<String>()
            val seenPrompts = mutableSetOf<String>()
            var totalCount = 0

            fun readObjectToMap(): Map<String, Any?> {
                val map = mutableMapOf<String, Any?>()
                reader.beginObject()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    when (reader.peek()) {
                        android.util.JsonToken.STRING -> map[key] = reader.nextString()
                        android.util.JsonToken.NUMBER -> {
                            val numStr = reader.nextString()
                            map[key] = numStr.toDoubleOrNull() ?: numStr
                        }
                        android.util.JsonToken.BOOLEAN -> map[key] = reader.nextBoolean()
                        android.util.JsonToken.NULL -> {
                            reader.nextNull()
                            map[key] = null
                        }
                        android.util.JsonToken.BEGIN_ARRAY -> {
                            val list = mutableListOf<Any?>()
                            reader.beginArray()
                            while (reader.hasNext()) {
                                when (reader.peek()) {
                                    android.util.JsonToken.STRING -> list.add(reader.nextString())
                                    android.util.JsonToken.NUMBER -> {
                                        val s = reader.nextString()
                                        list.add(s.toDoubleOrNull() ?: s)
                                    }
                                    android.util.JsonToken.BOOLEAN -> list.add(reader.nextBoolean())
                                    android.util.JsonToken.NULL -> {
                                        reader.nextNull()
                                        list.add(null)
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endArray()
                            map[key] = list
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                return map
            }

            fun parseItem() {
                totalCount++
                if (reader.peek() != android.util.JsonToken.BEGIN_OBJECT) {
                    reader.skipValue()
                    rejList.add(Pair(totalCount, "Item is not a valid JSON object"))
                    return
                }
                val map = readObjectToMap()
                try {
                    val jsonObject = JSONObject(map)
                    val q = Question.fromJson(jsonObject)
                    if (q.prompt.isBlank()) throw IllegalArgumentException("Missing prompt")
                    if (q.answers.isEmpty()) throw IllegalArgumentException("Missing answer")
                    val normPrompt = norm(q.prompt)
                    if (seenPrompts.contains(normPrompt)) {
                        throw IllegalArgumentException("Duplicate question prompt in import")
                    }
                    if (seenIds.contains(q.id)) {
                        throw IllegalArgumentException("Duplicate ID in import: ${q.id}")
                    }
                    seenIds.add(q.id)
                    seenPrompts.add(normPrompt)
                    okList.add(q)
                } catch (e: Exception) {
                    rejList.add(Pair(totalCount, e.message ?: "Invalid question record"))
                }
            }

            val rootToken = reader.peek()
            if (rootToken == android.util.JsonToken.BEGIN_ARRAY) {
                reader.beginArray()
                while (reader.hasNext()) {
                    parseItem()
                }
                reader.endArray()
            } else if (rootToken == android.util.JsonToken.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            parseItem()
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            } else {
                reader.close()
                return AnalyzeResult(err = "Expected a JSON array or object containing questions.")
            }
            reader.close()

            AnalyzeResult(ok = okList, rej = rejList, total = totalCount)
        } catch (e: Exception) {
            AnalyzeResult(err = "Stream parsing failed: ${e.message}")
        }
    }

    data class PlanResult(
        val add: List<Question> = emptyList(),
        val rep: List<Question> = emptyList(),
        val skip: List<Pair<Question, String>> = emptyList()
    )

    fun plan(okList: List<Question>, current: List<Question>): PlanResult {
        val byId = current.associateBy { it.id }
        val byPrompt = current.associateBy { norm(it.prompt) }

        val addList = mutableListOf<Question>()
        val repList = mutableListOf<Question>()
        val skipList = mutableListOf<Pair<Question, String>>()

        okList.forEach { q ->
            val existingById = byId[q.id]
            val existingByPrompt = byPrompt[norm(q.prompt)]

            if (existingById != null) {
                if (norm(existingById.prompt) != norm(q.prompt)) {
                    skipList.add(Pair(q, "ID ${q.id} is already used by a different question"))
                } else if (existingById == q) {
                    skipList.add(Pair(q, "Identical question already in bank"))
                } else {
                    repList.add(q)
                }
            } else if (existingByPrompt != null) {
                skipList.add(Pair(q, "Same question text as existing ${existingByPrompt.id}"))
            } else {
                addList.add(q)
            }
        }

        return PlanResult(add = addList, rep = repList, skip = skipList)
    }

    data class DiagnosticCheck(val name: String, val passed: Boolean, val message: String = "")

    fun runDiagnostics(currentBank: List<Question>): List<DiagnosticCheck> {
        val results = mutableListOf<DiagnosticCheck>()

        fun test(name: String, block: () -> Boolean) {
            try {
                val ok = block()
                results.add(DiagnosticCheck(name, ok, if (ok) "" else "Failed assertion"))
            } catch (e: Exception) {
                results.add(DiagnosticCheck(name, false, e.message ?: "Exception"))
            }
        }

        test("Normalization strips punctuation and whitespace") {
            norm("  Hello,   World!  ") == "hello world"
        }

        test("Rejects malformed JSON") {
            analyze("{bad").err != null
        }

        test("Accepts q / a / why field variants") {
            val r = analyze("[{\"q\":\"2+2?\",\"a\":\"4\",\"why\":\"math\"}]")
            r.ok.isNotEmpty() && r.ok[0].prompt == "2+2?" && r.ok[0].explanation == "math"
        }

        test("Question IDs are complete and unique in current bank") {
            val ids = currentBank.map { it.id }
            ids.distinct().size == ids.size && currentBank.all { it.answers.isNotEmpty() && it.prompt.isNotBlank() }
        }

        test("SRS scheduler: resets to box 0 on wrong answer") {
            val next = srsNext(SrsSchedule(box = 3, due = 0L), false, 2)
            next.box == 0
        }

        test("SRS scheduler: advances box on confident correct answer") {
            val next = srsNext(SrsSchedule(box = 1, due = 0L), true, 3)
            next.box == 2
        }

        test("Periodic table contains 118 elements") {
            PeriodicTable.elements.size == 118 && PeriodicTable.elements.map { it.symbol }.distinct().size == 118
        }

        test("Formula parser handles Ca(OH)2") {
            val counts = PeriodicTable.parseFormula("Ca(OH)2")
            counts["Ca"] == 1 && counts["O"] == 2 && counts["H"] == 2
        }

        test("Molar mass calculation of hydrate CuSO4·5H2O") {
            val mm = PeriodicTable.calculateMolarMass("CuSO4·5H2O")
            abs(mm.totalMass - 249.68) < 0.2
        }

        test("Balances chemical equation H2 + O2 -> H2O") {
            val res = PeriodicTable.balanceEquation("H2 + O2 -> H2O")
            res.coefficients == listOf(2, 1, 2)
        }

        test("Balances chemical equation C3H8 + O2 -> CO2 + H2O") {
            val res = PeriodicTable.balanceEquation("C3H8 + O2 -> CO2 + H2O")
            res.coefficients == listOf(1, 5, 3, 4)
        }

        test("Balances equation with atom conservation check") {
            val res = PeriodicTable.balanceEquation("Al + HCl -> AlCl3 + H2")
            res.atomBalances.all { it.isBalanced }
        }

        return results
    }
}
