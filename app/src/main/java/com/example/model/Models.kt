package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class Question(
    val id: String,
    val prompt: String,
    val answers: List<String>,
    val options: List<String> = emptyList(),
    val topic: String = "General",
    val difficulty: String = "medium",
    val type: String = "text", // "text", "number", "formula"
    val unit: String? = null,
    val img: String? = null, // URI, file path, or base64 data URI
    val tol: Double? = null,
    val explanation: String? = null,
    val whyCorrect: String? = null,
    val commonMistakes: String? = null,
    val concept: String? = null,
    val workedExample: String? = null,
    val memoryTip: String? = null,
    val examTrap: String? = null,
    val hints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val references: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("prompt", prompt)
        obj.put("answers", JSONArray(answers))
        if (options.isNotEmpty()) obj.put("options", JSONArray(options))
        obj.put("topic", topic)
        obj.put("difficulty", difficulty)
        obj.put("type", type)
        unit?.let { obj.put("unit", it) }
        img?.let { obj.put("img", it) }
        tol?.let { obj.put("tol", it) }
        explanation?.let { obj.put("explanation", it) }
        whyCorrect?.let { obj.put("whyCorrect", it) }
        commonMistakes?.let { obj.put("commonMistakes", it) }
        concept?.let { obj.put("concept", it) }
        workedExample?.let { obj.put("workedExample", it) }
        memoryTip?.let { obj.put("memoryTip", it) }
        examTrap?.let { obj.put("examTrap", it) }
        if (hints.isNotEmpty()) obj.put("hints", JSONArray(hints))
        if (tags.isNotEmpty()) obj.put("tags", JSONArray(tags))
        if (references.isNotEmpty()) obj.put("references", JSONArray(references))
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): Question {
            val prompt = obj.optString("prompt").ifBlank {
                obj.optString("question").ifBlank { obj.optString("q") }
            }
            val answersList = mutableListOf<String>()
            val ansRaw = obj.opt("answers") ?: obj.opt("answer") ?: obj.opt("a")
            if (ansRaw is JSONArray) {
                for (i in 0 until ansRaw.length()) {
                    val s = ansRaw.optString(i).trim()
                    if (s.isNotEmpty()) answersList.add(s)
                }
            } else if (ansRaw != null) {
                val s = ansRaw.toString().trim()
                if (s.isNotEmpty()) answersList.add(s)
            }

            val optsList = mutableListOf<String>()
            val optsRaw = obj.opt("options") ?: obj.opt("choices")
            if (optsRaw is JSONArray) {
                for (i in 0 until optsRaw.length()) {
                    val s = optsRaw.optString(i).trim()
                    if (s.isNotEmpty()) optsList.add(s)
                }
            } else {
                val optA = obj.optString("optionA").ifBlank { obj.optString("optA") }
                val optB = obj.optString("optionB").ifBlank { obj.optString("optB") }
                val optC = obj.optString("optionC").ifBlank { obj.optString("optC") }
                val optD = obj.optString("optionD").ifBlank { obj.optString("optD") }
                listOf(optA, optB, optC, optD).filter { it.isNotBlank() }.forEach { optsList.add(it) }
            }

            if (answersList.isEmpty() && optsList.isNotEmpty()) {
                val correctRaw = obj.opt("correctOptionIndex") ?: obj.opt("correct") ?: obj.opt("answer")
                val idx = when (correctRaw) {
                    is Number -> correctRaw.toInt()
                    is String -> when (correctRaw.trim().uppercase()) {
                        "A", "0" -> 0
                        "B", "1" -> 1
                        "C", "2" -> 2
                        "D", "3" -> 3
                        else -> 0
                    }
                    else -> 0
                }
                if (idx in optsList.indices) {
                    answersList.add(optsList[idx])
                } else {
                    answersList.add(optsList[0])
                }
            }

            val hintsList = mutableListOf<String>()
            obj.optJSONArray("hints")?.let { arr ->
                for (i in 0 until arr.length()) hintsList.add(arr.optString(i))
            }

            val tagsList = mutableListOf<String>()
            obj.optJSONArray("tags")?.let { arr ->
                for (i in 0 until arr.length()) tagsList.add(arr.optString(i))
            }

            val refsList = mutableListOf<String>()
            obj.optJSONArray("references")?.let { arr ->
                for (i in 0 until arr.length()) refsList.add(arr.optString(i))
            }

            val id = obj.optString("id").ifBlank {
                "q_" + prompt.lowercase().filter { it.isLetterOrDigit() }.hashCode().toString(36)
            }

            return Question(
                id = id,
                prompt = prompt,
                answers = answersList.distinct(),
                options = optsList.distinct(),
                topic = obj.optString("topic", "General").ifBlank { "General" },
                difficulty = obj.optString("difficulty", "medium").lowercase(),
                type = obj.optString("type", "text"),
                unit = obj.optString("unit").takeIf { it.isNotBlank() },
                img = obj.optString("img").takeIf { it.isNotBlank() },
                tol = if (obj.has("tol")) obj.optDouble("tol") else null,
                explanation = (obj.optString("explanation").ifBlank { obj.optString("why") }).takeIf { it.isNotBlank() },
                whyCorrect = obj.optString("whyCorrect").takeIf { it.isNotBlank() },
                commonMistakes = obj.optString("commonMistakes").takeIf { it.isNotBlank() },
                concept = obj.optString("concept").takeIf { it.isNotBlank() },
                workedExample = obj.optString("workedExample").takeIf { it.isNotBlank() },
                memoryTip = (obj.optString("memoryTip").ifBlank { obj.optString("tip") }).takeIf { it.isNotBlank() },
                examTrap = obj.optString("examTrap").takeIf { it.isNotBlank() },
                hints = hintsList,
                tags = tagsList,
                references = refsList
            )
        }
    }
}

