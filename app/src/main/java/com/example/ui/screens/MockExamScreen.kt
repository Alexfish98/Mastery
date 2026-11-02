package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ChemistryViewModel

@Composable
fun MockExamScreen(viewModel: ChemistryViewModel) {
    val activeBank by viewModel.activeBank.collectAsState()
    val allQuestions = activeBank.questions
    val topics = remember(activeBank) { allQuestions.map { it.topic }.distinct() }

    var questionCount by remember { mutableFloatStateOf(minOf(20, allQuestions.size).toFloat()) }
    var examMinutes by remember { mutableFloatStateOf(minOf(20, allQuestions.size).toFloat()) }

    var easyRatio by remember { mutableFloatStateOf(25f) }
    var mediumRatio by remember { mutableFloatStateOf(50f) }
    var hardRatio by remember { mutableFloatStateOf(25f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mock Exam & Rapid Sprint", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Simulate authentic exam conditions with strict time limits, randomized blueprints, and full diagnostic feedback.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Mock Exam Blueprint
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Exam Blueprint & EUEE Presets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                // Authentic EUEE National Exam Presets
                Text("Authentic EUEE Standard Specifications:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                val eueePresets = listOf(
                    Triple("Chemistry", 80, 120),
                    Triple("Physics", 60, 120),
                    Triple("Mathematics", 65, 180),
                    Triple("English", 120, 120),
                    Triple("Biology", 80, 120)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    eueePresets.forEach { (sub, q, m) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    questionCount = minOf(q, allQuestions.size).toFloat().coerceAtLeast(1f)
                                    examMinutes = m.toFloat()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(sub.take(4), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${m}m", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Question Count Slider
                val maxQ = maxOf(1, allQuestions.size)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Number of Questions:")
                        Text("${questionCount.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = questionCount,
                        onValueChange = { questionCount = it },
                        valueRange = 1f..maxQ.toFloat(),
                        steps = if (maxQ > 1) maxQ - 2 else 0
                    )
                }

                // Minutes Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Time Limit:")
                        Text("${examMinutes.toInt()} minutes", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = examMinutes,
                        onValueChange = { examMinutes = it },
                        valueRange = 5f..120f
                    )
                }

                // Difficulty Distribution
                Text("Difficulty Mix:", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("Easy", easyRatio) { v: Float -> easyRatio = v },
                        Triple("Med", mediumRatio) { v: Float -> mediumRatio = v },
                        Triple("Hard", hardRatio) { v: Float -> hardRatio = v }
                    ).forEach { (label, ratio, setter) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                                Text("${ratio.toInt()}%", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val count = questionCount.toInt().coerceIn(1, allQuestions.size)
                        val examQ = allQuestions.shuffled().take(count)
                        viewModel.startStudy("exam", examQ, "Mock Exam · ${examMinutes.toInt()}m", secondsPerQ = ((examMinutes * 60) / count).toInt())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_mock_exam_button")
                ) {
                    Text("Start Mock Exam (${questionCount.toInt()} Questions)")
                }
            }
        }

        // 60-Second Sprint
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("60-Second Sprint", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Text(
                    "High-speed multiple choice drill with automatic advancement. Answer as many questions as you can in 60 seconds to build lightning recall.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        val sprintQuestions = allQuestions.shuffled().take(100)
                        viewModel.startStudy("sprint", sprintQuestions, "Sprint · 60s", isOrdered = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start 60-Second Sprint")
                }
            }
        }
    }
}
