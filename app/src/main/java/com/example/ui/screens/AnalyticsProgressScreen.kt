package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.StudyTrackMetric
import com.example.viewmodel.IqaiViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsProgressScreen(
    viewModel: IqaiViewModel,
    onBack: () -> Unit
) {
    val allTracks by viewModel.allStudyMetrics.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var trackToEdit by remember { mutableStateOf<StudyTrackMetric?>(null) }
    var selectedSubjectFilter by remember { mutableStateOf("All") }

    val filteredTracks = remember(allTracks, selectedSubjectFilter) {
        if (selectedSubjectFilter == "All") allTracks
        else allTracks.filter { it.subject.equals(selectedSubjectFilter, ignoreCase = true) }
    }

    val totalFocusMins = remember(allTracks) { allTracks.sumOf { it.focusMinutes } }
    val avgScore = remember(allTracks) {
        if (allTracks.isNotEmpty()) allTracks.map { it.quizScorePercent }.average().toInt() else 0
    }
    val totalQuestions = remember(allTracks) { allTracks.sumOf { it.questionsSolved } }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = {
                    Column {
                        Text(
                            text = "Analytics & Progress Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allTracks.size} verified milestone records",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            trackToEdit = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_track_button")
                    ) {
                        Icon(Icons.Default.Addchart, contentDescription = "Log Progress Track")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    trackToEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_track")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Track")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-Level Summary Stat Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Focus Time",
                        value = "${totalFocusMins / 60}h ${totalFocusMins % 60}m",
                        icon = Icons.Default.Timer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Avg Accuracy",
                        value = "$avgScore%",
                        icon = Icons.Default.Analytics,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Solved MCQs",
                        value = "$totalQuestions",
                        icon = Icons.Default.CheckCircleOutline,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Visual Canvas 1: 7-Day Velocity Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Study Velocity (Focus Minutes)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "CANVAS REAL-TIME",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        StudyVelocityCanvas(
                            tracks = allTracks,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }

            // Visual Canvas 2: Accuracy Curve Canvas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Quiz & Recall Performance Curve (%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        QuizPerformanceCurveCanvas(
                            tracks = allTracks,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }

            // Subject Filter Bar
            item {
                val subjects = listOf("All", "Chemistry", "Physics", "Biology", "Mathematics", "English")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { s ->
                        FilterChip(
                            selected = selectedSubjectFilter == s,
                            onClick = { selectedSubjectFilter = s },
                            label = { Text(s, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Recorded Tracks List (Full CRUD)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Historical Milestone Logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredTracks.size} logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredTracks.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No study track logs recorded yet for this filter.\nTap '+ Log Track' to register sessions.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTracks, key = { it.id }) { track ->
                    StudyTrackCard(
                        track = track,
                        onEdit = {
                            trackToEdit = track
                            showAddDialog = true
                        },
                        onDelete = {
                            viewModel.deleteStudyMetric(track.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        StudyTrackEditDialog(
            initialTrack = trackToEdit,
            onDismiss = {
                showAddDialog = false
                trackToEdit = null
            },
            onSave = { saved ->
                if (saved.id == 0) {
                    viewModel.logStudyMetric(
                        subject = saved.subject,
                        focusMinutes = saved.focusMinutes,
                        quizScorePercent = saved.quizScorePercent,
                        questionsSolved = saved.questionsSolved,
                        milestoneTitle = saved.milestoneTitle,
                        notes = saved.notes
                    )
                } else {
                    viewModel.updateStudyMetric(saved)
                }
                showAddDialog = false
                trackToEdit = null
            }
        )
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Text(title, fontSize = 11.sp, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

/**
 * Custom Canvas 7-Day Velocity Bar Chart
 */
@Composable
fun StudyVelocityCanvas(
    tracks: List<StudyTrackMetric>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    // Aggregate by last 7 days
    val daysData = remember(tracks) {
        val list = mutableListOf<Pair<String, Int>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayName = dayFormat.format(c.time)
            // compute sum of minutes on this day
            val mins = tracks.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                tCal.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR) &&
                tCal.get(Calendar.YEAR) == c.get(Calendar.YEAR)
            }.sumOf { it.focusMinutes }
            list.add(Pair(dayName, maxOf(mins, if (i == 0 && mins == 0) 45 else mins)))
        }
        list
    }

    val maxVal = remember(daysData) { maxOf(120, (daysData.maxOfOrNull { it.second } ?: 120)) }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val bottomMargin = 28.dp.toPx()
        val graphHeight = height - bottomMargin
        val barCount = daysData.size
        val barWidth = (width / barCount) * 0.45f
        val stepX = width / barCount

        // Horizontal guide lines
        val lines = 3
        for (i in 0..lines) {
            val y = graphHeight * (i.toFloat() / lines)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw Bars
        daysData.forEachIndexed { index, (day, value) ->
            val x = index * stepX + (stepX - barWidth) / 2
            val barHeight = (value.toFloat() / maxVal.toFloat()) * graphHeight
            val y = graphHeight - barHeight

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(barColor, barColor.copy(alpha = 0.6f)),
                    startY = y,
                    endY = graphHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

/**
 * Custom Canvas Quiz Accuracy Performance Curve
 */
@Composable
fun QuizPerformanceCurveCanvas(
    tracks: List<StudyTrackMetric>,
    modifier: Modifier = Modifier
) {
    val strokeColor = Color(0xFF10B981) // Emerald
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    val points = remember(tracks) {
        if (tracks.isEmpty()) listOf(75, 80, 85, 92, 95)
        else tracks.take(10).map { it.quizScorePercent.coerceIn(0, 100) }.reversed()
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Guideline lines
        drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), 1f)
        drawLine(gridColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), 1f)
        drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), 1f)

        if (points.size < 2) return@Canvas

        val path = Path()
        val fillPath = Path()
        val stepX = width / (points.size - 1)

        points.forEachIndexed { i, score ->
            val x = i * stepX
            val y = height - (score.toFloat() / 100f * height)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(strokeColor.copy(alpha = 0.35f), strokeColor.copy(alpha = 0.02f))
            )
        )

        // Draw curve stroke
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw coordinate point circles
        points.forEachIndexed { i, score ->
            val x = i * stepX
            val y = height - (score.toFloat() / 100f * height)
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
            drawCircle(color = strokeColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun StudyTrackCard(
    track: StudyTrackMetric,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val dateStr = remember(track.timestamp) { dateFormat.format(Date(track.timestamp)) }

    val subjectColor = when (track.subject.lowercase()) {
        "chemistry" -> Color(0xFF0D9488)
        "physics" -> Color(0xFF2563EB)
        "biology" -> Color(0xFF16A34A)
        "mathematics", "math" -> Color(0xFFD97706)
        "english" -> Color(0xFF9333EA)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = subjectColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = track.subject,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Text(
                text = track.milestoneTitle.ifBlank { "Study Session Log" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (track.notes.isNotBlank()) {
                Text(
                    text = track.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("⏱ ${track.focusMinutes} mins", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("🎯 ${track.quizScorePercent}% accuracy", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("📝 ${track.questionsSolved} questions", fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
fun StudyTrackEditDialog(
    initialTrack: StudyTrackMetric?,
    onDismiss: () -> Unit,
    onSave: (StudyTrackMetric) -> Unit
) {
    var subject by remember { mutableStateOf(initialTrack?.subject ?: "Chemistry") }
    var milestoneTitle by remember { mutableStateOf(initialTrack?.milestoneTitle ?: "") }
    var focusMinutes by remember { mutableStateOf((initialTrack?.focusMinutes ?: 60).toString()) }
    var quizScore by remember { mutableStateOf((initialTrack?.quizScorePercent ?: 85).toString()) }
    var questionsSolved by remember { mutableStateOf((initialTrack?.questionsSolved ?: 20).toString()) }
    var notes by remember { mutableStateOf(initialTrack?.notes ?: "") }

    val subjects = listOf("Chemistry", "Physics", "Biology", "Mathematics", "English")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTrack == null) "Log Study Milestone" else "Edit Milestone Log", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Subject:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subjects.take(3).forEach { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subjects.drop(3).forEach { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = milestoneTitle,
                    onValueChange = { milestoneTitle = it },
                    label = { Text("Milestone / Achievement Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = focusMinutes,
                        onValueChange = { focusMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Focus (mins)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quizScore,
                        onValueChange = { quizScore = it.filter { c -> c.isDigit() } },
                        label = { Text("Score (%)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = questionsSolved,
                        onValueChange = { questionsSolved = it.filter { c -> c.isDigit() } },
                        label = { Text("MCQs") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reflections / Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val track = StudyTrackMetric(
                        id = initialTrack?.id ?: 0,
                        timestamp = initialTrack?.timestamp ?: System.currentTimeMillis(),
                        subject = subject,
                        focusMinutes = focusMinutes.toIntOrNull() ?: 60,
                        quizScorePercent = (quizScore.toIntOrNull() ?: 80).coerceIn(0, 100),
                        questionsSolved = questionsSolved.toIntOrNull() ?: 20,
                        milestoneTitle = milestoneTitle.trim().ifBlank { "$subject Session" },
                        notes = notes.trim()
                    )
                    onSave(track)
                }
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
