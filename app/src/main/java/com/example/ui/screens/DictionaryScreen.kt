package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.domain.model.DictionaryTerm
import com.example.util.SandboxStorageHelper
import com.example.viewmodel.IqaiViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    viewModel: IqaiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allTerms by viewModel.allDictionaryTerms.collectAsState()
    val searchQuery by viewModel.dictionaryQuery.collectAsState()
    val selectedSubject by viewModel.selectedDictionarySubject.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var termToEdit by remember { mutableStateOf<DictionaryTerm?>(null) }
    var importResultMessage by remember { mutableStateOf<String?>(null) }

    val subjects = listOf("All", "English", "Chemistry", "Physics", "Biology", "Mathematics")

    val filteredTerms = remember(allTerms, searchQuery, selectedSubject) {
        allTerms.filter { item ->
            val matchSubject = selectedSubject == "All" || item.subject.equals(selectedSubject, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                item.term.contains(searchQuery, ignoreCase = true) ||
                item.definition.contains(searchQuery, ignoreCase = true) ||
                item.synonyms.any { it.contains(searchQuery, ignoreCase = true) }
            matchSubject && matchQuery
        }.sortedBy { it.term.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = {
                    Column {
                        Text(
                            text = "Offline Syllabus Dictionary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allTerms.size} terms across 5 subjects",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("import_json_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import JSON Data")
                    }
                    IconButton(
                        onClick = {
                            termToEdit = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_term_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Term")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    termToEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_term")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Term")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar & Filter
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setDictionaryQuery(it) },
                        placeholder = { Text("Search terms, definitions, synonyms...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setDictionaryQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Subject Chips
                    ScrollableTabRow(
                        selectedTabIndex = subjects.indexOf(selectedSubject).coerceAtLeast(0),
                        edgePadding = 0.dp,
                        divider = {},
                        containerColor = Color.Transparent
                    ) {
                        subjects.forEach { s ->
                            val count = if (s == "All") allTerms.size else allTerms.count { it.subject.equals(s, ignoreCase = true) }
                            Tab(
                                selected = selectedSubject == s,
                                onClick = { viewModel.setDictionarySubject(s) },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
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
                }
            }

            // Terms List
            if (filteredTerms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No terms match '$searchQuery'" else "No terms in category '$selectedSubject'",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Import JSON dataset or tap '+ Add Term' to populate your offline repository.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        FilledTonalButton(onClick = { showImportDialog = true }) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import JSON Data")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTerms, key = { it.id }) { term ->
                        DictionaryTermCard(
                            term = term,
                            onEdit = {
                                termToEdit = term
                                showAddDialog = true
                            },
                            onDelete = {
                                viewModel.deleteDictionaryTerm(term.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dynamic JSON Import Dialog
    if (showImportDialog) {
        DynamicJsonImportDialog(
            onDismiss = { showImportDialog = false },
            onImportJson = { rawJson ->
                scope.launch {
                    val count = viewModel.importDictionaryJson(rawJson)
                    showImportDialog = false
                    importResultMessage = "Successfully imported and categorized $count terms!"
                }
            },
            onImportStream = { uri, onDone ->
                scope.launch {
                    try {
                        val stream = context.contentResolver.openInputStream(uri)
                        val count = if (stream != null) {
                            viewModel.importDictionaryFromStream(stream)
                        } else 0
                        onDone(count)
                        showImportDialog = false
                        importResultMessage = "Successfully streamed and categorized $count terms into Room SQLite!"
                    } catch (e: Exception) {
                        onDone(0)
                        importResultMessage = "Import failed: ${e.message}"
                    }
                }
            }
        )
    }

    // Add / Edit Term Dialog
    if (showAddDialog) {
        DictionaryTermDialog(
            initialTerm = termToEdit,
            onDismiss = {
                showAddDialog = false
                termToEdit = null
            },
            onSave = { saved ->
                viewModel.saveDictionaryTerm(saved)
                showAddDialog = false
                termToEdit = null
            }
        )
    }

    // Success SnackBar
    importResultMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3500)
            importResultMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                action = {
                    TextButton(onClick = { importResultMessage = null }) {
                        Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
}

@Composable
fun DictionaryTermCard(
    term: DictionaryTerm,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subjectColor = when (term.subject.lowercase()) {
        "chemistry" -> Color(0xFF0D9488)
        "physics" -> Color(0xFF2563EB)
        "biology" -> Color(0xFF16A34A)
        "mathematics", "math" -> Color(0xFFD97706)
        "english" -> Color(0xFF9333EA)
        else -> MaterialTheme.colorScheme.primary
    }

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = term.term,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (term.phonetic.isNotBlank()) {
                        Text(
                            text = term.phonetic,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = subjectColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = term.subject,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Text(
                text = term.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (term.exampleUsage.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${term.exampleUsage}”",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (term.synonyms.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Related:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    term.synonyms.take(4).forEach { syn ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(syn, fontSize = 10.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dynamic JSON Importer: Accepts local document via streaming without code dump, or optional manual paste
 */
@Composable
fun DynamicJsonImportDialog(
    onDismiss: () -> Unit,
    onImportJson: (String) -> Unit,
    onImportStream: ((Uri, onDone: (Int) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var pastedJson by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableLongStateOf(0L) }
    var showManualPaste by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = SandboxStorageHelper.getFileName(context, uri) ?: "dictionary.json"
            pastedJson = ""
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    selectedFileSize = pfd.statSize
                }
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Spellcheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Dictionary Data Ingestion", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Upload structured JSON dictionary data. Files are parsed via background streams directly into Room SQLite without taking up screen space.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { filePicker.launch("*/*") },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedFileName == null) "Pick JSON File" else "Change File")
                    }

                    OutlinedButton(
                        onClick = { showManualPaste = !showManualPaste },
                        enabled = !isProcessing
                    ) {
                        Icon(if (showManualPaste) Icons.Default.ExpandLess else Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

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
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedFileName ?: "Dictionary File", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                val sizeKb = (selectedFileSize / 1024.0).toInt()
                                Text(
                                    if (selectedFileSize > 0) "$sizeKb KB • Ready for streaming" else "Ready to import",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (isProcessing) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Streaming dictionary terms into Room SQLite...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (showManualPaste) {
                    Text("Manual JSON String (optional):", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = pastedJson,
                        onValueChange = { pastedJson = it },
                        placeholder = {
                            Text(
                                """[{"word": "Catalyst", "meaning": "Substance that accelerates...", "subject": "Chemistry"}]"""
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedFileUri != null && onImportStream != null) {
                        isProcessing = true
                        onImportStream(selectedFileUri!!) {
                            isProcessing = false
                            onDismiss()
                        }
                    } else if (pastedJson.isNotBlank()) {
                        onImportJson(pastedJson)
                    }
                },
                enabled = !isProcessing && (selectedFileUri != null || pastedJson.isNotBlank())
            ) {
                Text(if (isProcessing) "Streaming..." else "Process & Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DictionaryTermDialog(
    initialTerm: DictionaryTerm?,
    onDismiss: () -> Unit,
    onSave: (DictionaryTerm) -> Unit
) {
    var term by remember { mutableStateOf(initialTerm?.term ?: "") }
    var definition by remember { mutableStateOf(initialTerm?.definition ?: "") }
    var subject by remember { mutableStateOf(initialTerm?.subject ?: "Chemistry") }
    var partOfSpeech by remember { mutableStateOf(initialTerm?.partOfSpeech ?: "noun") }
    var phonetic by remember { mutableStateOf(initialTerm?.phonetic ?: "") }
    var exampleUsage by remember { mutableStateOf(initialTerm?.exampleUsage ?: "") }
    var synonymsText by remember { mutableStateOf(initialTerm?.synonyms?.joinToString(", ") ?: "") }

    val subjects = listOf("English", "Chemistry", "Physics", "Biology", "Mathematics")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialTerm == null) "Add Dictionary Term" else "Edit Term", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("Term / Word") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Subject Classification:", style = MaterialTheme.typography.labelMedium)
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

                OutlinedTextField(
                    value = definition,
                    onValueChange = { definition = it },
                    label = { Text("Definition / Meaning") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = partOfSpeech,
                        onValueChange = { partOfSpeech = it },
                        label = { Text("Part of Speech") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phonetic,
                        onValueChange = { phonetic = it },
                        label = { Text("Phonetic / IPA") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = exampleUsage,
                    onValueChange = { exampleUsage = it },
                    label = { Text("Example Usage / Context") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = synonymsText,
                    onValueChange = { synonymsText = it },
                    label = { Text("Synonyms / Related (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val syns = synonymsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val item = DictionaryTerm(
                        id = initialTerm?.id ?: 0,
                        term = term.trim(),
                        definition = definition.trim(),
                        subject = subject,
                        partOfSpeech = partOfSpeech.trim(),
                        phonetic = phonetic.trim(),
                        exampleUsage = exampleUsage.trim(),
                        synonyms = syns
                    )
                    onSave(item)
                },
                enabled = term.isNotBlank() && definition.isNotBlank()
            ) {
                Text("Save Term")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
