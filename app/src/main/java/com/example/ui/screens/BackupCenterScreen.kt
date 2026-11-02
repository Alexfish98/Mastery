package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.util.ZipBackupHelper
import com.example.util.ZipRestorePreview
import com.example.viewmodel.ChemistryViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupCenterScreen(
    viewModel: ChemistryViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var incNotes by remember { mutableStateOf(true) }
    var incTasks by remember { mutableStateOf(true) }
    var incCards by remember { mutableStateOf(true) }
    var incBanks by remember { mutableStateOf(true) }
    var incProgress by remember { mutableStateOf(true) }
    var incAlarms by remember { mutableStateOf(true) }

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var restorePreview by remember { mutableStateOf<ZipRestorePreview?>(null) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreMode by remember { mutableStateOf("merge") } // "merge" or "replace"

    var pendingSaveFile by remember { mutableStateOf<File?>(null) }

    // Launcher for saving ZIP file to device storage
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && pendingSaveFile != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(pendingSaveFile!!).use { input ->
                            input.copyTo(output)
                        }
                    }
                    Toast.makeText(context, "ZIP backup saved successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launcher for selecting ZIP to restore
    val openZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
            scope.launch {
                isImporting = true
                val preview = ZipBackupHelper.inspectZip(context, uri)
                isImporting = false
                if (preview.isValid) {
                    restorePreview = preview
                } else {
                    Toast.makeText(context, preview.errorMessage ?: "Invalid ZIP file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notice Card: App Data Backup vs Source Code Download
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "App Data Backup vs Source Code",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "• This Backup Center exports your private study data, notes, flashcards, questions, and alarm schedules into an encrypted/compressed .zip archive.\n• To download the complete editable Android Studio Kotlin project source code, tap Settings / Download project in the AI Studio platform header.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Export Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Export Everything as ZIP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Select which study content categories to bundle into the ZIP archive:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))

                        CheckboxRow("Notes & Formatted Highlights", incNotes) { incNotes = it }
                        CheckboxRow("Planner Tasks & Deadlines", incTasks) { incTasks = it }
                        CheckboxRow("Flashcards Deck & Memory Scores", incCards) { incCards = it }
                        CheckboxRow("All Question Banks & Custom Problems", incBanks) { incBanks = it }
                        CheckboxRow("SRS Spaced Repetition Boxes & Progress", incProgress) { incProgress = it }
                        CheckboxRow("Scheduled Study Alarms & Exam Dates", incAlarms) { incAlarms = it }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isExporting = true
                                        val file = viewModel.createZipArchive(
                                            incNotes, incTasks, incCards, incBanks, incProgress, incAlarms
                                        )
                                        isExporting = false
                                        pendingSaveFile = file
                                        createDocumentLauncher.launch(file.name)
                                    }
                                },
                                enabled = !isExporting,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.SaveAlt, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Save to Files")
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        isExporting = true
                                        val file = viewModel.createZipArchive(
                                            incNotes, incTasks, incCards, incBanks, incProgress, incAlarms
                                        )
                                        isExporting = false
                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/zip"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Study Backup ZIP"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isExporting
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }

            // Restore Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Unarchive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text("Restore from ZIP Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Select a previously generated EUEE Mastery .zip archive. The system will inspect its manifest and verify its contents before restoring.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                            enabled = !isImporting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary)
                            } else {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Select ZIP Backup File")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
            }
        }

        // Restore Preview & Conflict Handling Dialog
        restorePreview?.let { preview ->
            val dateStr = if (preview.exportedAt > 0) {
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(preview.exportedAt))
            } else "Unknown"

            AlertDialog(
                onDismissRequest = { restorePreview = null },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Backup Archive Verified") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Created: $dateStr", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Contents detected in archive:", style = MaterialTheme.typography.bodySmall)
                        Text("• Notes: ${preview.noteCount}")
                        Text("• Study Tasks: ${preview.taskCount}")
                        Text("• Flashcards: ${preview.flashcardCount}")
                        Text("• Question Banks: ${preview.bankCount} (${preview.questionCount} questions)")
                        Text("• Alarms: ${preview.alarmCount}")

                        Spacer(Modifier.height(8.dp))
                        Text("Restore Mode:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = restoreMode == "merge",
                                onClick = { restoreMode = "merge" }
                            )
                            Text("Merge (Preserve existing items, add new)", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = restoreMode == "replace",
                                onClick = { restoreMode = "replace" }
                            )
                            Text("Replace (Overwrite with archive data)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val success = viewModel.applyZipRestore(preview, restoreMode)
                                restorePreview = null
                                if (success) {
                                    Toast.makeText(context, "Backup successfully restored!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to restore backup items", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Apply Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { restorePreview = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