data class QuestionBank(
    val id: String,
    val name: String,
    val at: Long = System.currentTimeMillis(),
    val questions: List<Question> = emptyList()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("at", at)
        val arr = JSONArray()
        questions.forEach { arr.put(it.toJson()) }
        obj.put("questions", arr)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): QuestionBank {
            val qList = mutableListOf<Question>()
            obj.optJSONArray("questions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { qList.add(Question.fromJson(it)) }
                }
            }
            return QuestionBank(
                id = obj.optString("id", "bank_default"),
                name = obj.optString("name", "Question Bank"),
                at = obj.optLong("at", System.currentTimeMillis()),
                questions = qList
            )
        }
    }
}

data class QuestionProgress(
    var att: Int = 0,
    var ok: Int = 0,
    var bad: Int = 0,
    var conf: Int = 0, // 1: Unsure, 2: Fairly sure, 3: Sure
    var ans: String = "",
    var at: Long = 0L,
    var lo: Int = 0, // last was ok (1) or not (0)
    var state: String = "learning", // "learning", "mastered"
    var nb: Boolean = false, // in incorrect notebook
    var rev: Long = 0L,
    var fm: Long = 0L, // first missed
    var man: Int = 0, // manual override
    var rep: String? = null // flag reason
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("att", att)
        obj.put("ok", ok)
        obj.put("bad", bad)
        obj.put("conf", conf)
        obj.put("ans", ans)
        obj.put("at", at)
        obj.put("lo", lo)
        obj.put("state", state)
        obj.put("nb", nb)
        obj.put("rev", rev)
        obj.put("fm", fm)
        obj.put("man", man)
        rep?.let { obj.put("rep", it) }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): QuestionProgress {
            return QuestionProgress(
                att = obj.optInt("att", 0),
                ok = obj.optInt("ok", 0),
                bad = obj.optInt("bad", 0),
                conf = obj.optInt("conf", 0),
                ans = obj.optString("ans", ""),
                at = obj.optLong("at", 0L),
                lo = obj.optInt("lo", 0),
                state = obj.optString("state", "learning"),
                nb = obj.optBoolean("nb", false),
                rev = obj.optLong("rev", 0L),
                fm = obj.optLong("fm", 0L),
                man = obj.optInt("man", 0),
                rep = obj.optString("rep").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class SrsSchedule(
    var box: Int = 0,
    var due: Long = 0L,
    var ease: Double = 2.5
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("box", box)
        obj.put("due", due)
        obj.put("ease", ease)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): SrsSchedule {
            return SrsSchedule(
                box = obj.optInt("box", 0),
                due = obj.optLong("due", 0L),
                ease = obj.optDouble("ease", 2.5)
            )
        }
    }
}

