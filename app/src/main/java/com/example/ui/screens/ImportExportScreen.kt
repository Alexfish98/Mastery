package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.ChemistryLogic
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.util.ExportHelper
import com.example.util.SandboxStorageHelper
import com.example.viewmodel.ChemistryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportExportScreen(viewModel: ChemistryViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeBank by viewModel.activeBank.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val srs by viewModel.srs.collectAsState()

    var pastedJson by remember { mutableStateOf("") }
    var importAnalysis by remember { mutableStateOf<ChemistryLogic.AnalyzeResult?>(null) }
    var importMode by remember { mutableStateOf("merge") } // "merge", "new", "replace"
    var newBankName by remember { mutableStateOf("") }

    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableLongStateOf(0L) }
    var isValidating by remember { mutableStateOf(false) }
    var validationStatusText by remember { mutableStateOf("") }
    var showManualPasteField by remember { mutableStateOf(false) }

    var exportScope by remember { mutableStateOf("all") } // "all", "wrong", "marked"
    var includeProgressInJson by remember { mutableStateOf(true) }

    var diagnosticChecks by remember { mutableStateOf<List<ChemistryLogic.DiagnosticCheck>?>(null) }

    // File Picker Launcher for JSON import using safe background stream
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = SandboxStorageHelper.getFileName(context, uri) ?: "questions.json"
            selectedFileName = fileName
            isValidating = true
            validationStatusText = "Analyzing structured stream from $fileName..."
            pastedJson = ""
            importAnalysis = null

            coroutineScope.launch {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        selectedFileSize = pfd.statSize
                    }
                } catch (_: Exception) {}

                val result = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            ChemistryLogic.analyzeStream(stream)
                        } ?: ChemistryLogic.AnalyzeResult(err = "Failed to open input stream")
                    } catch (e: Exception) {
                        ChemistryLogic.AnalyzeResult(err = "Read error: ${e.message}")
                    }
                }
                isValidating = false
                importAnalysis = result
                validationStatusText = if (result.err != null) "Analysis failed: ${result.err}" else "Validated ${result.ok.size} questions from $fileName"
            }
        }
    }

    // File Picker Launcher for Backup Restore
    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    if (content.isNotBlank()) {
                        viewModel.restoreBackupJson(content)
                    }
                } catch (e: Exception) {
                    viewModel.showToast("Could not restore backup: ${e.message}")
                }
            }
        }
    }

    val sampleExampleJson = """
[
  {
    "id": "chem-001",
    "question": "What is the chemical symbol for sodium?",
    "answer": "Na",
    "type": "formula",
    "explanation": "Sodium is represented by Na, derived from its Latin name natrium.",
    "topic": "Chemical symbols",
    "difficulty": "easy",
    "hints": ["It is a two-letter symbol starting with N."]
  }
]
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Import & Export Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)

        // 1. Import Section
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Import Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Import questions from any JSON file safely. The engine validates question structures in the background without cluttering the screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { jsonFilePickerLauncher.launch("*/*") },
                        enabled = !isValidating
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedFileName == null) "Pick .json File" else "Change File")
                    }
                    OutlinedButton(
                        onClick = {
                            showManualPasteField = !showManualPasteField
                        }
                    ) {
                        Icon(if (showManualPasteField) Icons.Default.ExpandLess else Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (showManualPasteField) "Hide Paste" else "Paste Text")
                    }
                }

                // File Staging Card (Replaces the raw code dump!)
                if (selectedFileName != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedFileName ?: "JSON File", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                val sizeKb = (selectedFileSize / 1024.0).toInt()
                                Text(
                                    if (selectedFileSize > 0) "$sizeKb KB • Local File" else "Ready for Ingestion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (isValidating) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(validationStatusText, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }

                // Only show manual paste field if user explicitly clicked "Paste Text"
                if (showManualPasteField) {
                    OutlinedTextField(
                        value = pastedJson,
                        onValueChange = {
                            pastedJson = it
                            if (it.isNotBlank()) importAnalysis = ChemistryLogic.analyze(it)
                        },
                        label = { Text("Paste JSON array (optional)") },
                        placeholder = { Text("[{\"prompt\": \"...\", \"answers\": [\"...\"]}]") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                importAnalysis?.let { analysis ->
                    if (analysis.err != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Error: ${analysis.err}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        val plan = ChemistryLogic.plan(analysis.ok, activeBank.questions)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Preview Validation", fontWeight = FontWeight.Bold)
                                Text("${analysis.total} records · ${analysis.ok.size} valid · ${analysis.rej.size} rejected")
                                Text("Merge into '${activeBank.name}': ${plan.add.size} to add · ${plan.rep.size} to replace · ${plan.skip.size} to skip")
                            }
                        }

                        // Target mode selector
                        Column {
                            Text("Apply mode:", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = importMode == "merge",
                                    onClick = { importMode = "merge" },
                                    label = { Text("Merge into Bank") }
                                )
                                FilterChip(
                                    selected = importMode == "new",
                                    onClick = { importMode = "new" },
                                    label = { Text("Add as New Bank") }
                                )
                                FilterChip(
                                    selected = importMode == "replace",
                                    onClick = { importMode = "replace" },
                                    label = { Text("Replace Bank") }
                                )
                            }
                        }

                        if (importMode == "new") {
                            OutlinedTextField(
                                value = newBankName,
                                onValueChange = { newBankName = it },
                                label = { Text("Name for new bank") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.applyImport(analysis, importMode, newBankName)
                                pastedJson = ""
                                importAnalysis = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Import (${analysis.ok.size} Questions)")
                        }
                    }
                }
            }
        }

        // 2. Export Section
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Export questions and study data from '${activeBank.name}' in standardized formats:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Scope
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = exportScope == "all",
                        onClick = { exportScope = "all" },
                        label = { Text("All (${activeBank.questions.size})") }
                    )
                    FilterChip(
                        selected = exportScope == "wrong",
                        onClick = { exportScope = "wrong" },
                        label = { Text("Notebook (${viewModel.getFilteredQuestions("wrong").size})") }
                    )
                    FilterChip(
                        selected = exportScope == "marked",
                        onClick = { exportScope = "marked" },
                        label = { Text("Bookmarked (${viewModel.getFilteredQuestions("marked").size})") }
                    )
                }

                val questionsToExport = when (exportScope) {
                    "wrong" -> viewModel.getFilteredQuestions("wrong")
                    "marked" -> viewModel.getFilteredQuestions("marked")
                    else -> activeBank.questions
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeProgressInJson,
                        onCheckedChange = { includeProgressInJson = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Include my learning progress in JSON", style = MaterialTheme.typography.bodySmall)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        val json = viewModel.exportQuestionBankJson(includeProgress = includeProgressInJson)
                        ExportHelper.shareText(context, "${activeBank.name}_questions.json", json)
                    }) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Questions JSON")
                    }

                    FilledTonalButton(onClick = {
                        try {
                            val pdfFile = ExportHelper.generateStudySheetPdf(context, activeBank.name, questionsToExport)
                            ExportHelper.sharePdfFile(context, "${activeBank.name} Study Sheet", pdfFile)
                        } catch (e: Exception) {
                            viewModel.showToast("Could not generate PDF: ${e.message}")
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF Study Sheet")
                    }

                    FilledTonalButton(onClick = {
                        try {
                            val wrongQuestions = viewModel.getFilteredQuestions("wrong")
                            if (wrongQuestions.isEmpty()) {
                                viewModel.showToast("No incorrect questions recorded in notebook.")
                            } else {
                                val pdfFile = ExportHelper.generateIncorrectQuestionsReportPdf(context, activeBank.name, wrongQuestions, progress)
                                ExportHelper.sharePdfFile(context, "${activeBank.name} Incorrect Questions Report", pdfFile)
                            }
                        } catch (e: Exception) {
                            viewModel.showToast("Could not generate PDF: ${e.message}")
                        }
                    }) {
                        Icon(Icons.Default.AssignmentLate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF Mistake Report")
                    }

                    FilledTonalButton(onClick = {
                        val csv = ExportHelper.buildCsv(questionsToExport, progress, srs)
                        ExportHelper.shareText(context, "${activeBank.name}_questions.csv", csv)
                    }) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share CSV")
                    }

                    FilledTonalButton(onClick = {
                        val txt = ExportHelper.buildRevisionSheet(questionsToExport)
                        ExportHelper.shareText(context, "${activeBank.name}_revision_sheet.txt", txt)
                    }) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Revision Sheet (.txt)")
                    }
                }

                HorizontalDivider()

                Text("Full Backup & Restore", fontWeight = FontWeight.Bold)
                Text("Backs up all question banks, progress, schedules, exams, AND student notes, tasks, and flashcards into a portable JSON backup file.", style = MaterialTheme.typography.bodySmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val backupJson = viewModel.exportBackupJson()
                            ExportHelper.shareText(context, "EUEE-Mastery-full-backup.json", backupJson)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download Full Backup")
                    }

                    OutlinedButton(
                        onClick = { backupRestoreLauncher.launch("application/json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore Full Backup")
                    }
                }
            }
        }

        // Workspace Data Card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Study Notes & Workspace JSON", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Import or export only notes, planner tasks, and flashcard decks without modifying question banks.", style = MaterialTheme.typography.bodySmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val wsJson = viewModel.exportWorkspaceJson()
                            ExportHelper.shareText(context, "EUEE-Mastery-workspace.json", wsJson)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export Workspace")
                    }

                    OutlinedButton(
                        onClick = { jsonFilePickerLauncher.launch("application/json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import Workspace")
                }
            }
        }

        // 3. Diagnostics / Self-test
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Diagnostics & Self-Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Runs comprehensive internal regression tests across equation balancing, molar mass, periodic table, and Leitner scheduler.", style = MaterialTheme.typography.bodySmall)

                Button(
                    onClick = {
                        diagnosticChecks = ChemistryLogic.runDiagnostics(activeBank.questions)
                    }
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Run Self-Test")
                }

                diagnosticChecks?.let { checks ->
                    val passed = checks.count { it.passed }
                    Text(
                        text = "$passed of ${checks.size} checks passed.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (passed == checks.size) SuccessGreen else MaterialTheme.colorScheme.error
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        checks.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (c.passed) "✓ " else "✗ ", color = if (c.passed) SuccessGreen else ErrorRed, fontWeight = FontWeight.Bold)
                                Text(c.name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
