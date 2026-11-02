package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.logic.ChemistryLogic
import com.example.model.Question
import com.example.model.QuestionBank
import com.example.ui.theme.SuccessGreen
import com.example.util.ExportHelper
import com.example.viewmodel.ChemistryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: ChemistryViewModel) {
    val context = LocalContext.current
    val activeBank by viewModel.activeBank.collectAsState()
    val allBanks by viewModel.banks.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val srs by viewModel.srs.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("id") }

    var editingQuestion by remember { mutableStateOf<Question?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    var showBankMenu by remember { mutableStateOf(false) }
    var showNewBankDialog by remember { mutableStateOf(false) }
    var newBankName by remember { mutableStateOf("") }

    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeSourceBankId by remember { mutableStateOf("") }

    val topics = remember(activeBank) {
        listOf("") + activeBank.questions.map { it.topic }.distinct().sorted()
    }

    val difficulties = remember(activeBank) {
        listOf("", "easy", "medium", "hard")
    }

    val filteredQuestions = remember(activeBank, searchQuery, selectedTopic, selectedDifficulty, sortBy) {
        val qNorm = ChemistryLogic.norm(searchQuery)
        activeBank.questions.filter { q ->
            (selectedTopic.isEmpty() || q.topic == selectedTopic) &&
                (selectedDifficulty.isEmpty() || q.difficulty == selectedDifficulty) &&
                (qNorm.isEmpty() || ChemistryLogic.norm("${q.prompt} ${q.answers.joinToString(" ")} ${q.tags.joinToString(" ")}").contains(qNorm))
        }.sortedWith(
            when (sortBy) {
                "topic" -> compareBy { it.topic }
                "difficulty" -> compareBy { it.difficulty }
                "prompt" -> compareBy { it.prompt }
                else -> compareBy { it.id }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingQuestion = null
                    showEditDialog = true
                },
                modifier = Modifier.testTag("add_question_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Question")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Header with Bank Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Question Library",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${filteredQuestions.size} matching · ${activeBank.questions.size} total in bank",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        OutlinedButton(onClick = { showBankMenu = true }) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(activeBank.name.take(14) + if (activeBank.name.length > 14) "…" else "")
                        }
                        DropdownMenu(
                            expanded = showBankMenu,
                            onDismissRequest = { showBankMenu = false }
                        ) {
                            allBanks.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text("${b.name} (${b.questions.size})") },
                                    onClick = {
                                        viewModel.selectBank(b.id)
                                        showBankMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("＋ Create New Bank") },
                                onClick = {
                                    showBankMenu = false
                                    showNewBankDialog = true
                                }
                            )
                            if (allBanks.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Merge Another Bank...") },
                                    onClick = {
                                        showBankMenu = false
                                        showMergeDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete This Bank", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showBankMenu = false
                                        viewModel.deleteCurrentBank()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Action Toolbar (New Question, Restore, Merge)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingQuestion = null
                            showEditDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                    OutlinedButton(
                        onClick = { viewModel.restoreLastDeleted() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore")
                    }
                }
            }

            // Search Bar & Filter Controls
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by question, answer, tag...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            // Topic Filter Dropdown
                            var topicExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { topicExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedTopic.ifEmpty { "All Topics" }, maxLines = 1)
                                }
                                DropdownMenu(expanded = topicExpanded, onDismissRequest = { topicExpanded = false }) {
                                    topics.forEach { top ->
                                        DropdownMenuItem(
                                            text = { Text(top.ifEmpty { "All Topics" }) },
                                            onClick = {
                                                selectedTopic = top
                                                topicExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Difficulty Filter Dropdown
                            var diffExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { diffExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedDifficulty.ifEmpty { "Any Level" }, maxLines = 1)
                                }
                                DropdownMenu(expanded = diffExpanded, onDismissRequest = { diffExpanded = false }) {
                                    difficulties.forEach { diff ->
                                        DropdownMenuItem(
                                            text = { Text(diff.ifEmpty { "Any Level" }.replaceFirstChar { it.uppercase() }) },
                                            onClick = {
                                                selectedDifficulty = diff
                                                diffExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Questions List
            if (filteredQuestions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No questions match your filter.\nTap 'New' or 'Import' to add chemistry questions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredQuestions, key = { it.id }) { q ->
                    val p = progress[q.id]
                    val r = srs[q.id]
                    val isBookmarked = bookmarks.contains(q.id)

                    val statusLabel = when {
                        p == null || p.att == 0 -> "New"
                        p.state == "mastered" -> "Mastered"
                        r != null && r.due <= System.currentTimeMillis() -> "Due"
                        else -> "Learning"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SuggestionChip(onClick = {}, label = { Text(q.topic) })
                                    SuggestionChip(onClick = {}, label = { Text(q.difficulty.replaceFirstChar { it.uppercase() }) })
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(statusLabel) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (statusLabel == "Mastered") SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleBookmark(q.id) }) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isBookmarked) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = q.prompt,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Answer: ${ChemistryLogic.exp(q)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    ExportHelper.shareQuestionImage(context, q)
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Image")
                                }
                                TextButton(onClick = {
                                    editingQuestion = q
                                    showEditDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                Button(
                                    onClick = {
                                        viewModel.startStudy("practice", listOf(q), "Single Question Practice")
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Study")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        QuestionEditDialog(
            question = editingQuestion,
            currentBank = activeBank,
            allBanks = allBanks,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false }
        )
    }

    if (showNewBankDialog) {
        AlertDialog(
            onDismissRequest = { showNewBankDialog = false },
            title = { Text("Create Question Bank") },
            text = {
                OutlinedTextField(
                    value = newBankName,
                    onValueChange = { newBankName = it },
                    label = { Text("Bank name (e.g. Organic Chemistry)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBankName.isNotBlank()) {
                            viewModel.createBank(newBankName)
                            newBankName = ""
                            showNewBankDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBankDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMergeDialog) {
        val otherBanks = allBanks.filter { it.id != activeBank.id }
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Merge Bank into '${activeBank.name}'") },
            text = {
                Column {
                    Text("Select bank to merge from. Duplicates will be skipped:")
                    Spacer(Modifier.height(8.dp))
                    otherBanks.forEach { b ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = mergeSourceBankId == b.id,
                                onClick = { mergeSourceBankId = b.id }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${b.name} (${b.questions.size} questions)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mergeSourceBankId.isNotEmpty()) {
                            viewModel.mergeBanks(mergeSourceBankId)
                            showMergeDialog = false
                        }
                    },
                    enabled = mergeSourceBankId.isNotEmpty()
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
