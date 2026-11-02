package com.example.logic

import com.example.data.local.ChatHistory
import com.example.data.local.Flashcard
import com.example.data.local.OfflineNote
import com.example.data.local.Textbook
import com.example.model.BuilderInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 6. MASTERY LOCAL INTELLIGENCE MATRIX FRAMEWORK
 * Generates structured on-device context matrices and processes offline intelligent responses.
 */
object IqaiMatrixEngine {

    /**
     * Processing pipeline method that loads all saved ChatHistory entries,
     * local notes, and academic documents into a structural string data structure.
     */
    fun feedContextToMastery(
        chatHistory: List<ChatHistory>,
        notes: List<OfflineNote>,
        textbooks: List<Textbook>,
        flashcards: List<Flashcard>
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()

        sb.appendLine("==================================================================")
        sb.appendLine("MASTERY LOCAL INTELLIGENCE CONTEXT MATRIX (100% OFFLINE)")
        sb.appendLine("Timestamp: ${dateFormat.format(Date())}")
        sb.appendLine("Student: ${BuilderInfo.NAME} (${BuilderInfo.GRADE_ROLE})")
        sb.appendLine("Execution Environment: On-Device Secure JVM / Zero Network Socket")
        sb.appendLine("==================================================================")
        sb.appendLine()

        // 1. Chat History Stream
        sb.appendLine("### [SECTION 1: CHAT INTERACTION HISTORY & REASONING MEMORY]")
        if (chatHistory.isEmpty()) {
            sb.appendLine("(No prior conversation history recorded)")
        } else {
            chatHistory.takeLast(15).forEach { msg ->
                val timeStr = dateFormat.format(Date(msg.timestamp))
                sb.appendLine("[$timeStr] ${msg.role.uppercase()}: ${msg.content}")
            }
        }
        sb.appendLine()

        // 2. Offline Notes & Wiki Links
        sb.appendLine("### [SECTION 2: LOCAL KNOWLEDGE BASE & WIKI NOTES]")
        if (notes.isEmpty()) {
            sb.appendLine("(No offline notes authored yet)")
        } else {
            notes.forEach { note ->
                val wikiLinks = Regex("\\[\\[([^\\]]+)\\]\\]").findAll(note.content)
                    .map { it.groupValues[1] }.toList()
                sb.appendLine("• NOTE [ID: ${note.id}]: \"${note.title}\"")
                if (wikiLinks.isNotEmpty()) {
                    sb.appendLine("  Outgoing Wiki-Links: ${wikiLinks.joinToString(", ")}")
                }
                sb.appendLine("  Excerpt: ${note.content.take(160).replace("\n", " ")}...")
            }
        }
        sb.appendLine()

        // 3. Textbooks & Document Library
        sb.appendLine("### [SECTION 3: LOCAL TEXTBOOKS & DOCUMENTS]")
        if (textbooks.isEmpty()) {
            sb.appendLine("(No local PDF/EPUB textbooks loaded)")
        } else {
            textbooks.forEach { book ->
                val progressPct = if (book.totalPages > 0) (book.lastPageRead * 100 / book.totalPages) else 0
                sb.appendLine("• TEXTBOOK [ID: ${book.id}]: \"${book.title}\"")
                sb.appendLine("  Read: Page ${book.lastPageRead} of ${book.totalPages} ($progressPct%)")
            }
        }
        sb.appendLine()

        // 4. Flashcard Review Mastery Matrix
        sb.appendLine("### [SECTION 4: ACTIVE RECALL (SRS) MASTERY MATRIX]")
        val dueCards = flashcards.filter { it.dueDate <= System.currentTimeMillis() }
        val lowEaseCards = flashcards.filter { it.easeFactor < 2.0 }
        sb.appendLine("Total Flashcards: ${flashcards.size} | Due Now: ${dueCards.size} | Low Ease (< 2.0): ${lowEaseCards.size}")
        if (flashcards.isNotEmpty()) {
            sb.appendLine("Sample Cards for Review:")
            flashcards.take(5).forEach { card ->
                sb.appendLine("  [${card.deckName}] Q: ${card.frontText.take(60)} -> Interval: ${card.interval}d (EF: %.2f)".format(card.easeFactor))
            }
        }
        sb.appendLine("==================================================================")

        return sb.toString()
    }