data class AttemptRecord(
    val q: String,
    val b: String,
    val ok: Int,
    val ans: String,
    val at: Long = System.currentTimeMillis(),
    val c: Int = 2,
    val m: String = "practice",
    val h: Int = 0,
    val s: Int = 0,
    val d: String? = null // diagnosis: "concept", "careless", "calc", "misread", "guess"
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("q", q)
        obj.put("b", b)
        obj.put("ok", ok)
        obj.put("ans", ans)
        obj.put("at", at)
        obj.put("c", c)
        obj.put("m", m)
        obj.put("h", h)
        obj.put("s", s)
        d?.let { obj.put("d", it) }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): AttemptRecord {
            return AttemptRecord(
                q = obj.optString("q"),
                b = obj.optString("b"),
                ok = obj.optInt("ok"),
                ans = obj.optString("ans"),
                at = obj.optLong("at", System.currentTimeMillis()),
                c = obj.optInt("c", 2),
                m = obj.optString("m", "practice"),
                h = obj.optInt("h", 0),
                s = obj.optInt("s", 0),
                d = obj.optString("d").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class ExamResult(
    val at: Long = System.currentTimeMillis(),
    val bank: String,
    val total: Int,
    val ok: Int,
    val ans: Int,
    val secs: Int,
    val rows: List<ExamRow>
) {
    data class ExamRow(val id: String, val ok: Int, val ans: String)

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("at", at)
        obj.put("bank", bank)
        obj.put("total", total)
        obj.put("ok", ok)
        obj.put("ans", ans)
        obj.put("secs", secs)
        val arr = JSONArray()
        rows.forEach {
            val r = JSONObject()
            r.put("id", it.id)
            r.put("ok", it.ok)
            r.put("ans", it.ans)
            arr.put(r)
        }
        obj.put("rows", arr)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): ExamResult {
            val rows = mutableListOf<ExamRow>()
            obj.optJSONArray("rows")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let {
                        rows.add(ExamRow(it.optString("id"), it.optInt("ok"), it.optString("ans")))
                    }
                }
            }
            return ExamResult(
                at = obj.optLong("at", System.currentTimeMillis()),
                bank = obj.optString("bank"),
                total = obj.optInt("total"),
                ok = obj.optInt("ok"),
                ans = obj.optInt("ans"),
                secs = obj.optInt("secs"),
                rows = rows
            )
        }
    }
}

data class SessionLog(
    val at: Long = System.currentTimeMillis(),
    val mode: String,
    val title: String,
    val bank: String,
    val ok: Int,
    val ans: Int,
    val total: Int,
    val secs: Int
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("at", at)
        obj.put("mode", mode)
        obj.put("title", title)
        obj.put("bank", bank)
        obj.put("ok", ok)
        obj.put("ans", ans)
        obj.put("total", total)
        obj.put("secs", secs)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): SessionLog {
            return SessionLog(
                at = obj.optLong("at", System.currentTimeMillis()),
                mode = obj.optString("mode"),
                title = obj.optString("title"),
                bank = obj.optString("bank"),
                ok = obj.optInt("ok"),
                ans = obj.optInt("ans"),
                total = obj.optInt("total"),
                secs = obj.optInt("secs")
            )
        }
    }
}

object BuilderInfo {
    const val APP_NAME = "Mastery Study Suite"
    const val NAME = "Alexander Fiseha"
    const val ACADEMIC_BACKGROUND = "Grade 12 Student"
    const val GRADE_ROLE = "Grade 12 Natural Science Scholar & Software Developer"
    const val SHORT_ROLE = "Grade 12 Natural Science"
    const val ORIGIN_LOCATION = "Mentebteb, Tahtay Adyabo, Tigray, Ethiopia"
    const val MISSION_STATEMENT = "Built explicitly to conquer the EUEE with a target score of 100/100."
    const val DISTRIBUTION_MODEL = "Premium-tier execution, entirely free for public use with zero paywalls, premium restrictions, or external ads."
    const val PHONES = "0993623238, 0942534265"
    const val PHONE_PRIMARY = "0993623238"
    const val PHONE_SECONDARY = "0942534265"
    const val EMAIL = "alexanderfiseha98@gmail.com"
    const val BACKUP_EMAIL = "alexawroraiq@gmail.com"
    const val BIO = "Designed and developed Mastery Study Suite to empower Ethiopian Grade 12 students with a 100% offline-ready ecosystem featuring streaming question ingestion, science calculation suites, spaced repetition, and high-performance offline tools."
    const val APP_VERSION = "3.0 (Mastery Enterprise Edition)"
}

