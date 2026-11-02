package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logic.ChemistryLogic
import com.example.model.Question
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.ChemistryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotebookScreen(viewModel: ChemistryViewModel) {
    val activeBank by viewModel.activeBank.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val attempts by viewModel.attempts.collectAsState()

    var statusFilter by remember { mutableStateOf("open") } // "open", "mastered", "all"
    var selectedTopic by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("") }
    var selectedAgeDays by remember { mutableStateOf<Int?>(null) } // 7, 30, null
    var sortBy by remember { mutableStateOf("missed") } // "missed", "stale", "recent"

    var showClearDialog by remember { mutableStateOf(false) }

    val topics = remember(activeBank) {
        listOf("") + activeBank.questions.map { it.topic }.distinct().sorted()
    }

    val notebookQuestions = remember(activeBank, progress, statusFilter, selectedTopic, selectedDifficulty, selectedAgeDays, sortBy) {
        val now = System.currentTimeMillis()
        activeBank.questions.filter { q ->
            val p = progress[q.id]
            if (p == null || !p.nb) return@filter false
            val matchesStatus = when (statusFilter) {
                "open" -> p.state != "mastered"
                "mastered" -> p.state == "mastered"
                else -> true
            }
            val matchesTopic = selectedTopic.isEmpty() || q.topic == selectedTopic
            val matchesDiff = selectedDifficulty.isEmpty() || q.difficulty == selectedDifficulty
            val matchesAge = selectedAgeDays == null || (now - p.at <= selectedAgeDays!! * ChemistryLogic.DAY_MS)
            matchesStatus && matchesTopic && matchesDiff && matchesAge
        }.sortedWith { a, b ->
            val pa = progress[a.id]
            val pb = progress[b.id]
            when (sortBy) {
                "missed" -> (pb?.bad ?: 0).compareTo(pa?.bad ?: 0)
                "stale" -> {
                    val ta = if ((pa?.rev ?: 0L) > 0L) pa!!.rev else (pa?.at ?: 0L)
                    val tb = if ((pb?.rev ?: 0L) > 0L) pb!!.rev else (pb?.at ?: 0L)
                    ta.compareTo(tb)
                }
                else -> (pb?.at ?: 0L).compareTo(pa?.at ?: 0L)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Incorrect-Answer Notebook",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Every question you miss is preserved here with its full attempt history and error causes. Answering correctly later marks it as mastered without erasing history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Toolbar
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.startStudy("review", notebookQuestions, "Notebook Review", isOrdered = true)
                    },
                    enabled = notebookQuestions.isNotEmpty()
                ) {
                    Text("Review All (${notebookQuestions.size})")
                }

                FilledTonalButton(
                    onClick = {
                        val wrongList = viewModel.getFilteredQuestions("wrong")
                        viewModel.startStudy("practice", wrongList, "Retry Incorrect")
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Retry Incorrect")
                }

                OutlinedButton(
                    onClick = { showClearDialog = true }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear Notebook")
                }
            }
        }

        // Filters Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Pair("open", "Needs Review"), Pair("mastered", "Mastered"), Pair("all", "All")).forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { statusFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Sort & Age Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Pair("missed", "Most Missed"), Pair("recent", "Most Recent"), Pair("stale", "Least Reviewed")).forEach { (key, label) ->
                            FilterChip(
                                selected = sortBy == key,
                                onClick = { sortBy = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Question items
        if (notebookQuestions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Notebook is clean! 🌟",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Any question you answer incorrectly during practice, flashcards, or exams will automatically appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(notebookQuestions, key = { it.id }) { q ->
                NotebookCard(
                    question = q,
                    viewModel = viewModel,
                    attempts = attempts.filter { it.q == q.id }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Notebook") },
            text = { Text("Clear all entries from the notebook? Attempt history and stats are kept. Questions will return if missed again.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearNotebook()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NotebookCard(
    question: Question,
    viewModel: ChemistryViewModel,
    attempts: List<com.example.model.AttemptRecord>
) {
    val progress by viewModel.progress.collectAsState()
    val p = progress[question.id]
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(question.topic) })
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (p?.state == "mastered") "Mastered" else "Needs Review") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (p?.state == "mastered") SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Your last answer: ${p?.ans?.ifBlank { "—" } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed
            )
            Text(
                text = "Expected: ${ChemistryLogic.exp(question)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SuccessGreen
            )

            Text(
                text = "Attempts: ${p?.att ?: 0} · Missed: ${p?.bad ?: 0} · Last: ${if ((p?.at ?: 0L) > 0) dateFormat.format(Date(p!!.at)) else "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // History & Explanation expander
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (expanded) "Hide History & Explanation" else "Show Attempt History & Explanation",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (attempts.isNotEmpty()) {
                        Text("Recent Attempts:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        attempts.takeLast(6).reversed().forEach { att ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dateFormat.format(Date(att.at)), style = MaterialTheme.typography.labelSmall)
                                Text(if (att.ok == 1) "✓ Correct" else "✗ Missed: ${att.ans}", style = MaterialTheme.typography.labelSmall, color = if (att.ok == 1) SuccessGreen else ErrorRed)
                                att.d?.let { Text("($it)", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }

                    question.explanation?.let {
                        Text("Explanation: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    question.memoryTip?.let {
                        Text("Tip: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.removeFromNotebook(question.id) }) {
                    Text("Remove")
                }
                TextButton(onClick = { viewModel.toggleMastered(question.id) }) {
                    Text(if (p?.state == "mastered") "Reopen" else "Mark Mastered")
                }
                Button(
                    onClick = {
                        viewModel.startStudy("practice", listOf(question), "Retry Question")
                    }
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
