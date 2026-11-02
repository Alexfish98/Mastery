package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.viewmodel.ChemistryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SyllabusTrackerScreen(
    viewModel: ChemistryViewModel
) {
    val scope = rememberCoroutineScope()
    val syllabusManager = remember { SyllabusManager(viewModel.getApplication()) }
    val statusMap by syllabusManager.statusMap.collectAsState()

    LaunchedEffect(Unit) {
        syllabusManager.load()
    }

    var selectedSubject by remember { mutableStateOf("All") }
    var selectedGrade by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val subjects = listOf("All", "Mathematics", "Physics", "Chemistry", "Biology", "English", "General Aptitude")
    val overallProgress = remember(statusMap) { syllabusManager.calculateOverallProgress() }

    val filteredCurriculum = remember(selectedSubject, selectedGrade, searchQuery, statusMap) {
        SyllabusData.curriculum.filter { syllabus ->
            (selectedSubject == "All" || syllabus.subject.equals(selectedSubject, ignoreCase = true)) &&
            (selectedGrade == null || syllabus.grade == selectedGrade)
        }.map { subjectSyllabus ->
            if (searchQuery.isBlank()) subjectSyllabus
            else {
                val q = searchQuery.trim().lowercase()
                val matchingUnits = subjectSyllabus.units.mapNotNull { unit ->
                    val matchingTopics = unit.topics.filter { topic ->
                        topic.name.lowercase().contains(q) ||
                        topic.subtopics.any { it.lowercase().contains(q) } ||
                        unit.title.lowercase().contains(q)
                    }
                    if (matchingTopics.isNotEmpty()) unit.copy(topics = matchingTopics) else null
                }
                subjectSyllabus.copy(units = matchingUnits)
            }
        }.filter { it.units.isNotEmpty() }
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "📚 EUEE Syllabus Mastery",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Official Grades 11-12 Curriculum Tracker",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${(overallProgress * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        Spacer(Modifier.height(12.dp))
                        // Quick status legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatusLegend(label = "Not Started", color = Color.Gray)
                            StatusLegend(label = "Studying", color = MaterialTheme.colorScheme.primary)
                            StatusLegend(label = "Revision", color = Color(0xFFE65100))
                            StatusLegend(label = "Mastered", color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            // Search and Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search units, topics or concepts...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Subject filter chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        subjects.forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { selectedSubject = subj },
                                label = { Text(subj, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Grade filter
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedGrade == null,
                            onClick = { selectedGrade = null },
                            label = { Text("All Grades", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedGrade == 11,
                            onClick = { selectedGrade = if (selectedGrade == 11) null else 11 },
                            label = { Text("Grade 11", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedGrade == 12,
                            onClick = { selectedGrade = if (selectedGrade == 12) null else 12 },
                            label = { Text("Grade 12", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Units and Topics List
            filteredCurriculum.forEach { subjectItem ->
                item {
                    val subjectProgress = syllabusManager.calculateProgress(subjectItem.subject, subjectItem.grade)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${subjectItem.subject} (Grade ${subjectItem.grade})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Progress: ${(subjectProgress * 100).toInt()}% completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(subjectItem.units, key = { it.id }) { unit ->
                    UnitCard(
                        unit = unit,
                        statusMap = statusMap,
                        onUpdateStatus = { topicId, newStatus ->
                            scope.launch { syllabusManager.setStatus(topicId, newStatus) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun UnitCard(
    unit: SyllabusUnit,
    statusMap: Map<String, SyllabusStatus>,
    onUpdateStatus: (String, SyllabusStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unit ${unit.unitNumber}: ${unit.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${unit.topics.size} key topics",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Unit"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    unit.topics.forEach { topic ->
                        val currentStatus = statusMap[topic.id] ?: SyllabusStatus.NOT_STARTED

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = topic.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Cycle through status
                                    val nextStatus = when (currentStatus) {
                                        SyllabusStatus.NOT_STARTED -> SyllabusStatus.STUDYING
                                        SyllabusStatus.STUDYING -> SyllabusStatus.NEEDS_REVISION
                                        SyllabusStatus.NEEDS_REVISION -> SyllabusStatus.MASTERED
                                        SyllabusStatus.MASTERED -> SyllabusStatus.NOT_STARTED
                                    }

                                    val statusColor = when (currentStatus) {
                                        SyllabusStatus.NOT_STARTED -> Color.Gray
                                        SyllabusStatus.STUDYING -> MaterialTheme.colorScheme.primary
                                        SyllabusStatus.NEEDS_REVISION -> Color(0xFFE65100)
                                        SyllabusStatus.MASTERED -> Color(0xFF2E7D32)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = statusColor.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor),
                                        modifier = Modifier.clickable {
                                            onUpdateStatus(topic.id, nextStatus)
                                        }
                                    ) {
                                        Text(
                                            text = currentStatus.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (topic.subtopics.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    topic.subtopics.forEach { sub ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                                        ) {
                                            Text("• ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
