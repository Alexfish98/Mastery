package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ChemistryViewModel

import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.example.model.BuilderInfo
import com.example.util.SandboxStorageHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: ChemistryViewModel) {
    val prefs by viewModel.prefs.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val allTextbooks by viewModel.allTextbooks.collectAsState()
    val allBookmarks by viewModel.allBookmarks.collectAsState()
    val allIqaiCards by viewModel.allIqaiCards.collectAsState()
    val allOfflineNotes by viewModel.allOfflineNotes.collectAsState()
    val allIqaiSessions by viewModel.allIqaiSessions.collectAsState()
    val allTimetablePlans by viewModel.allTimetablePlans.collectAsState()
    val allMetricTracks by viewModel.allMetricTracks.collectAsState()
    val allDictEntries by viewModel.allDictEntries.collectAsState()
    val allCustomQuestions by viewModel.allCustomQuestions.collectAsState()

    var showResetProgressDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }
    var showTransferCodeDialog by remember { mutableStateOf(false) }
    var showRestoreTransferDialog by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var tableToPurge by remember { mutableStateOf<Pair<String, String>?>(null) }
    var generatedTransferCode by remember { mutableStateOf("") }
    var inputTransferCode by remember { mutableStateOf("") }

    var dailyGoalText by remember { mutableStateOf(prefs.goal.toString()) }
    var dailyMinutesText by remember { mutableStateOf(prefs.mins.toString()) }
    var newCapText by remember { mutableStateOf(prefs.ncap.toString()) }
    var examDateText by remember { mutableStateOf(prefs.exd) }
    var examSecondsText by remember { mutableStateOf(prefs.exs.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings & Customization", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)

        // Student Profile & Credentials
        var studentNameInput by remember(prefs.studentName) { mutableStateOf(prefs.studentName) }
        var studentGradeInput by remember(prefs.studentGrade) { mutableStateOf(prefs.studentGrade) }
        var studentPhoneInput by remember(prefs.studentPhone) { mutableStateOf(prefs.studentPhone) }
        var studentEmailInput by remember(prefs.studentEmail) { mutableStateOf(prefs.studentEmail) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎓", fontSize = 24.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Student & Builder Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "BUILDER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Official candidate credentials for EUEE Mastery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                OutlinedTextField(
                    value = studentNameInput,
                    onValueChange = { studentNameInput = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studentGradeInput,
                    onValueChange = { studentGradeInput = it },
                    label = { Text("Academic Level / Track") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studentPhoneInput,
                    onValueChange = { studentPhoneInput = it },
                    label = { Text("Phone Number(s)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = studentEmailInput,
                    onValueChange = { studentEmailInput = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            studentNameInput = BuilderInfo.NAME
                            studentGradeInput = BuilderInfo.SHORT_ROLE
                            studentPhoneInput = BuilderInfo.PHONES
                            studentEmailInput = BuilderInfo.EMAIL
                            viewModel.updateStudentProfile(
                                name = BuilderInfo.NAME,
                                grade = BuilderInfo.SHORT_ROLE,
                                phone = BuilderInfo.PHONES,
                                email = BuilderInfo.EMAIL
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset to Builder", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.updateStudentProfile(
                                name = studentNameInput.trim(),
                                grade = studentGradeInput.trim(),
                                phone = studentPhoneInput.trim(),
                                email = studentEmailInput.trim()
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Profile", fontSize = 12.sp)
                    }
                }
            }
        }

        // Dedicated Builder & Developer Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⚡", fontSize = 20.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "App Builder & Developer Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = BuilderInfo.APP_VERSION,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = "${BuilderInfo.NAME} • ${BuilderInfo.GRADE_ROLE}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = BuilderInfo.BIO,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Contacts:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("📞 ${BuilderInfo.PHONES}", style = MaterialTheme.typography.bodySmall)
                        Text("✉️ ${BuilderInfo.EMAIL}", style = MaterialTheme.typography.bodySmall)
                    }

                    IconButton(
                        onClick = {
                            val contactInfo = "Builder: ${BuilderInfo.NAME}\nRole: ${BuilderInfo.GRADE_ROLE}\nPhones: ${BuilderInfo.PHONES}\nEmail: ${BuilderInfo.EMAIL}\nApp: EUEE Mastery"
                            clipboardManager.setText(AnnotatedString(contactInfo))
                            viewModel.showToast("Copied builder contact info!")
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Builder Info")
                    }
                }
            }
        }

        // Appearance
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Pair("auto", "System"), Pair("light", "Light"), Pair("dark", "Dark")).forEach { (key, label) ->
                        FilterChip(
                            selected = prefs.theme == key,
                            onClick = { viewModel.updateTheme(key) },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Accent Color", fontWeight = FontWeight.SemiBold)
                val accents = listOf(
                    Pair("Teal", TealPrimary),
                    Pair("Blue", BluePrimary),
                    Pair("Purple", PurpleAccent),
                    Pair("Pink", PinkAccent),
                    Pair("Orange", OrangeAccent),
                    Pair("Green", GreenAccent),
                    Pair("Graphite", GraphiteAccent)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accents.forEach { (name, color) ->
                        val isSelected = prefs.accent == name
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.updateAccent(name) }
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Goals & Audio
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Study Goals & Daily Targets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sound & Haptic Feedback", fontWeight = FontWeight.SemiBold)
                        Text("Tone synthesis and vibration for correct/incorrect answers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = prefs.snd,
                        onCheckedChange = { viewModel.updateSound(it) }
                    )
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = dailyGoalText,
                    onValueChange = {
                        dailyGoalText = it
                        it.toIntOrNull()?.let { g -> viewModel.updateGoal(g) }
                    },
                    label = { Text("Daily question goal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dailyMinutesText,
                    onValueChange = {
                        dailyMinutesText = it
                        it.toIntOrNull()?.let { m -> viewModel.updateDailyMinutes(m) }
                    },
                    label = { Text("Target study minutes per day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newCapText,
                    onValueChange = {
                        newCapText = it
                        it.toIntOrNull()?.let { c -> viewModel.updateNewCap(c) }
                    },
                    label = { Text("New questions per day limit (ncap)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = examDateText,
                    onValueChange = {
                        examDateText = it
                        viewModel.updateExamDate(it)
                    },
                    label = { Text("Target Exam Date (YYYY-MM-DD)") },
                    placeholder = { Text("e.g. 2026-06-15") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = examSecondsText,
                    onValueChange = {
                        examSecondsText = it
                        it.toIntOrNull()?.let { s -> viewModel.updateExamSeconds(s) }
                    },
                    label = { Text("Exam seconds per question") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Transfer Code Card (Cross-device GZIP Base64 Transfer)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Device Transfer Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Create a compact compressed text code to transfer all your questions, progress, notebook, and notes to another device or restore previous states.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val code = viewModel.createTransferCode()
                            generatedTransferCode = code
                            clipboardManager.setText(AnnotatedString(code))
                            showTransferCodeDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create Code")
                    }

                    OutlinedButton(
                        onClick = { showRestoreTransferDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore Code")
                    }
                }
            }
        }

        // How Scheduling & Mastery Work (From HTML App)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How Spaced Repetition Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "The EUEE Mastery scheduling algorithm uses a 5-box Leitner system augmented with dynamic Ease Factor calculations:",
                    style = MaterialTheme.typography.bodySmall
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Box 0 (New / Re-queue): Same-day review (10 min queue)", style = MaterialTheme.typography.bodySmall)
                    Text("• Box 1: 1 day interval", style = MaterialTheme.typography.bodySmall)
                    Text("• Box 2: 3 days interval", style = MaterialTheme.typography.bodySmall)
                    Text("• Box 3: 7 days interval", style = MaterialTheme.typography.bodySmall)
                    Text("• Box 4: 14 days interval", style = MaterialTheme.typography.bodySmall)
                    Text("• Box 5 (Mastered): 30 days interval", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("• Ease Factor: 1.3 to 3.2 multiplier adjusted automatically based on confidence rating and mistakes.", style = MaterialTheme.typography.bodySmall)
                    Text("• Leech Alert: Questions answered incorrectly 3+ times are highlighted as Leeches for targeted review.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Keyboard Shortcuts & Gestures Reference (From HTML App)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Shortcuts & Gestures Reference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Support for hardware keyboards and fast gesture workflows during study sessions:",
                    style = MaterialTheme.typography.bodySmall
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• Keys 1 – 4: Select multiple-choice option A, B, C, D", style = MaterialTheme.typography.bodySmall)
                    Text("• Enter: Check / Submit answer or proceed to next question", style = MaterialTheme.typography.bodySmall)
                    Text("• Space: Flip flashcard / toggle explanation visibility", style = MaterialTheme.typography.bodySmall)
                    Text("• H: Reveal progressive hint", style = MaterialTheme.typography.bodySmall)
                    Text("• M: Star / bookmark question", style = MaterialTheme.typography.bodySmall)
                    Text("• N: Add / remove question from Mistakes Notebook", style = MaterialTheme.typography.bodySmall)
                    Text("• Left / Right: Previous and next question in library", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Data Storage & Management
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "All learning records, custom questions, and attempts are stored safely on your device storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showResetProgressDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Progress")
                    }

                    Button(
                        onClick = { showResetAllDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset All Data")
                    }
                }
            }
        }

        // Master Data Dashboard & Room SQLite Backbone
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text("Master Data Dashboard & Room Backbone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Real-time telemetry, direct schema CRUD, and local database management", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Table metrics chips
                val tableStats = listOf(
                    Triple("Textbooks", "textbooks", allTextbooks.size),
                    Triple("Bookmarks", "bookmarks", allBookmarks.size),
                    Triple("Flashcards", "iqai_flashcards", allIqaiCards.size),
                    Triple("Offline Notes", "offline_notes", allOfflineNotes.size),
                    Triple("Focus Logs", "iqai_focus_sessions", allIqaiSessions.size),
                    Triple("Timetable", "timetable_plans", allTimetablePlans.size),
                    Triple("Analytics", "study_metric_tracks", allMetricTracks.size),
                    Triple("Dictionary", "dictionary_entries", allDictEntries.size),
                    Triple("Custom MCQs", "custom_mcq_questions", allCustomQuestions.size)
                )

                Text("Room SQLite Entity Record Count:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tableStats.chunked(3).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEach { (label, tableName, count) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(count.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        Spacer(Modifier.height(4.dp))
                                        TextButton(
                                            onClick = { tableToPurge = Pair(label, tableName) },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier.height(20.dp)
                                        ) {
                                            Text("Purge", fontSize = 9.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // In-App File Isolation Sandbox Status
                val sandboxedFiles = remember { SandboxStorageHelper.getSandboxedFiles(context) }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Column {
                                Text("In-App File Sandbox", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${sandboxedFiles.size} offline isolated documents/media", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        TextButton(
                            onClick = {
                                val purged = SandboxStorageHelper.purgeSandbox(context)
                                viewModel.showToast("Purged $purged sandboxed files")
                            }
                        ) {
                            Text("Purge Sandbox", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Master Export Button
                Button(
                    onClick = {
                        val json = viewModel.exportAllTablesJson()
                        clipboardManager.setText(AnnotatedString(json))
                        showExportSuccessDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export All Room Tables (JSON)")
                }
            }
        }
    }

    if (showTransferCodeDialog) {
        AlertDialog(
            onDismissRequest = { showTransferCodeDialog = false },
            title = { Text("Transfer Code Generated") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your transfer code has been copied to your clipboard! You can paste this code into another device's Restore field:")
                    OutlinedTextField(
                        value = generatedTransferCode,
                        onValueChange = {},
                        readOnly = true,
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(generatedTransferCode))
                    viewModel.showToast("Copied code to clipboard!")
                    showTransferCodeDialog = false
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy & Close")
                }
            }
        )
    }

    if (showRestoreTransferDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreTransferDialog = false },
            title = { Text("Restore From Transfer Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste a transfer code generated from another device below:")
                    OutlinedTextField(
                        value = inputTransferCode,
                        onValueChange = { inputTransferCode = it },
                        placeholder = { Text("Paste code here...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputTransferCode.isNotBlank()) {
                        viewModel.restoreTransferCode(inputTransferCode.trim())
                        showRestoreTransferDialog = false
                    }
                }) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreTransferDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = { showResetProgressDialog = false },
            title = { Text("Reset Learning Progress") },
            text = { Text("Reset all answer attempts, schedules, and notebook records? Your question banks and custom questions will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetProgress()
                        showResetProgressDialog = false
                    }
                ) {
                    Text("Reset Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text("Reset All Data") },
            text = { Text("Delete ALL data and restore the initial 10 chemistry sample questions? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAll()
                        showResetAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            title = { Text("Database Exported") },
            text = { Text("All tables and records from Room SQLite have been formatted into structured JSON and copied to your clipboard!") },
            confirmButton = {
                Button(onClick = { showExportSuccessDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (tableToPurge != null) {
        val (label, tableName) = tableToPurge!!
        AlertDialog(
            onDismissRequest = { tableToPurge = null },
            title = { Text("Purge $label Table?") },
            text = { Text("Are you sure you want to clear all rows in '$label'? Other tables will remain untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purgeTable(tableName)
                        viewModel.showToast("Purged $label records")
                        tableToPurge = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Purge")
                }
            },
            dismissButton = {
                TextButton(onClick = { tableToPurge = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
