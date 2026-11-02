package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.ChemistryViewModel

@Composable
fun DailyPlanScreen(viewModel: ChemistryViewModel) {
    val banks by viewModel.banks.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val srs by viewModel.srs.collectAsState()

    var dailyMinutes by remember { mutableFloatStateOf(prefs.mins.toFloat()) }

    val totalPriority = remember(banks, prefs) {
        maxOf(1, banks.sumOf { prefs.pri[it.id] ?: 3 })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Daily Study Plan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Each question bank represents a subject. Set your available daily study time and subject priorities to compute a balanced study timetable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Study Budget:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${dailyMinutes.toInt()} minutes/day", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = dailyMinutes,
                    onValueChange = {
                        dailyMinutes = it
                        viewModel.updateDailyMinutes(it.toInt())
                    },
                    valueRange = 15f..360f,
                    steps = 22
                )
            }
        }

        banks.forEach { b ->
            val pri = prefs.pri[b.id] ?: 3
            val allocatedMins = ((dailyMinutes * pri) / totalPriority).toInt()
            val now = System.currentTimeMillis()
            val dueCount = b.questions.count { q -> srs[q.id]?.due?.let { it <= now } ?: false }
            val masteredCount = b.questions.count { q -> progress[q.id]?.state == "mastered" }
            val newCount = b.questions.count { q -> (progress[q.id]?.att ?: 0) == 0 }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$allocatedMins min today",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "$dueCount due · $newCount new · $masteredCount of ${b.questions.size} mastered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Priority: $pri of 5", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { pVal ->
                                FilterChip(
                                    selected = pri == pVal,
                                    onClick = { viewModel.updatePriority(b.id, pVal) },
                                    label = { Text("$pVal") }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.selectBank(b.id)
                            val list = viewModel.getFilteredQuestions("smart")
                            viewModel.startStudy("practice", list, "${b.name} · Smart Plan", isOrdered = true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Study ${b.name} Now")
                    }
                }
            }
        }
    }
}
