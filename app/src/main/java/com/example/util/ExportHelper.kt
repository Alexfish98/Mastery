package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.logic.ChemistryLogic
import com.example.model.Question
import com.example.model.QuestionProgress
import com.example.model.SrsSchedule
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private fun escapeCsv(v: String?): String {
        var str = v ?: ""
        if (str.startsWith("=") || str.startsWith("+") || str.startsWith("@") || str.startsWith("-")) {
            str = "'$str"
        }
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    fun buildCsv(
        questions: List<Question>,
        progressMap: Map<String, QuestionProgress>,
        srsMap: Map<String, SrsSchedule>
    ): String {
        val rows = mutableListOf<List<String>>()
        rows.add(
            listOf(
                "id", "topic", "difficulty", "question", "answers", "explanation", "tags",
                "attempts", "correct", "incorrect", "status", "last_answer", "last_attempt", "confidence", "next_due"
            )
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        questions.forEach { q ->
            val p = progressMap[q.id]
            val r = srsMap[q.id]
            val status = when {
                p == null || p.att == 0 -> "New"
                p.state == "mastered" -> "Mastered"
                r != null && r.due <= System.currentTimeMillis() -> "Due"
                else -> "Learning"
            }
            val confLabel = when (p?.conf) {
                1 -> "Unsure"
                2 -> "Fairly sure"
                3 -> "Sure"
                else -> ""
            }
            val lastAttStr = if (p != null && p.at > 0) dateFormat.format(Date(p.at)) else ""
            val nextDueStr = if (r != null && r.due > 0) dateFormat.format(Date(r.due)) else ""

            rows.add(
                listOf(
                    q.id,
                    q.topic,
                    q.difficulty,
                    q.prompt,
                    q.answers.joinToString(" | "),
                    q.explanation ?: "",
                    q.tags.joinToString("; "),
                    (p?.att ?: 0).toString(),
                    (p?.ok ?: 0).toString(),
                    (p?.bad ?: 0).toString(),
                    status,
                    p?.ans ?: "",
                    lastAttStr,
                    confLabel,
                    nextDueStr
                )
            )
        }

        return "\uFEFF" + rows.joinToString("\r\n") { row ->
            row.joinToString(",") { escapeCsv(it) }
        }
    }

    fun buildRevisionSheet(questions: List<Question>): String {
        return questions.mapIndexed { idx, q ->
            val num = idx + 1
            val builder = StringBuilder()
            builder.append("$num. ${q.prompt} (${q.topic})\n")
            builder.append("   Answer: ${ChemistryLogic.exp(q)}\n")
            q.explanation?.let { builder.append("   Why: $it\n") }
            q.memoryTip?.let { builder.append("   Tip: $it\n") }
            q.examTrap?.let { builder.append("   Exam trap: $it\n") }
            builder.toString().trimEnd()
        }.joinToString("\n\n")
    }

    fun shareText(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun renderQuestionBitmap(
        question: Question,
        includeAnswer: Boolean = true,
        includeExplanation: Boolean = true
    ): Bitmap {
        val width = 1000
        val pad = 56
        val maxContentWidth = width - (pad * 2)

        data class TextBlock(val text: String, val fontSize: Float, val color: Int, val isBold: Boolean, val lineSpacing: Float)

        val blocks = mutableListOf<TextBlock>()
        blocks.add(
            TextBlock(
                text = "${question.topic} · ${question.difficulty.uppercase()}",
                fontSize = 24f,
                color = android.graphics.Color.parseColor("#475569"),
                isBold = true,
                lineSpacing = 30f
            )
        )
        blocks.add(
            TextBlock(
                text = question.prompt,
                fontSize = 34f,
                color = android.graphics.Color.parseColor("#0F172A"),
                isBold = true,
                lineSpacing = 44f
            )
        )

        if (includeAnswer) {
            blocks.add(
                TextBlock(
                    text = "Answer: " + ChemistryLogic.exp(question),
                    fontSize = 28f,
                    color = android.graphics.Color.parseColor("#166534"),
                    isBold = true,
                    lineSpacing = 38f
                )
            )
        }

        if (includeExplanation) {
            question.explanation?.let {
                blocks.add(
                    TextBlock(
                        text = "Why: $it",
                        fontSize = 26f,
                        color = android.graphics.Color.parseColor("#1E293B"),
                        isBold = false,
                        lineSpacing = 36f
                    )
                )
            }
            question.memoryTip?.let {
                blocks.add(
                    TextBlock(
                        text = "Tip: $it",
                        fontSize = 24f,
                        color = android.graphics.Color.parseColor("#334155"),
                        isBold = false,
                        lineSpacing = 34f
                    )
                )
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Word-wrap function
        fun wrapText(text: String, fontSize: Float, isBold: Boolean): List<String> {
            paint.textSize = fontSize
            paint.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val lines = mutableListOf<String>()

            for (paragraph in text.split("\n")) {
                var currentLine = ""
                for (word in paragraph.split("\\s+".toRegex())) {
                    val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(candidate) <= maxContentWidth) {
                        currentLine = candidate
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine)
            }
            return lines
        }

        val wrappedBlocks = blocks.map { b -> Pair(b, wrapText(b.text, b.fontSize, b.isBold)) }

        var totalHeight = pad * 2 + 60
        wrappedBlocks.forEach { (b, lines) ->
            totalHeight += (lines.size * b.lineSpacing + 22).toInt()
        }
        totalHeight = totalHeight.coerceIn(400, 5000)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(android.graphics.Color.WHITE)

        // Draw top accent bar (Indigo)
        paint.color = android.graphics.Color.parseColor("#4F46E5")
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, paint)

        var y = (pad + 20).toFloat()
        wrappedBlocks.forEach { (b, lines) ->
            paint.textSize = b.fontSize
            paint.color = b.color
            paint.typeface = if (b.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

            lines.forEach { line ->
                canvas.drawText(line, pad.toFloat(), y, paint)
                y += b.lineSpacing
            }
            y += 22f
        }

        // Draw footer
        paint.textSize = 20f
        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("EUEE Mastery", pad.toFloat(), (totalHeight - pad + 10).toFloat(), paint)

        return bitmap
    }

    fun shareQuestionImage(context: Context, question: Question, includeAnswer: Boolean = true, includeExplanation: Boolean = true) {
        try {
            val bmp = renderQuestionBitmap(question, includeAnswer, includeExplanation)
            val cacheDir = File(context.cacheDir, "shared_images")
            cacheDir.mkdirs()
            val file = File(cacheDir, "question_${question.id}.png")
            val fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Question: ${question.prompt}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Question Card Image"))
        } catch (_: Exception) {
            // Fallback to text share
            shareText(context, "Question: ${question.prompt}", "${question.prompt}\n\nAnswer: ${ChemistryLogic.exp(question)}")
        }
    }

    fun sharePdfFile(context: Context, title: String, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
    }

    fun generateStudySheetPdf(context: Context, bankName: String, questions: List<Question>): File {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageW = 595
        val pageH = 842
        val margin = 40
        val contentW = pageW - margin * 2

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNum = 1

        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(cnv: Canvas, pNum: Int) {
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 16f
            paint.color = android.graphics.Color.parseColor("#315FE8")
            cnv.drawText("EUEE Mastery · Study Sheet", margin.toFloat(), 45f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = android.graphics.Color.DKGRAY
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
            cnv.drawText("$bankName · $dateStr · ${questions.size} questions", margin.toFloat(), 62f, paint)

            paint.color = android.graphics.Color.LTGRAY
            paint.strokeWidth = 1f
            cnv.drawLine(margin.toFloat(), 70f, (pageW - margin).toFloat(), 70f, paint)

            paint.textSize = 9f
            paint.color = android.graphics.Color.GRAY
            cnv.drawText("Page $pNum", (pageW - margin - 35).toFloat(), (pageH - 25).toFloat(), paint)
        }

        drawHeader(canvas, pageNum)
        var y = 88f

        fun wrap(text: String, size: Float, isBold: Boolean): List<String> {
            paint.textSize = size
            paint.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val lines = mutableListOf<String>()
            for (p in text.split("\n")) {
                var curLine = ""
                for (w in p.split("\\s+".toRegex())) {
                    val cand = if (curLine.isEmpty()) w else "$curLine $w"
                    if (paint.measureText(cand) <= contentW) {
                        curLine = cand
                    } else {
                        if (curLine.isNotEmpty()) lines.add(curLine)
                        curLine = w
                    }
                }
                if (curLine.isNotEmpty()) lines.add(curLine)
            }
            return lines
        }

        questions.forEachIndexed { idx, q ->
            val num = idx + 1
            val promptLines = wrap("$num. ${q.prompt}", 11f, true)
            val ansLines = wrap("Answer: ${ChemistryLogic.exp(q)}", 10.5f, true)
            val expLines = if (!q.explanation.isNullOrBlank()) wrap("Why: ${q.explanation}", 10f, false) else emptyList()
            val tipLines = if (!q.memoryTip.isNullOrBlank()) wrap("Tip: ${q.memoryTip}", 9.5f, false) else emptyList()

            val itemHeight = (promptLines.size * 14) + (ansLines.size * 14) + (expLines.size * 13) + (tipLines.size * 12) + 26

            if (y + itemHeight > (pageH - 45)) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas, pageNum)
                y = 88f
            }

            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = android.graphics.Color.parseColor("#0F172A")
            promptLines.forEach { line ->
                canvas.drawText(line, margin.toFloat(), y, paint)
                y += 14f
            }

            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = android.graphics.Color.parseColor("#64748B")
            canvas.drawText("[${q.topic} · ${q.difficulty}]", margin.toFloat(), y, paint)
            y += 13f

            paint.textSize = 10.5f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = android.graphics.Color.parseColor("#166534")
            ansLines.forEach { line ->
                canvas.drawText(line, margin.toFloat(), y, paint)
                y += 14f
            }

            if (expLines.isNotEmpty()) {
                paint.textSize = 10f
                paint.typeface = Typeface.DEFAULT
                paint.color = android.graphics.Color.parseColor("#334155")
                expLines.forEach { line ->
                    canvas.drawText(line, margin.toFloat(), y, paint)
                    y += 13f
                }
            }

            if (tipLines.isNotEmpty()) {
                paint.textSize = 9.5f
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.color = android.graphics.Color.parseColor("#854D0E")
                tipLines.forEach { line ->
                    canvas.drawText(line, margin.toFloat(), y, paint)
                    y += 12f
                }
            }

            y += 12f
        }

        pdfDocument.finishPage(page)

        val outDir = File(context.cacheDir, "exported_pdfs")
        outDir.mkdirs()
        val file = File(outDir, "study_sheet_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return file
    }

    fun generateIncorrectQuestionsReportPdf(
        context: Context,
        bankName: String,
        questions: List<Question>,
        progressMap: Map<String, QuestionProgress>
    ): File {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageW = 595
        val pageH = 842
        val margin = 40
        val contentW = pageW - margin * 2

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNum = 1

        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader(cnv: Canvas, pNum: Int) {
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 16f
            paint.color = android.graphics.Color.parseColor("#B91C1C")
            cnv.drawText("EUEE Mastery · Incorrect-Question Report", margin.toFloat(), 45f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = android.graphics.Color.DKGRAY
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
            cnv.drawText("$bankName · $dateStr · ${questions.size} review questions", margin.toFloat(), 62f, paint)

            paint.color = android.graphics.Color.LTGRAY
            paint.strokeWidth = 1f
            cnv.drawLine(margin.toFloat(), 70f, (pageW - margin).toFloat(), 70f, paint)

            paint.textSize = 9f
            paint.color = android.graphics.Color.GRAY
            cnv.drawText("Page $pNum", (pageW - margin - 35).toFloat(), (pageH - 25).toFloat(), paint)
        }

        drawHeader(canvas, pageNum)
        var y = 88f

        fun wrap(text: String, size: Float, isBold: Boolean): List<String> {
            paint.textSize = size
            paint.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val lines = mutableListOf<String>()
            for (p in text.split("\n")) {
                var curLine = ""
                for (w in p.split("\\s+".toRegex())) {
                    val cand = if (curLine.isEmpty()) w else "$curLine $w"
                    if (paint.measureText(cand) <= contentW) {
                        curLine = cand
                    } else {
                        if (curLine.isNotEmpty()) lines.add(curLine)
                        curLine = w
                    }
                }
                if (curLine.isNotEmpty()) lines.add(curLine)
            }
            return lines
        }

        questions.forEachIndexed { idx, q ->
            val p = progressMap[q.id]
            val num = idx + 1
            val promptLines = wrap("$num. ${q.prompt}", 11f, true)
            val lastAns = p?.ans?.ifBlank { "(none)" } ?: "(none)"
            val ansLines = wrap("Your last answer: $lastAns · Missed ${p?.bad ?: 0} of ${p?.att ?: 0} attempts", 10f, false)
            val expectedLines = wrap("Expected answer: ${ChemistryLogic.exp(q)}", 10.5f, true)
            val expLines = if (!q.explanation.isNullOrBlank()) wrap("Why: ${q.explanation}", 10f, false) else emptyList()

            val itemHeight = (promptLines.size * 14) + (ansLines.size * 13) + (expectedLines.size * 14) + (expLines.size * 13) + 26

            if (y + itemHeight > (pageH - 45)) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas, pageNum)
                y = 88f
            }

            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = android.graphics.Color.parseColor("#0F172A")
            promptLines.forEach { line ->
                canvas.drawText(line, margin.toFloat(), y, paint)
                y += 14f
            }

            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            paint.color = android.graphics.Color.parseColor("#B91C1C")
            ansLines.forEach { line ->
                canvas.drawText(line, margin.toFloat(), y, paint)
                y += 13f
            }

            paint.textSize = 10.5f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = android.graphics.Color.parseColor("#166534")
            expectedLines.forEach { line ->
                canvas.drawText(line, margin.toFloat(), y, paint)
                y += 14f
            }

            if (expLines.isNotEmpty()) {
                paint.textSize = 10f
                paint.typeface = Typeface.DEFAULT
                paint.color = android.graphics.Color.parseColor("#334155")
                expLines.forEach { line ->
                    canvas.drawText(line, margin.toFloat(), y, paint)
                    y += 13f
                }
            }

            y += 14f
        }

        pdfDocument.finishPage(page)

        val outDir = File(context.cacheDir, "exported_pdfs")
        outDir.mkdirs()
        val file = File(outDir, "mistakes_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return file
    }
}
