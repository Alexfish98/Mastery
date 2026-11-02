package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfflineNote
import com.example.viewmodel.IqaiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 4. WIKI-STYLE MARKDOWN NOTES WITH [[TargetNoteTitle]] PARSING & FTS4 SEARCH
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiNotesScreen(
    viewModel: IqaiViewModel,
    onBack: () -> Unit = {}
) {
    val notes by viewModel.filteredNotes.collectAsState()
    val activeNoteId by viewModel.activeNoteId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val currentNote = remember(activeNoteId, notes) {
        notes.find { it.id == activeNoteId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentNote != null) currentNote.title else "Wiki Notes (FTS4)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentNote != null) {
                            viewModel.openNote(null)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentNote == null) {
                        IconButton(onClick = {
                            viewModel.saveNote(0, "Untitled Note", "# New Note\n\nLink to another note using [[TargetNoteTitle]].")
                        }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "Create Note")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentNote == null) {
                // Notes List with FTS Search
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Instant Offline FTS4 Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search notes via instant Room FTS4...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    if (notes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No notes yet. Tap + to start authoring!" else "No notes match \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    onClick = { viewModel.openNote(note.id) },
                                    onDelete = { viewModel.deleteNote(note.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Note Editor & Markdown Viewer with Wiki-Links
                NoteDetailEditor(
                    note = currentNote,
                    onSave = { title, content -> viewModel.saveNote(currentNote.id, title, content) },
                    onWikiLinkClicked = { targetTitle -> viewModel.openNoteByTitle(targetTitle) },
                    onDelete = { viewModel.deleteNote(currentNote.id) }
                )
            }
        }
    }
}

@Composable
fun NoteCardItem(
    note: OfflineNote,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.lastModified))
    val wikiLinks = remember(note.content) {
        Regex("\\[\\[([^\\]]+)\\]\\]").findAll(note.content).map { it.groupValues[1] }.toList()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Text(
                text = note.content.take(120).replace("\n", " ") + "...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (wikiLinks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔗", fontSize = 11.sp)
                    wikiLinks.take(3).forEach { link ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = link,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun NoteDetailEditor(
    note: OfflineNote,
    onSave: (String, String) -> Unit,
    onWikiLinkClicked: (String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var isEditMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isEditMode,
                    onClick = {
                        if (isEditMode) onSave(title, content)
                        isEditMode = false
                    },
                    label = { Text("Preview & Links") },
                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                )
                FilterChip(
                    selected = isEditMode,
                    onClick = { isEditMode = true },
                    label = { Text("Edit Markdown") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        if (isEditMode) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    onSave(title, content)
                },
                label = { Text("Note Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    onSave(title, content)
                },
                label = { Text("Content (Markdown + [[WikiLinks]])") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HorizontalDivider()

                    // Markdown rendering with clickable [[WikiLinks]]
                    val parsedAnnotatedString = parseMarkdownWithWikiLinks(content)

                    ClickableText(
                        text = parsedAnnotatedString,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            parsedAnnotatedString.getStringAnnotations(tag = "WIKI_LINK", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    onWikiLinkClicked(annotation.item)
                                }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Markdown text parser in Kotlin that scans OfflineNote.content using regex
 * to identify wiki-links formatted as [[TargetNoteTitle]].
 */
fun parseMarkdownWithWikiLinks(content: String): AnnotatedString {
    val wikiRegex = Regex("\\[\\[([^\\]]+)\\]\\]")

    return buildAnnotatedString {
        val lines = content.lines()
        for (line in lines) {
            when {
                line.startsWith("# ") -> {
                    withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                        appendLine(line.removePrefix("# "))
                    }
                }
                line.startsWith("## ") -> {
                    withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)) {
                        appendLine(line.removePrefix("## "))
                    }
                }
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)) {
                        appendLine(line.removePrefix("### "))
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    append("• ")
                    appendParsedInline(line.substring(2), wikiRegex)
                    appendLine()
                }
                line.startsWith("> ") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF64748B))) {
                        append("▎ ")
                        appendParsedInline(line.removePrefix("> "), wikiRegex)
                        appendLine()
                    }
                }
                else -> {
                    appendParsedInline(line, wikiRegex)
                    appendLine()
                }
            }
        }
    }
}

/**
 * Appends inline markdown (bold **text**, italic *text*, code `code`, and [[WikiLink]])
 */
private fun AnnotatedString.Builder.appendParsedInline(text: String, wikiRegex: Regex) {
    var lastIndex = 0

    // Find all [[WikiLinks]]
    val matches = wikiRegex.findAll(text).toList()

    for (match in matches) {
        val pre = text.substring(lastIndex, match.range.first)
        appendSimpleMarkdown(pre)

        val targetTitle = match.groupValues[1]

        // Push clickable wiki link annotation
        pushStringAnnotation(tag = "WIKI_LINK", annotation = targetTitle)
        withStyle(
            SpanStyle(
                color = Color(0xFF6366F1),
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("[[$targetTitle]]")
        }
        pop()

        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        appendSimpleMarkdown(text.substring(lastIndex))
    }
}

private fun AnnotatedString.Builder.appendSimpleMarkdown(text: String) {
    // Quick handle for bold **text** and `code`
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val endBold = text.indexOf("**", i + 2)
            if (endBold != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(i + 2, endBold))
                }
                i = endBold + 2
                continue
            }
        }
        if (text[i] == '`') {
            val endCode = text.indexOf('`', i + 1)
            if (endCode != -1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x226366F1))) {
                    append(text.substring(i + 1, endCode))
                }
                i = endCode + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
