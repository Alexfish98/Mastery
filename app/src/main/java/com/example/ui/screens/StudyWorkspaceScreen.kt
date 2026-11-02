package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.logic.ChemistryLogic
import com.example.model.Question
import com.example.ui.theme.*
import com.example.util.ExportHelper
import com.example.viewmodel.ActiveStudySession
import com.example.viewmodel.ChemistryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyWorkspaceScreen(
    session: ActiveStudySession,
    viewModel: ChemistryViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val q = viewModel.currentQuestion()
    val a = viewModel.currentAnswer()
    val scrollState = rememberScrollState()

    var showFlagDialog by remember { mutableStateOf(false) }
    var flagNote by remember { mutableStateOf("") }

    if (session.isFinished && session.summary != null) {
        StudySummaryView(session = session, viewModel = viewModel, onExit = onExit)
        return
    }

    if (q == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val n = session.questionIds.size
    val i = session.currentIndex
    val isExam = session.mode == "exam"
    val isFlash = session.mode == "flash"
    val isRecall = session.mode == "recall"
    val isReview = session.mode == "review"
    val isQuiz = session.mode == "quiz"
    val isPractice = session.mode == "practice"

    val options = viewModel.getOptionsForQuestion(q)
    val hasMultipleChoice = (isQuiz || isExam) && options.size >= 2
    val answeredCount = session.answers.values.count { it.done || (isExam && it.ans.isNotBlank()) }

    val bookmarks by viewModel.bookmarks.collectAsState()
    val isBookmarked = bookmarks.contains(q.id)

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = {
                    Column {
                        Text(
                            text = "${session.title} (${i + 1}/$n)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { ((i + 1).toFloat() / n.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, end = 16.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.testTag("exit_study_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Study")
                    }
                },
                actions = {
                    if (session.endTimestamp > 0L) {
                        val remainingSec = maxOf(0, ((session.endTimestamp - System.currentTimeMillis()) / 1000).toInt())
                        val min = remainingSec / 60
                        val sec = remainingSec % 60
                        AssistChip(
                            onClick = {},
                            label = { Text(String.format(Locale.US, "%02d:%02d", min, sec), fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (remainingSec < 30) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleBookmark(q.id) },
                        modifier = Modifier.testTag("study_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.prevQuestion() },
                        enabled = i > 0,
                        modifier = Modifier.testTag("study_prev_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Prev")
                    }

                    Text(
                        text = "$answeredCount of $n answered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (i == n - 1) {
                                viewModel.finishStudySession()
                            } else {
                                viewModel.nextQuestion()
                            }
                        },
                        modifier = Modifier.testTag("study_next_button")
                    ) {
                        Text(if (i == n - 1) (if (isExam) "Finish Exam" else "Finish") else "Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Topic & Difficulty Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(q.topic) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(q.difficulty.replaceFirstChar { it.uppercase() }) }
                )
                if (q.type == "formula") {
                    SuggestionChip(onClick = {}, label = { Text("Formula") })
                } else if (q.type == "number") {
                    SuggestionChip(onClick = {}, label = { Text("Calculation") })
                }
            }

            // Optional Image / Diagram
            if (!q.img.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AsyncImage(
                        model = q.img,
                        contentDescription = "Question Diagram",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Question Prompt / Card
            if (isFlash) {
                // Interactive 3D flip card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .clickable { viewModel.toggleFlip() }
                        .testTag("flashcard_flip_box"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (a.shown) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (a.shown) "ANSWER" else "TAP TO FLIP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (a.shown) ChemistryLogic.exp(q) else q.prompt,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = q.prompt,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
            }

            // Hints display
            if (a.h > 0 && q.hints.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (idx in 0 until a.h.coerceAtMost(q.hints.size)) {
                            Text("💡 Hint ${idx + 1}: ${q.hints[idx]}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Answer Input: Multiple Choice or Text Field
            if (hasMultipleChoice) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEachIndexed { idx, opt ->
                        val isSelected = a.ans == opt
                        val buttonColor = when {
                            a.done && ChemistryLogic.okOf(q, opt) -> SuccessGreenBg
                            a.done && isSelected && !a.ok -> ErrorRedBg
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            a.done && ChemistryLogic.okOf(q, opt) -> SuccessGreen
                            a.done && isSelected && !a.ok -> ErrorRed
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !a.done || isExam) {
                                    viewModel.selectOption(opt)
                                }
                                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                .testTag("option_button_$idx"),
                            color = buttonColor
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        a.done && ChemistryLogic.okOf(q, opt) -> SuccessGreen
                                        a.done && isSelected && !a.ok -> ErrorRed
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (!isFlash && !isRecall && !isReview) {
                // Typed answer input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = a.ans,
                        onValueChange = { viewModel.updateTypedAnswer(it) },
                        label = { Text("Your answer") },
                        enabled = !a.done || isExam,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("study_answer_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Chemical symbols / sub-superscript keyboard toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val syms = listOf("₂", "₃", "₄", "⁺", "⁻", "²", "³", "→", "⇌", "°", "·", "Δ", "↑", "↓")
                        syms.forEach { sym ->
                            AssistChip(
                                onClick = {
                                    if (!a.done || isExam) {
                                        viewModel.updateTypedAnswer(a.ans + sym)
                                    }
                                },
                                label = { Text(sym, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            )
                        }
                    }
                }
            }

            // Confidence rating before checking
            if (!a.done && (isPractice || (isQuiz && !hasMultipleChoice))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confidence:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Pair(1, "Unsure"), Pair(2, "Fairly sure"), Pair(3, "Sure")).forEach { (cVal, label) ->
                            FilterChip(
                                selected = a.c == cVal,
                                onClick = { viewModel.setConfidence(cVal) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.checkCurrentAnswer() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("study_check_button")
                    ) {
                        Text("Check Answer")
                    }
                    OutlinedButton(
                        onClick = { viewModel.revealAnswer() }
                    ) {
                        Text("Show Answer")
                    }
                }
            }

            // Active Recall or Flashcard rating buttons
            if ((isRecall || isFlash) && !a.done) {
                if (a.shown) {
                    Text("Rate your recall:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.rateRecall(0) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Missed it")
                        }
                        Button(
                            onClick = { viewModel.rateRecall(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unsure")
                        }
                        Button(
                            onClick = { viewModel.rateRecall(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sure")
                        }
                    }
                } else if (isRecall) {
                    Button(
                        onClick = { viewModel.toggleFlip() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Answer")
                    }
                }
            }

            // Results and Feedback
            if (a.done && !isExam) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (a.ok) SuccessGreenBg else ErrorRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (a.ok) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (a.ok) SuccessGreen else ErrorRed
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (a.ok) "Correct!" else "Not correct",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (a.ok) SuccessGreen else ErrorRed
                            )
                        }
                        if (!a.ok) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Expected: ${ChemistryLogic.exp(q)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Error Diagnosis for misses
                if (!a.ok && !session.isSprint) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Why did you miss it?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val diagList = listOf(
                                    Pair("concept", "Concept gap"),
                                    Pair("careless", "Careless slip"),
                                    Pair("calc", "Calculation"),
                                    Pair("misread", "Misread"),
                                    Pair("guess", "Guess")
                                )
                                diagList.forEach { (key, label) ->
                                    FilterChip(
                                        selected = a.dx == key,
                                        onClick = { viewModel.recordDiagnosis(key) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Explanation & Tips Card
                if (!a.hx) {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Explanation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            q.explanation?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            q.whyCorrect?.let {
                                Text("Why correct: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            q.concept?.let {
                                Text("Concept: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            q.workedExample?.let {
                                Text("Worked example: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            q.commonMistakes?.let {
                                Text("Common mistakes: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            q.memoryTip?.let {
                                Text("💡 Memory tip: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            q.examTrap?.let {
                                Text("⚠️ Exam trap: $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD97706))
                            }
                        }
                    }
                }
            }

            // Extra Question Actions (Hint, Flag, Image Share, Retry)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isExam && q.hints.isNotEmpty()) {
                    OutlinedButton(onClick = { viewModel.useHint() }) {
                        Text("Hint (${a.h}/${q.hints.size})")
                    }
                }

                OutlinedButton(onClick = {
                    ExportHelper.shareQuestionImage(context, q, includeAnswer = a.done, includeExplanation = a.done)
                }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share Card")
                }

                OutlinedButton(onClick = { showFlagDialog = true }) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Flag")
                }

                if (a.done && !isExam) {
                    OutlinedButton(onClick = { viewModel.retryCurrentQuestion() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }

    if (showFlagDialog) {
        AlertDialog(
            onDismissRequest = { showFlagDialog = false },
            title = { Text("Flag Question") },
            text = {
                Column {
                    Text("Describe any issue or typo in this question:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = flagNote,
                        onValueChange = { flagNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Typo in formula...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.showToast("Question flagged for review.")
                        showFlagDialog = false
                    }
                ) {
                    Text("Submit Flag")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFlagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySummaryView(
    session: ActiveStudySession,
    viewModel: ChemistryViewModel,
    onExit: () -> Unit
) {
    val sum = session.summary ?: return
    val pct = if (sum.total > 0) ((sum.ok.toDouble() / sum.total) * 100).toInt() else 0
    val activeBank by viewModel.activeBank.collectAsState()
    val qMap = remember(activeBank) { activeBank.questions.associateBy { it.id } }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = { Text(if (session.mode == "exam") "Exam Results" else "Session Summary") },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (pct >= 80) SuccessGreenBg else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (pct >= 80) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${sum.ok} of ${sum.total} correct",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Time: ${sum.secs / 60}m ${sum.secs % 60}s · Answered ${sum.answered} of ${sum.total}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (sum.missedIds.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.retryMissedInSession() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Missed (${sum.missedIds.size})")
                    }
                }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Return to Hub")
                }
            }

            Text("Question Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            session.questionIds.forEachIndexed { idx, id ->
                val q = qMap[id]
                val a = session.answers[id]
                if (q != null) {
                    val isOk = a?.ok == true
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isOk) SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${idx + 1}. ${q.prompt}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Your answer: ${a?.ans?.ifBlank { "(unanswered)" } ?: "(unanswered)"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOk) SuccessGreen else ErrorRed
                            )
                            if (!isOk) {
                                Text(
                                    text = "Expected: ${ChemistryLogic.exp(q)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