    /**
     * Local intelligent offline reasoning engine for Mastery student assistant.
     */
    fun processOfflineQuery(
        query: String,
        notes: List<OfflineNote>,
        textbooks: List<Textbook>,
        flashcards: List<Flashcard>
    ): String {
        val q = query.trim().lowercase(Locale.getDefault())

        // Check if querying notes
        val matchingNote = notes.firstOrNull { note ->
            note.title.lowercase(Locale.getDefault()).contains(q) ||
                    q.contains(note.title.lowercase(Locale.getDefault()))
        }
        if (matchingNote != null) {
            return "📖 **Found in your Offline Notes: \"${matchingNote.title}\"**\n\n" +
                    "${matchingNote.content}\n\n" +
                    "_Tip: You can use `[[${matchingNote.title}]]` in any other note to link back to this concept._"
        }

        // Check for common EUEE Science & Academic queries
        if (q.contains("buffer") || q.contains("henderson") || q.contains("ph")) {
            return "🧪 **Buffer Solutions & pH Calculation**\n\n" +
                    "A buffer solution maintains a nearly constant pH when small quantities of acid or base are introduced.\n\n" +
                    "• **Henderson-Hasselbalch Equation**:\n" +
                    "  pH = pKa + log([A⁻] / [HA])\n\n" +
                    "• **EUEE Exam Trap**: A buffer has maximum buffering capacity when [A⁻] = [HA], meaning pH = pKa."
        }

        if (q.contains("le chatelier") || q.contains("equilibrium")) {
            return "⚖️ **Le Chatelier's Principle (High-Yield)**\n\n" +
                    "When an external stress is applied to a chemical system at dynamic equilibrium:\n" +
                    "1. **Concentration**: Adding reactant shifts equilibrium **Right** (forward).\n" +
                    "2. **Pressure/Volume**: Increasing pressure shifts toward fewer gaseous moles.\n" +
                    "3. **Temperature**:\n" +
                    "   - Exothermic (ΔH < 0): Heating shifts **Left** (decreases Kc).\n" +
                    "   - Endothermic (ΔH > 0): Heating shifts **Right** (increases Kc)."
        }

        if (q.contains("mechanics") || q.contains("velocity") || q.contains("kinematics") || q.contains("escape")) {
            return "🚀 **Physics Kinematics & Gravitation**\n\n" +
                    "• **Escape Velocity Formula**:\n" +
                    "  v_esc = √(2GM / R)\n\n" +
                    "• **Uniform Acceleration Equations**:\n" +
                    "  1. v = u + at\n" +
                    "  2. s = ut + (1/2)at²\n" +
                    "  3. v² = u² + 2as"
        }

        if (q.contains("flashcard") || q.contains("srs") || q.contains("anki") || q.contains("review")) {
            val due = flashcards.filter { it.dueDate <= System.currentTimeMillis() }
            return "🧠 **Active Recall (SRS) Status**\n\n" +
                    "You have **${due.size} flashcards** scheduled for review right now across **${flashcards.size} total cards**.\n\n" +
                    "• **SM-2 Recommendation**: Always rate honestly:\n" +
                    "  - **Again**: Missed completely (resets to 1 day).\n" +
                    "  - **Hard**: Recalled with substantial hesitation.\n" +
                    "  - **Good**: Solid recall within target time.\n" +
                    "  - **Easy**: Instant effortless retrieval (grants bonus interval)."
        }

        if (q.contains("textbook") || q.contains("pdf") || q.contains("book")) {
            if (textbooks.isEmpty()) {
                return "📚 **Textbook Reader**\n\n" +
                        "No local PDF/EPUB textbooks have been imported yet. Open the **Textbook Reader** tab and tap **Import Local PDF** to load your school textbooks directly into device memory."
            } else {
                val list = textbooks.joinToString("\n") { "• ${it.title} (Page ${it.lastPageRead}/${it.totalPages})" }
                return "📚 **Your Saved Local Textbooks**:\n\n$list\n\nTap on any textbook in the reader module to resume reading from memory."
            }
        }

        if (q.contains("focus") || q.contains("timer") || q.contains("strict")) {
            return "⏱️ **Strict Mode Focus Timer**\n\n" +
                    "The Mastery focus timer operates under **Strict Discipline Mode**:\n" +
                    "• When active, you must keep the app in the foreground.\n" +
                    "• If you leave the app, answer calls, or switch tasks, the lifecycle triggers an immediate penalty, saving a failed session record into your Room database.\n" +
                    "• Open the **Strict Focus** tab to begin a session."
        }

        // Generic intelligent synthesis
        return "🤖 **Mastery Local Study Intelligence**\n\n" +
                "I analyzed your query: \"$query\" against your on-device local database.\n\n" +
                "• **Academic Database**: ${notes.size} Notes | ${textbooks.size} Textbooks | ${flashcards.size} Flashcards.\n" +
                "• **Cross-References**: You can link concepts together in notes using `[[Title]]`.\n" +
                "• **Need a specific subject review?** Ask me about *Le Chatelier, Buffer Solutions, Escape Velocity, Kinematics, Flashcard Review*, or search your local notes!"
    }
}
