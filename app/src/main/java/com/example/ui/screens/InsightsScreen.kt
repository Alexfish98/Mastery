package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun InsightsScreen(viewModel: ChemistryViewModel) {
    val activeBank by viewModel.activeBank.collectAsState()
    val attempts by viewModel.attempts.collectAsState()
    val exams by viewModel.exams.collectAsState()

    val qMap = remember(activeBank) { activeBank.questions.associateBy { it.id } }

    // 1. Confident but wrong (misconceptions: ok == 0 and c == 3)
    val misconceptions = remember(attempts, activeBank) {
        val wrongSureAttempts = attempts.filter { it.b == activeBank.id && it.ok == 0 && it.c == 3 && qMap.containsKey(it.q) }
        wrongSureAttempts.groupBy { it.q }.map { (qid, attList) ->
            val q = qMap[qid]!!
            val mostCommonWrong = attList.groupBy { it.ans }.maxByOrNull { it.value.size }?.key ?: ""
            Triple(q, attList.size, mostCommonWrong)
        }.sortedByDescending { it.second }
    }

    // 2. Diagnosis distribution
    val diagCounts = remember(attempts) {
        val map = mutableMapOf<String, Int>()
        attempts.forEach { att ->
            att.d?.let { d -> map[d] = (map[d] ?: 0) + 1 }
        }
        map
    }
    val maxDiag = remember(diagCounts) { maxOf(1, diagCounts.values.maxOrNull() ?: 1) }

    // 3. Response speed analytics
    val timedAttempts = remember(attempts) { attempts.filter { it.s > 0 } }
    val medianSpeed = remember(timedAttempts) {
        if (timedAttempts.isEmpty()) 0 else {
            val sorted = timedAttempts.map { it.s }.sorted()
            sorted[sorted.size / 2]
        }
    }

    val slowQuestions = remember(timedAttempts, activeBank, medianSpeed) {
        val threshold = maxOf(20, medianSpeed * 2)
        timedAttempts.filter { it.ok == 1 && it.s > threshold && qMap.containsKey(it.q) }
            .groupBy { it.q }
            .mapNotNull { (qid, atts) ->
                qMap[qid]?.let { q -> Pair(q, atts.maxOf { it.s }) }
            }.sortedByDescending { it.second }
    }

    // 4. Exam topic analytics
    val examMissedQuestions = remember(exams, activeBank) {
        exams.filter { it.bank == activeBank.id }
            .flatMap { it.rows.filter { r -> r.ok == 0 }.map { r -> r.id } }
            .distinct()
            .mapNotNull { qMap[it] }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Learning Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Deep diagnosis of misconceptions, cognitive speed, error patterns, and mock exam retention.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Misconceptions Card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Confident but Wrong (Misconceptions)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Wrong answers you rated 'Sure' indicate misconceptions rather than simple gaps in knowledge. Correcting these gives the highest exam score improvement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (misconceptions.isEmpty()) {
                    Text("No misconceptions detected yet! Rate your confidence while answering to populate this diagnostic.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    misconceptions.take(6).forEach { (q, count, wrongAns) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(q.prompt, fontWeight = FontWeight.SemiBold)
                                Text("Missed $count× while confident · Common mistake: '$wrongAns'", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                                Text("Correct: ${ChemistryLogic.exp(q)}", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val list = misconceptions.map { it.first }
                            viewModel.startStudy("practice", list, "Misconception Drill")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Drill Misconceptions (${misconceptions.size})")
                    }
                }
            }
        }

        // Why You Miss Questions
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Root Cause Diagnosis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Categorization of incorrect answers selected in study sessions:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                val diagLabels = listOf(
                    Pair("concept", "Concept Gap"),
                    Pair("careless", "Careless Slip"),
                    Pair("calc", "Calculation"),
                    Pair("misread", "Misread"),
                    Pair("guess", "Guess")
                )

                diagLabels.forEach { (key, label) ->
                    val count = diagCounts[key] ?: 0
                    val factor = count.toFloat() / maxDiag.toFloat()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("$count", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { factor },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }
        }

        // Response Speed
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Response Speed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (medianSpeed > 0) "Median time per answer: $medianSpeed seconds over ${timedAttempts.size} timed answers." else "Speed telemetry begins with your first answered question.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (slowQuestions.isNotEmpty()) {
                    Text("Correct but slow (> 2× your median):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    slowQuestions.take(5).forEach { (q, secs) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(q.prompt.take(45) + "…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("${secs}s", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.startStudy("practice", slowQuestions.map { it.first }, "Speed Drill")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Drill Slow Questions (${slowQuestions.size})")
                    }
                }
            }
        }

        // Mock Exam Remedial
        if (exams.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Mock Exam Remedial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("You have completed ${exams.size} mock exams.", style = MaterialTheme.typography.bodyMedium)

                    if (examMissedQuestions.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.startStudy("practice", examMissedQuestions, "Exam Remedial Practice")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Practise Missed in Exams (${examMissedQuestions.size})")
                        }
                    }
                }
            }
        }
    }
}
