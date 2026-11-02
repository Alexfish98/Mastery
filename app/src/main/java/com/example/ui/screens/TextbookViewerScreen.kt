package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.Bookmark
import com.example.data.local.Textbook
import com.example.viewmodel.IqaiViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * 2. LOCAL TEXTBOOK READER (PDF/EPUB) AND FILE PICKER
 * Opens local device documents, persists URI into Room DB, and renders pages entirely from memory.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextbookViewerScreen(
    viewModel: IqaiViewModel,
    onBackToMain: () -> Unit = {}
) {
    val context = LocalContext.current
    val textbooks by viewModel.allTextbooks.collectAsState()
    val selectedId by viewModel.selectedTextbookId.collectAsState()

    val currentTextbook = remember(selectedId, textbooks) {
        textbooks.find { it.id == selectedId }
    }

    // PDF Document Picker with persistable permissions
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Securely acquire read-only local storage URI permissions
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Some URIs (e.g. downloads) don't support persistable flags; still accessible
            }

            // Query file display name
            var fileName = "Offline Textbook"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: "Offline Textbook"
                    }
                }
            }

            // Inspect page count via PdfRenderer
            var totalPages = 1
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val renderer = PdfRenderer(pfd)
                    totalPages = renderer.pageCount
                    renderer.close()
                }
            } catch (e: Exception) {
                totalPages = 1
            }

            // Persist file URI into the 'Textbook' database table
            viewModel.registerTextbook(
                title = fileName,
                uriString = uri.toString(),
                totalPages = totalPages
            )
            Toast.makeText(context, "Loaded \"$fileName\" ($totalPages pages)", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTextbook?.title ?: "Mastery Local Textbook Reader",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (currentTextbook != null) {
                            Text(
                                text = "Page ${currentTextbook.lastPageRead + 1} of ${currentTextbook.totalPages}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedId != null) {
                            viewModel.selectTextbook(null)
                        } else {
                            onBackToMain()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            openDocLauncher.launch(arrayOf("application/pdf"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import PDF", fontSize = 13.sp)
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
            if (currentTextbook == null) {
                // Textbook Library List
                TextbookLibraryList(
                    textbooks = textbooks,
                    onSelect = { viewModel.selectTextbook(it.id) },
                    onDelete = { viewModel.deleteTextbook(it.id) },
                    onGenerateSample = {
                        val sampleUri = createSamplePdf(context)
                        viewModel.registerTextbook(
                            title = "EUEE Natural Science Summary (Sample)",
                            uriString = sampleUri.toString(),
                            totalPages = 4
                        )
                    }
                )
            } else {
                // Active In-Memory PDF Page Renderer
                ActivePdfViewer(
                    textbook = currentTextbook,
                    viewModel = viewModel,
                    onClose = { viewModel.selectTextbook(null) }
                )
            }
        }
    }
}

@Composable
fun TextbookLibraryList(
    textbooks: List<Textbook>,
    onSelect: (Textbook) -> Unit,
    onDelete: (Textbook) -> Unit,
    onGenerateSample: () -> Unit
) {
    if (textbooks.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("No Textbooks Loaded Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Import local PDF curriculum books and study guides securely into your local Room database without internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onGenerateSample,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate Offline Starter Textbook")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(textbooks, key = { it.id }) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(book) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            val pct = if (book.totalPages > 0) (book.lastPageRead * 100 / book.totalPages) else 0
                            Text("Page ${book.lastPageRead + 1} of ${book.totalPages} • $pct% completed", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            LinearProgressIndicator(
                                progress = { if (book.totalPages > 0) (book.lastPageRead + 1).toFloat() / book.totalPages.toFloat() else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }

                        IconButton(onClick = { onDelete(book) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight local canvas/view layout rendering PDF pages smoothly entirely from device memory
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivePdfViewer(
    textbook: Textbook,
    viewModel: IqaiViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember(textbook.id) { mutableIntStateOf(textbook.lastPageRead) }
    var totalPages by remember(textbook.id) { mutableIntStateOf(maxOf(1, textbook.totalPages)) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNote by remember { mutableStateOf("") }
    var showBookmarksSheet by remember { mutableStateOf(false) }

    val bookmarks by viewModel.getBookmarks(textbook.id).collectAsState(initial = emptyList())

    // Render page into memory bitmap
    LaunchedEffect(textbook.localUri, currentPage) {
        try {
            val uri = Uri.parse(textbook.localUri)
            val pfd: ParcelFileDescriptor? = if (uri.scheme == "file") {
                ParcelFileDescriptor.open(File(uri.path ?: ""), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            pfd?.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                totalPages = renderer.pageCount
                val pageIndex = currentPage.coerceIn(0, totalPages - 1)
                val page = renderer.openPage(pageIndex)

                val bitmap = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()

                currentBitmap = bitmap

                // Persist progress to Room DB
                viewModel.updateTextbookProgress(textbook.id, pageIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
    ) {
        // Top Toolbar: Page indicators and Bookmarks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (currentPage > 0) currentPage -= 1
                    },
                    enabled = currentPage > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                }

                Text(
                    text = "${currentPage + 1} / $totalPages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                IconButton(
                    onClick = {
                        if (currentPage < totalPages - 1) currentPage += 1
                    },
                    enabled = currentPage < totalPages - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showBookmarkDialog = true }) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Bookmark this page")
                }
                IconButton(onClick = { showBookmarksSheet = true }) {
                    Icon(Icons.Default.Bookmarks, contentDescription = "View bookmarks")
                }
            }
        }

        // Memory Canvas / AndroidView rendering PDF Bitmap
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentBitmap != null) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageBitmap(currentBitmap)
                        }
                    },
                    update = { view ->
                        view.setImageBitmap(currentBitmap)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Bookmark Dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Bookmark Page ${currentPage + 1}") },
            text = {
                OutlinedTextField(
                    value = bookmarkNote,
                    onValueChange = { bookmarkNote = it },
                    label = { Text("Bookmark Note (Optional)") },
                    placeholder = { Text("e.g. Important derivation, exam trap") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addBookmark(textbook.id, currentPage + 1, bookmarkNote.trim())
                    Toast.makeText(context, "Saved Bookmark!", Toast.LENGTH_SHORT).show()
                    bookmarkNote = ""
                    showBookmarkDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bookmarks Drawer/Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Bookmarks for ${textbook.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (bookmarks.isEmpty()) {
                    Text("No bookmarks added yet for this textbook.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bookmarks, key = { it.id }) { b ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPage = b.pageNumber - 1
                                        showBookmarksSheet = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Page ${b.pageNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        if (b.note.isNotBlank()) {
                                            Text(b.note, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteBookmark(b.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Creates an offline sample PDF document in application cache if user tests without selecting an external PDF
 */
private fun createSamplePdf(context: Context): Uri {
    val file = File(context.cacheDir, "euee_sample_textbook.pdf")
    if (file.exists()) return Uri.fromFile(file)

    val doc = android.graphics.pdf.PdfDocument()
    val titles = listOf(
        "Page 1: EUEE Chemistry Fundamentals",
        "Page 2: Le Chatelier's Principle & Equilibrium",
        "Page 3: Henderson-Hasselbalch Buffer Derivations",
        "Page 4: High-Yield Practice Formula Sheet"
    )

    titles.forEachIndexed { index, title ->
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, index + 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 22f
            isAntiAlias = true
        }

        canvas.drawText(title, 40f, 60f, paint)

        paint.textSize = 14f
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("Ethiopian University Entrance Exam Mastery Suite (Offline Mode)", 40f, 100f, paint)
        canvas.drawText("100% On-Device Local Memory Rendering via PdfRenderer", 40f, 130f, paint)
        canvas.drawText("• Fully persists reading progress and bookmarks in Room database.", 40f, 180f, paint)
        canvas.drawText("• Zoom and flip smoothly across curriculum pages.", 40f, 210f, paint)

        doc.finishPage(page)
    }

    FileOutputStream(file).use { out ->
        doc.writeTo(out)
    }
    doc.close()

    return Uri.fromFile(file)
}
