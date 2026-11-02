package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PdfDocumentEntity
import com.example.viewmodel.ChemistryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    document: PdfDocumentEntity,
    viewModel: ChemistryViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPageIndex by remember { mutableIntStateOf(document.lastPage.coerceAtLeast(0)) }
    var totalPages by remember { mutableIntStateOf(document.pageCount.coerceAtLeast(1)) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Bookmarks set
    val bookmarks = remember(document.bookmarksJson) {
        val set = mutableSetOf<Int>()
        try {
            val arr = JSONArray(document.bookmarksJson)
            for (i in 0 until arr.length()) set.add(arr.getInt(i))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        set
    }

    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("${document.title} - P.${currentPageIndex + 1}") }
    var noteBody by remember { mutableStateOf("") }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    var pdfRenderer: PdfRenderer? by remember { mutableStateOf(null) }
    var pfd: ParcelFileDescriptor? by remember { mutableStateOf(null) }

    BackHandler {
        onClose()
    }

    // Function to render a specific page
    fun renderPage(pageIdx: Int) {
        val renderer = pdfRenderer ?: return
        if (pageIdx < 0 || pageIdx >= renderer.pageCount) return

        scope.launch(Dispatchers.Default) {
            try {
                val page = renderer.openPage(pageIdx)
                val width = (page.width * 1.5).toInt().coerceAtLeast(800)
                val height = (page.height * 1.5).toInt().coerceAtLeast(1000)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                withContext(Dispatchers.Main) {
                    currentBitmap?.recycle()
                    currentBitmap = bitmap
                    currentPageIndex = pageIdx
                    scale = 1f
                    offset = Offset.Zero
                    // Save last read page in Room
                    viewModel.updatePdfPage(document.id, pageIdx)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error rendering page: ${e.localizedMessage}"
                }
            }
        }
    }

    // Initialize PdfRenderer
    LaunchedEffect(document.id) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                val uri = Uri.parse(document.uriString)

                // Cache locally if needed
                val cacheDir = File(context.cacheDir, "rendered_pdfs")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val cachedFile = File(cacheDir, "doc_${document.id}.pdf")

                if (!cachedFile.exists() || cachedFile.length() == 0L) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(cachedFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val descriptor = ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pfd = descriptor
                val renderer = PdfRenderer(descriptor)
                pdfRenderer = renderer

                withContext(Dispatchers.Main) {
                    totalPages = renderer.pageCount
                    isLoading = false
                    val startPage = document.lastPage.coerceIn(0, totalPages - 1)
                    renderPage(startPage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Cannot open PDF. Ensure file still exists: ${e.localizedMessage}"
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentBitmap?.recycle()
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = {
                    Column {
                        Text(
                            text = document.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Page ${currentPageIndex + 1} of $totalPages • ${document.subject}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Reader")
                    }
                },
                actions = {
                    // Bookmark toggle
                    val isBookmarked = bookmarks.contains(currentPageIndex)
                    IconButton(
                        onClick = {
                            val updated = bookmarks.toMutableSet()
                            if (isBookmarked) updated.remove(currentPageIndex) else updated.add(currentPageIndex)
                            viewModel.updatePdfBookmarks(document.id, updated)
                            Toast.makeText(
                                context,
                                if (isBookmarked) "Bookmark removed" else "Page ${currentPageIndex + 1} bookmarked",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Page",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Bookmarks list
                    IconButton(onClick = { showBookmarksSheet = true }) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "View Bookmarks")
                    }

                    // Add note from page
                    IconButton(
                        onClick = {
                            noteTitle = "${document.title} - P.${currentPageIndex + 1}"
                            noteBody = "Study note from Page ${currentPageIndex + 1}:\n"
                            showNoteDialog = true
                        }
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = "Add Note from Page")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (currentPageIndex > 0) renderPage(currentPageIndex - 1) },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                    }

                    Slider(
                        value = currentPageIndex.toFloat(),
                        onValueChange = { renderPage(it.toInt()) },
                        valueRange = 0f..(totalPages - 1).coerceAtLeast(0).toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )

                    IconButton(
                        onClick = { if (currentPageIndex < totalPages - 1) renderPage(currentPageIndex + 1) },
                        enabled = currentPageIndex < totalPages - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF202020)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(text = errorMessage ?: "", color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onClose) {
                            Text("Back to Library")
                        }
                    }
                }
                currentBitmap != null -> {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page ${currentPageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state = transformState)
                    )
                }
            }
        }

        // Bookmarks Bottom Sheet
        if (showBookmarksSheet) {
            ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔖 Bookmarked Pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (bookmarks.isEmpty()) {
                        Text("No bookmarks yet. Tap the bookmark icon on any page to save it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        bookmarks.sorted().forEach { pageNum ->
                            ListItem(
                                headlineContent = { Text("Page ${pageNum + 1}") },
                                leadingContent = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingContent = {
                                    FilledTonalButton(
                                        onClick = {
                                            renderPage(pageNum)
                                            showBookmarksSheet = false
                                        }
                                    ) {
                                        Text("Jump")
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Note Dialog
        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                title = { Text("Add Note from Book") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = noteBody,
                            onValueChange = { noteBody = it },
                            label = { Text("Notes & Key Highlights") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveStudyNote(
                                title = noteTitle,
                                subject = document.subject,
                                body = noteBody
                            )
                            showNoteDialog = false
                            Toast.makeText(context, "Note saved to Student Workspace", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save Note")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