data class UserPreferences(
    var theme: String = "auto", // "light", "dark", "auto"
    var accent: String = "Teal", // "Teal", "Blue", "Purple", "Pink", "Orange", "Green", "Graphite"
    var selectedFont: String = "Modern Sans", // "Modern Sans", "Ethiopic Serif", "Classic Serif", "Scientific Mono"
    var bank: String? = null,
    var goal: Int = 20,
    var exd: String = "", // exam date string
    var exs: Int = 60, // seconds per exam question
    var snd: Boolean = true, // sound feedback
    var mins: Int = 120, // minutes per day
    var ncap: Int = 10, // new questions per day cap
    var pri: MutableMap<String, Int> = mutableMapOf(),
    var lastExport: Long = 0L,
    var studentName: String = BuilderInfo.NAME,
    var studentGrade: String = BuilderInfo.SHORT_ROLE,
    var studentPhone: String = BuilderInfo.PHONES,
    var studentEmail: String = BuilderInfo.EMAIL,
    var dashboardConfigJson: String = ""
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("theme", theme)
        obj.put("accent", accent)
        obj.put("selectedFont", selectedFont)
        bank?.let { obj.put("bank", it) }
        obj.put("goal", goal)
        obj.put("exd", exd)
        obj.put("exs", exs)
        obj.put("snd", snd)
        obj.put("mins", mins)
        obj.put("ncap", ncap)
        obj.put("lastExport", lastExport)
        obj.put("studentName", studentName)
        obj.put("studentGrade", studentGrade)
        obj.put("studentPhone", studentPhone)
        obj.put("studentEmail", studentEmail)
        obj.put("dashboardConfigJson", dashboardConfigJson)
        val priObj = JSONObject()
        pri.forEach { (k, v) -> priObj.put(k, v) }
        obj.put("pri", priObj)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): UserPreferences {
            val priMap = mutableMapOf<String, Int>()
            obj.optJSONObject("pri")?.let { p ->
                p.keys().forEach { k -> priMap[k] = p.optInt(k, 3) }
            }
            return UserPreferences(
                theme = obj.optString("theme", "auto"),
                accent = obj.optString("accent", "Teal"),
                selectedFont = obj.optString("selectedFont", "Modern Sans"),
                bank = obj.optString("bank").takeIf { it.isNotBlank() },
                goal = obj.optInt("goal", 20),
                exd = obj.optString("exd", ""),
                exs = obj.optInt("exs", 60),
                snd = obj.optBoolean("snd", true),
                mins = obj.optInt("mins", 120),
                ncap = obj.optInt("ncap", 10),
                pri = priMap,
                lastExport = obj.optLong("lastExport", 0L),
                studentName = obj.optString("studentName", BuilderInfo.NAME),
                studentGrade = obj.optString("studentGrade", BuilderInfo.SHORT_ROLE),
                studentPhone = obj.optString("studentPhone", BuilderInfo.PHONES),
                studentEmail = obj.optString("studentEmail", BuilderInfo.EMAIL),
                dashboardConfigJson = obj.optString("dashboardConfigJson", "")
            )
        }
    }
}

data class StudyNote(
    val id: String,
    val title: String,
    val subject: String = "General",
    val body: String,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("subject", subject)
        obj.put("body", body)
        obj.put("created", created)
        obj.put("updated", updated)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): StudyNote {
            val title = obj.optString("title").ifBlank { obj.optString("name") }
            val body = obj.optString("body").ifBlank {
                obj.optString("content").ifBlank {
                    obj.optString("text").ifBlank { obj.optString("note") }
                }
            }
            val created = obj.optLong("created", System.currentTimeMillis())
            val updated = obj.optLong("updated", created)
            return StudyNote(
                id = obj.optString("id", "note_" + System.currentTimeMillis().toString(36)),
                title = title.ifBlank { "Untitled Note" },
                subject = obj.optString("subject", "General").ifBlank { "General" },
                body = body,
                created = created,
                updated = updated
            )
        }
    }
}

data class PlannerTask(
    val id: String,
    val title: String,
    val subject: String = "General",
    val due: String = "",
    val priority: String = "Normal", // "High", "Normal", "Low"
    var done: Boolean = false,
    val created: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("subject", subject)
        obj.put("due", due)
        obj.put("priority", priority)
        obj.put("done", done)
        obj.put("created", created)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): PlannerTask {
            return PlannerTask(
                id = obj.optString("id", "t_" + System.currentTimeMillis().toString(36)),
                title = obj.optString("title", "Study Task"),
                subject = obj.optString("subject", "General"),
                due = obj.optString("due", ""),
                priority = obj.optString("priority", "Normal"),
                done = obj.optBoolean("done", false),
                created = obj.optLong("created", System.currentTimeMillis())
            )
        }
    }
}

data class StandaloneFlashcard(
    val id: String,
    val front: String,
    val back: String,
    val subject: String = "General",
    val created: Long = System.currentTimeMillis(),
    var reviews: Int = 0,
    var known: Int = 0,
    var again: Int = 0
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("front", front)
        obj.put("back", back)
        obj.put("subject", subject)
        obj.put("created", created)
        obj.put("reviews", reviews)
        obj.put("known", known)
        obj.put("again", again)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): StandaloneFlashcard {
            val front = obj.optString("front").ifBlank { obj.optString("term") }
            val back = obj.optString("back").ifBlank { obj.optString("answer") }
            return StandaloneFlashcard(
                id = obj.optString("id", "f_" + System.currentTimeMillis().toString(36)),
                front = front,
                back = back,
                subject = obj.optString("subject", "General"),
                created = obj.optLong("created", System.currentTimeMillis()),
                reviews = obj.optInt("reviews", 0),
                known = obj.optInt("known", 0),
                again = obj.optInt("again", 0)
            )
        }
    }
}

