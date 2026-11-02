package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.domain.model.QuestionItem
import com.example.util.SandboxStorageHelper
import com.example.viewmodel.IqaiViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionStudioScreen(
    viewModel: IqaiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allQuestions by viewModel.allQuestions.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Dual-Column Practice & Review, 1 = Question Bank CRUD Studio
    var selectedSubject by remember { mutableStateOf("All") }

    // Practice Review State
    var activeQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var isDropdownLayoutMode by remember { mutableStateOf(false) }

    // CRUD & Import State
    var showQuestionDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableLongStateOf(0L) }
    var importSourceTitle by remember { mutableStateOf("EUEE_Question_Bank") }
    var importSubject by remember { mutableStateOf("Chemistry") }
    var isIngesting by remember { mutableStateOf(false) }
    var ingestionStatusText by remember { mutableStateOf("Select a .json file to begin high-speed stream") }
    var ingestedCount by remember { mutableIntStateOf(0) }
    var questionToEdit by remember { mutableStateOf<QuestionItem?>(null) }

    // Streaming JSON file picker with zero-raw-text memory footprint
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            val fileName = SandboxStorageHelper.getFileName(context, uri) ?: "EUEE_Questions.json"
            selectedFileName = fileName
            importSourceTitle = fileName.substringBeforeLast(".")
            ingestionStatusText = "File staged: $fileName. Ready for streaming ingestion."
            ingestedCount = 0
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    selectedFileSize = pfd.statSize
                }
            } catch (_: Exception) {}
        }
    }

    val subjects = listOf("All", "Chemistry", "Physics", "Biology", "Mathematics", "English")

    val filteredQuestions = remember(allQuestions, selectedSubject) {
        if (selectedSubject == "All") allQuestions
        else allQuestions.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
    }

    val currentQuestion = remember(filteredQuestions, activeQuestionIndex) {
        if (filteredQuestions.isNotEmpty()) {
            filteredQuestions[activeQuestionIndex.coerceIn(0, filteredQuestions.lastIndex)]
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = {
                    Column {
                        Text(
                            text = "Question Studio & MCQ Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allQuestions.size} active entrance exam questions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showImportDialog = true
                        },
                        modifier = Modifier.testTag("import_questions_json_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import JSON Questions")
                    }
                    IconButton(
                        onClick = {
                            questionToEdit = null
                            showQuestionDialog = true
                        },
                        modifier = Modifier.testTag("add_mcq_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Question")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Tabs
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Dual-Column Practice", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Question Bank CRUD", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            // Subject Filter Bar
            ScrollableTabRow(
                selectedTabIndex = subjects.indexOf(selectedSubject).coerceAtLeast(0),
                edgePadding = 16.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                subjects.forEach { s ->
                    val count = if (s == "All") allQuestions.size else allQuestions.count { it.subject.equals(s, ignoreCase = true) }
                    Tab(
                        selected = selectedSubject == s,
                        onClick = {
                            selectedSubject = s
                            activeQuestionIndex = 0
                            selectedOptionIndex = null
                            showExplanation = false
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(s, fontSize = 12.sp, fontWeight = if (selectedSubject == s) FontWeight.Bold else FontWeight.Normal)
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedSubject == s) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$count",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSubject == s) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (activeTab == 0) {
                // ==========================================
                // DUAL-COLUMN PRACTICE & REVIEW UI
                // ==========================================
                if (currentQuestion == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No questions available for '$selectedSubject'. Add questions in the CRUD tab.")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header with Sequential Toggle Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Question ${activeQuestionIndex + 1} of ${filteredQuestions.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Toggle layout mode (Dual-column vs Dropdown/Stack)
                                IconButton(
                                    onClick = { isDropdownLayoutMode = !isDropdownLayoutMode },
                                    modifier = Modifier.testTag("toggle_layout_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDropdownLayoutMode) Icons.Default.ViewAgenda else Icons.Default.ViewColumn,
                                        contentDescription = "Toggle Dual-Column / Dropdown View"
                                    )
                                }

                                // SEQUENTIAL TOGGLE NAVIGATION BUTTON
                                // Directly adjacent to selection, shifts focus/active selection to exact next item down seamlessly
                                FilledTonalButton(
                                    onClick = {
                                        if (filteredQuestions.isNotEmpty()) {
                                            activeQuestionIndex = (activeQuestionIndex + 1) % filteredQuestions.size
                                            selectedOptionIndex = null
                                            showExplanation = false
                                        }
                                    },
                                    modifier = Modifier.testTag("sequential_toggle_navigation_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Next Question", fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Question Prompt Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = currentQuestion.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (currentQuestion.chapter.isNotBlank()) {
                                        Text(
                                            text = currentQuestion.chapter,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = currentQuestion.prompt,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 24.sp
                                )
                            }
                        }

                        // DUAL-COLUMN LAYOUT CONFIGURATION
                        // Align odd options (A, C -> index 0, 2) on LEFT side of grid
                        // Align even options (B, D -> index 1, 3) on RIGHT side of grid
                        if (!isDropdownLayoutMode) {
                            Text(
                                text = "Select Answer (Dual-Column Layout):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // LEFT COLUMN: ODD OPTIONS (Option A = 0, Option C = 2)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OptionSelectCard(
                                        label = "A (Odd #1)",
                                        text = currentQuestion.optionA,
                                        isSelected = selectedOptionIndex == 0,
                                        isCorrect = showExplanation && currentQuestion.correctOptionIndex == 0,
                                        isWrong = showExplanation && selectedOptionIndex == 0 && currentQuestion.correctOptionIndex != 0,
                                        onClick = {
                                            selectedOptionIndex = 0
                                            showExplanation = true
                                        }
                                    )

                                    OptionSelectCard(
                                        label = "C (Odd #3)",
                                        text = currentQuestion.optionC,
                                        isSelected = selectedOptionIndex == 2,
                                        isCorrect = showExplanation && currentQuestion.correctOptionIndex == 2,
                                        isWrong = showExplanation && selectedOptionIndex == 2 && currentQuestion.correctOptionIndex != 2,
                                        onClick = {
                                            selectedOptionIndex = 2
                                            showExplanation = true
                                        }
                                    )
                                }

                                // RIGHT COLUMN: EVEN OPTIONS (Option B = 1, Option D = 3)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OptionSelectCard(
                                        label = "B (Even #2)",
                                        text = currentQuestion.optionB,
                                        isSelected = selectedOptionIndex == 1,
                                        isCorrect = showExplanation && currentQuestion.correctOptionIndex == 1,
                                        isWrong = showExplanation && selectedOptionIndex == 1 && currentQuestion.correctOptionIndex != 1,
                                        onClick = {
                                            selectedOptionIndex = 1
                                            showExplanation = true
                                        }
                                    )

                                    OptionSelectCard(
                                        label = "D (Even #4)",
                                        text = currentQuestion.optionD,
                                        isSelected = selectedOptionIndex == 3,
                                        isCorrect = showExplanation && currentQuestion.correctOptionIndex == 3,
                                        isWrong = showExplanation && selectedOptionIndex == 3 && currentQuestion.correctOptionIndex != 3,
                                        onClick = {
                                            selectedOptionIndex = 3
                                            showExplanation = true
                                        }
                                    )
                                }
                            }
                        } else {
                            // STACKED DROPDOWN / LIST UTILITY
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Pair(0, "A: ${currentQuestion.optionA}"),
                                    Pair(1, "B: ${currentQuestion.optionB}"),
                                    Pair(2, "C: ${currentQuestion.optionC}"),
                                    Pair(3, "D: ${currentQuestion.optionD}")
                                ).forEach { (optIdx, optText) ->
                                    OptionSelectCard(
                                        label = listOf("A", "B", "C", "D")[optIdx],
                                        text = optText,
                                        isSelected = selectedOptionIndex == optIdx,
                                        isCorrect = showExplanation && currentQuestion.correctOptionIndex == optIdx,
                                        isWrong = showExplanation && selectedOptionIndex == optIdx && currentQuestion.correctOptionIndex != optIdx,
                                        onClick = {
                                            selectedOptionIndex = optIdx
                                            showExplanation = true
                                        }
                                    )
                                }
                            }
                        }

                        // Explanation Card
                        if (showExplanation) {
                            val isCorrect = selectedOptionIndex == currentQuestion.correctOptionIndex
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCorrect) Color(0xFF10B981).copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = if (isCorrect) "Correct Answer!" else "Incorrect. Review Solution:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = currentQuestion.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // QUESTION BANK MANAGEMENT (FULL CRUD) TAB
                // ==========================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredQuestions, key = { _, item -> item.id }) { index, q ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = q.subject,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "#${index + 1} • ${q.difficulty.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                questionToEdit = q
                                                showQuestionDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Question", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteQuestion(q.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Question", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = q.prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "Correct: ${listOf("A", "B", "C", "D").getOrElse(q.correctOptionIndex) { "A" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuestionDialog) {
        QuestionEditDialog(
            initialQuestion = questionToEdit,
            onDismiss = {
                showQuestionDialog = false
                questionToEdit = null
            },
            onSave = { saved ->
                if (questionToEdit == null) {
                    viewModel.saveQuestion(saved)
                } else {
                    viewModel.updateQuestion(saved)
                }
                showQuestionDialog = false
                questionToEdit = null
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isIngesting) showImportDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("High-Speed Stream Ingestion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Optimized structured stream reads question objects directly into Room SQLite without RAM overhead. Filenames automatically title the target question group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { jsonFilePickerLauncher.launch("*/*") },
                        enabled = !isIngesting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedFileName.isBlank()) "Select Local .JSON File" else "Change File")
                    }

                    if (selectedFileName.isNotBlank()) {
                        // File Stage Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedFileName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    val sizeKb = (selectedFileSize / 1024.0).toInt()
                                    Text(
                                        if (selectedFileSize > 0) "$sizeKb KB • Ready for streaming" else "Local JSON Bank",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = importSourceTitle,
                            onValueChange = { importSourceTitle = it },
                            label = { Text("Target Group / Chapter Title") },
                            enabled = !isIngesting,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Subject Selector
                        Text("Target Subject:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Chemistry", "Physics", "Biology", "Mathematics", "English").forEach { sub ->
                                val isSel = importSubject.equals(sub, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = !isIngesting) { importSubject = sub }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = sub.take(4),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dynamic Ingestion Telemetry & Progress
                    if (isIngesting) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text(ingestionStatusText, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else if (ingestedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Success: Ingested $ingestedCount questions into Room SQLite!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = ingestionStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedFileUri
                        if (uri != null) {
                            isIngesting = true
                            ingestionStatusText = "Ingesting Data from Stream..."
                            coroutineScope.launch {
                                try {
                                    val stream = context.contentResolver.openInputStream(uri)
                                    if (stream != null) {
                                        val count = viewModel.importQuestionsFromStream(
                                            inputStream = stream,
                                            defaultSubject = importSubject,
                                            defaultGroup = importSourceTitle.ifBlank { "EUEE Questions" },
                                            onProgress = { count, status ->
                                                ingestionStatusText = status
                                            }
                                        )
                                        ingestedCount = count
                                        isIngesting = false
                                        ingestionStatusText = "Success: Ingested $count questions!"
                                        Toast.makeText(context, "Ingested $count questions into Room SQLite!", Toast.LENGTH_LONG).show()
                                    } else {
                                        isIngesting = false
                                        ingestionStatusText = "Failed to open input stream."
                                    }
                                } catch (e: Exception) {
                                    isIngesting = false
                                    ingestionStatusText = "Ingestion failed: ${e.localizedMessage}"
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please select a .json file first.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isIngesting && selectedFileUri != null
                ) {
                    Text(if (isIngesting) "Streaming..." else "Ingest Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    enabled = !isIngesting
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun OptionSelectCard(
    label: String,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCorrect -> Color(0xFF10B981)
        isWrong -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    val containerColor = when {
        isCorrect -> Color(0xFF10B981).copy(alpha = 0.15f)
        isWrong -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = borderColor,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label.take(1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected || isCorrect || isWrong) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuestionEditDialog(
    initialQuestion: QuestionItem?,
    onDismiss: () -> Unit,
    onSave: (QuestionItem) -> Unit
) {
    var subject by remember { mutableStateOf(initialQuestion?.subject ?: "Chemistry") }
    var chapter by remember { mutableStateOf(initialQuestion?.chapter ?: "") }
    var prompt by remember { mutableStateOf(initialQuestion?.prompt ?: "") }
    var optionA by remember { mutableStateOf(initialQuestion?.optionA ?: "") }
    var optionB by remember { mutableStateOf(initialQuestion?.optionB ?: "") }
    var optionC by remember { mutableStateOf(initialQuestion?.optionC ?: "") }
    var optionD by remember { mutableStateOf(initialQuestion?.optionD ?: "") }
    var correctIndex by remember { mutableIntStateOf(initialQuestion?.correctOptionIndex ?: 0) }
    var explanation by remember { mutableStateOf(initialQuestion?.explanation ?: "") }
    var difficulty by remember { mutableStateOf(initialQuestion?.difficulty ?: "medium") }

    val subjects = listOf("Chemistry", "Physics", "Biology", "Mathematics", "English")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialQuestion == null) "Create MCQ Question" else "Edit MCQ Question", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Subject:", style = MaterialTheme.typography.labelSmall)
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
                }

                item {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Question Prompt") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = optionA,
                        onValueChange = { optionA = it },
                        label = { Text("Option A (Odd #1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = optionB,
                        onValueChange = { optionB = it },
                        label = { Text("Option B (Even #2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = optionC,
                        onValueChange = { optionC = it },
                        label = { Text("Option C (Odd #3)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = optionD,
                        onValueChange = { optionD = it },
                        label = { Text("Option D (Even #4)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Select Correct Option:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("A", "B", "C", "D").forEachIndexed { idx, optName ->
                            FilterChip(
                                selected = correctIndex == idx,
                                onClick = { correctIndex = idx },
                                label = { Text(optName, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Explanation / Solution Steps") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = QuestionItem(
                        id = initialQuestion?.id ?: ("mcq_" + UUID.randomUUID().toString().take(8)),
                        prompt = prompt.trim(),
                        optionA = optionA.trim(),
                        optionB = optionB.trim(),
                        optionC = optionC.trim(),
                        optionD = optionD.trim(),
                        correctOptionIndex = correctIndex,
                        explanation = explanation.trim(),
                        subject = subject,
                        chapter = chapter.trim(),
                        difficulty = difficulty,
                        isUserCustom = true
                    )
                    onSave(item)
                },
                enabled = prompt.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank()
            ) {
                Text("Save Question")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
