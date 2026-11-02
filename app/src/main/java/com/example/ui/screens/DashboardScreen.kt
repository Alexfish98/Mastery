package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.ChemistryLogic
import com.example.model.BuilderInfo
import com.example.model.Question
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.ChemistryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ChemistryViewModel) {
    val activeBank by viewModel.activeBank.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    val attempts by viewModel.attempts.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val srs by viewModel.srs.collectAsState()

    val stats = remember(activeBank, attempts, progress, srs) {
        viewModel.calculateStats()
    }

    var selectedSource by remember { mutableStateOf("smart") }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var showBuilderDialog by remember { mutableStateOf(false) }
    var showLayoutCustomizer by remember { mutableStateOf(false) }

    val defaultCards = listOf(
        Pair("mastery_ring", "Mastery Ring & Goals"),
        Pair("quick_study", "Smart Study Launcher"),
        Pair("topic_breakdown", "Topic Mastery Breakdown"),
        Pair("velocity_chart", "14-Day Study Velocity Chart"),
        Pair("achievements", "Badges & Achievements")
    )
    val enabledCards = remember(prefs.dashboardConfigJson) {
        if (prefs.dashboardConfigJson.isNotBlank()) {
            try {
                val arr = org.json.JSONArray(prefs.dashboardConfigJson)
                (0 until arr.length()).map { arr.getString(it) }.toSet()
            } catch (_: Exception) {
                defaultCards.map { it.first }.toSet()
            }
        } else {
            defaultCards.map { it.first }.toSet()
        }
    }

    val sources = remember(activeBank, attempts, progress, srs) {
        listOf(
            Pair("smart", "Smart mix (due, weak, new)"),
            Pair("due", "Due for review (${viewModel.getFilteredQuestions("due").size})"),
            Pair("new", "Not yet attempted (${viewModel.getFilteredQuestions("new").size})"),
            Pair("weak", "Weak topics (${viewModel.getFilteredQuestions("weak").size})"),
            Pair("marked", "Bookmarked (${viewModel.getFilteredQuestions("marked").size})"),
            Pair("wrong", "Incorrect (notebook) (${viewModel.getFilteredQuestions("wrong").size})"),
            Pair("r10", "Random 10 (${minOf(10, activeBank.questions.size)})"),
            Pair("leech", "Leeches (missed 3+ times) (${viewModel.getFilteredQuestions("leech").size})"),
            Pair("all", "All questions (${activeBank.questions.size})")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bank Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome, ${prefs.studentName} 🎓",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${prefs.studentGrade} • Bank: ${activeBank.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = { showLayoutCustomizer = true },
                    modifier = Modifier.testTag("customize_dashboard_button")
                ) {
                    Icon(Icons.Default.DashboardCustomize, contentDescription = "Customize Cards")
                }
                AssistChip(
                    onClick = { showBuilderDialog = true },
                    label = { Text("Builder", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        // Hero Card with Mastery Ring & Gamification
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mastery Ring
                    val targetProgress = if (stats.totalQuestions > 0) stats.mastered.toFloat() / stats.totalQuestions.toFloat() else 0f
                    val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "mastery_ring")

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(96.dp)
                            .padding(4.dp)
                    ) {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.outlineVariant
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "mastered",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (stats.todayCount >= prefs.goal) "Daily goal reached! 🎉" else "${prefs.goal - stats.todayCount} questions to goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Pills
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "🔥 ${stats.streakDays}d streak",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "⭐ Lvl ${stats.level}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "⚡ ${stats.dueForReview} due",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Daily Goal Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Today: ${stats.todayCount}/${prefs.goal}", style = MaterialTheme.typography.labelSmall)
                                Text("XP: ${stats.totalXp}", style = MaterialTheme.typography.labelSmall)
                            }
                            LinearProgressIndicator(
                                progress = { (stats.todayCount.toFloat() / prefs.goal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val list = viewModel.getFilteredQuestions("smart")
                            viewModel.startStudy("practice", list, "Smart Session", isOrdered = true)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_smart_session_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Smart Session")
                    }

                    OutlinedButton(
                        onClick = {
                            val list = viewModel.getFilteredQuestions("due")
                            viewModel.startStudy("flash", list, "Due Flashcards")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Due Cards (${stats.dueForReview})")
                    }
                }
            }
        }

        // Super-App Center Hub Quick Launcher
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "⚡ Super-App Workspaces & Toolkits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val superHubs = listOf(
                    Triple("iqai_chat", "🤖 Mastery Assistant", MaterialTheme.colorScheme.primaryContainer),
                    Triple("iqai_textbook", "📖 Local Textbooks", MaterialTheme.colorScheme.secondaryContainer),
                    Triple("iqai_quiz", "🧠 Active Recall (SM-2)", MaterialTheme.colorScheme.tertiaryContainer),
                    Triple("iqai_notes", "📝 Wiki Notes (FTS)", MaterialTheme.colorScheme.surfaceVariant),
                    Triple("iqai_focus", "🛡️ Strict Focus", MaterialTheme.colorScheme.primaryContainer),
                    Triple("iqai_archive", "📑 Local Archive", MaterialTheme.colorScheme.secondaryContainer),
                    Triple("syllabus", "📚 Syllabus Tracker", MaterialTheme.colorScheme.surfaceVariant),
                    Triple("science_tools", "🔬 Science & Math", MaterialTheme.colorScheme.tertiaryContainer),
                    Triple("utilities", "🧰 ToolNest (50+)", MaterialTheme.colorScheme.surfaceVariant)
                )

                superHubs.forEach { (navId, label, containerColor) ->
                    Card(
                        onClick = { viewModel.navigateTo(navId) },
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 145.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Question Source Selector Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Start Studying",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Dropdown for Source
                ExposedDropdownMenuBox(
                    expanded = sourceDropdownExpanded,
                    onExpandedChange = { sourceDropdownExpanded = !sourceDropdownExpanded }
                ) {
                    val currentLabel = sources.find { it.first == selectedSource }?.second ?: "Select..."
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Questions to study") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = sourceDropdownExpanded,
                        onDismissRequest = { sourceDropdownExpanded = false }
                    ) {
                        sources.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedSource = key
                                    sourceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Modes Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modeButtons = listOf(
                        Pair("practice", "Practice"),
                        Pair("quiz", "Multiple Choice"),
                        Pair("recall", "Active Recall"),
                        Pair("flash", "Flashcards"),
                        Pair("exam", "Mock Exam")
                    )
                    modeButtons.forEach { (mKey, mLabel) ->
                        FilledTonalButton(
                            onClick = {
                                val list = viewModel.getFilteredQuestions(selectedSource)
                                val title = "$mLabel · ${sources.find { it.first == selectedSource }?.second?.substringBefore(" (") ?: ""}"
                                viewModel.startStudy(mKey, list, title)
                            },
                            modifier = Modifier.testTag("mode_button_$mKey")
                        ) {
                            Text(mLabel)
                        }
                    }
                }
            }
        }

        // Stats Matrix
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val statItems = listOf(
                    Pair("${stats.totalQuestions}", "Total Questions"),
                    Pair("${stats.attempted}", "Attempted"),
                    Pair("${stats.correctAttempts}", "Correct Attempts"),
                    Pair("${stats.incorrectAttempts}", "Incorrect Attempts"),
                    Pair(stats.accuracy, "Accuracy"),
                    Pair("${stats.mastered}", "Mastered"),
                    Pair("${stats.learning}", "Learning"),
                    Pair("${stats.openInNotebook}", "In Notebook")
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statItems.forEach { (value, label) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .widthIn(min = 140.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Topic Mastery Breakdown
        val topics = remember(activeBank, progress) {
            activeBank.questions.groupBy { it.topic }.map { (topic, qList) ->
                val masteredCount = qList.count { progress[it.id]?.state == "mastered" }
                val totalAtt = qList.sumOf { progress[it.id]?.att ?: 0 }
                val totalOk = qList.sumOf { progress[it.id]?.ok ?: 0 }
                val acc = if (totalAtt > 0) "${((totalOk.toDouble() / totalAtt) * 100).toInt()}%" else "Not started"
                Triple(topic, qList, Pair(masteredCount, acc))
            }.sortedBy { it.third.first.toFloat() / it.second.size.toFloat() }
        }

        if (topics.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Topic Mastery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    topics.forEach { (topic, qList, info) ->
                        val (masteredCount, acc) = info
                        val pct = if (qList.isNotEmpty()) masteredCount.toFloat() / qList.size.toFloat() else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(topic, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(
                                    "$masteredCount/${qList.size} mastered · $acc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        viewModel.startStudy("practice", qList, "Drill: $topic")
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Drill")
                                }
                            }
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // Activity Last 14 Days Chart
        val fourteenDaysData = remember(attempts) {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayFormat = SimpleDateFormat("d", Locale.US)
            val now = System.currentTimeMillis()
            (0..13).map { i ->
                val dayTime = now - (13 - i) * ChemistryLogic.DAY_MS
                val dateStr = format.format(Date(dayTime))
                val dayLabel = dayFormat.format(Date(dayTime))
                val dayAttempts = attempts.filter { format.format(Date(it.at)) == dateStr }
                val okCount = dayAttempts.count { it.ok == 1 }
                Triple(dayLabel, dayAttempts.size, okCount)
            }
        }

        val maxAttemptsIn14Days = remember(fourteenDaysData) {
            maxOf(1, fourteenDaysData.maxOf { it.second })
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Activity & Accuracy (14 Days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    fourteenDaysData.forEach { (label, count, okCount) ->
                        val barHeightFactor = (count.toFloat() / maxAttemptsIn14Days.toFloat()).coerceIn(0.04f, 1f)
                        val okHeightFactor = if (count > 0) okCount.toFloat() / count.toFloat() else 0f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(barHeightFactor)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(okHeightFactor)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bar height is total questions answered; solid dark fill is correct share.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Achievements / Badges
        val achievements = remember(stats, attempts) {
            listOf(
                Triple("First Steps", "Answer 1 question", stats.totalAttempts >= 1),
                Triple("Warm-up", "Give 50 answers", stats.totalAttempts >= 50),
                Triple("Centurion", "Give 200 answers", stats.totalAttempts >= 200),
                Triple("Week Streak", "Study 7 days in a row", stats.streakDays >= 7),
                Triple("Locked In", "Study 14 days in a row", stats.streakDays >= 14),
                Triple("Mastery 10", "Master 10 questions", stats.mastered >= 10),
                Triple("Mastery 50", "Master 50 questions", stats.mastered >= 50),
                Triple("Precision", "90%+ accuracy over 50+ answers", stats.totalAttempts >= 50 && (stats.correctAttempts.toFloat() / stats.totalAttempts) >= 0.9f)
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Achievements (${achievements.count { it.third }}/${achievements.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    achievements.forEach { (title, desc, isUnlocked) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isUnlocked) "🏅" else "🔒", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBuilderDialog) {
        AlertDialog(
            onDismissRequest = { showBuilderDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡", fontSize = 20.sp)
                    Text("App Builder & Developer")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = BuilderInfo.NAME,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = BuilderInfo.GRADE_ROLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = BuilderInfo.BIO,
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("📞 ${BuilderInfo.PHONES}", style = MaterialTheme.typography.bodySmall)
                    Text("✉️ ${BuilderInfo.EMAIL}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBuilderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showLayoutCustomizer) {
        val currentSet = enabledCards.toMutableSet()
        AlertDialog(
            onDismissRequest = { showLayoutCustomizer = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Customize Dashboard Cards", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Toggle or assign cards to show on your personal main dashboard view:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    defaultCards.forEach { (cardKey, cardTitle) ->
                        var isChecked by remember(cardKey) { mutableStateOf(cardKey in enabledCards) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cardTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        isChecked = checked
                                        if (checked) currentSet.add(cardKey) else currentSet.remove(cardKey)
                                        viewModel.updateDashboardConfig(org.json.JSONArray(currentSet).toString())
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLayoutCustomizer = false }) {
                    Text("Done")
                }
            }
        )
    }
}
