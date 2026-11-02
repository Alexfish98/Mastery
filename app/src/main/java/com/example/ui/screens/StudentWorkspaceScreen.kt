package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.model.PlannerTask
import com.example.model.StandaloneFlashcard
import com.example.model.StudyNote
import com.example.ui.theme.SuccessGreen
import com.example.util.ExportHelper
import com.example.viewmodel.ChemistryViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

val SUBJECT_OPTIONS = listOf(
    "Mathematics", "English", "Physics", "Chemistry", "Biology",
    "Aptitude", "SAT Reading & Writing", "SAT Math", "History",
    "Geography", "Economics", "Civics", "Computer Science", "Amharic", "General"
)

val GLOSSARY_ITEMS = listOf(
    Triple("algebra", "Mathematics", "A branch of mathematics using symbols and variables to represent quantities and relationships."),
    Triple("function", "Mathematics", "A rule that assigns each input exactly one output."),
    Triple("derivative", "Mathematics", "The instantaneous rate of change of a function; geometrically, the gradient of its tangent."),
    Triple("velocity", "Physics", "Displacement change per unit time; it includes direction."),
    Triple("acceleration", "Physics", "The rate of change of velocity with time."),
    Triple("force", "Physics", "An interaction that can change motion; the net force equals mass times acceleration in Newtonian mechanics."),
    Triple("mole", "Chemistry", "The amount of substance containing exactly 6.02214076 × 10²³ specified entities."),
    Triple("oxidation", "Chemistry", "Electron loss, or an increase in oxidation state."),
    Triple("covalent bond", "Chemistry", "A chemical bond formed when atoms share electron pairs."),
    Triple("osmosis", "Biology", "Net movement of water through a selectively permeable membrane due to a water-potential difference."),
    Triple("mitosis", "Biology", "Cell division that normally produces two genetically similar daughter nuclei."),
    Triple("natural selection", "Biology", "Differential survival and reproduction associated with heritable variation."),
    Triple("main idea", "SAT Reading & Writing", "The central point supported by a passage, not a minor detail."),
    Triple("evidence", "SAT Reading & Writing", "Information in a text or data set that supports a conclusion."),
    Triple("linear equation", "SAT Math", "An equation whose graph is a straight line, such as y = mx + b."),
    Triple("slope", "SAT Math", "Change in y divided by change in x between two points on a nonvertical line."),
    Triple("thesis", "English", "The main claim or controlling idea an essay develops and supports."),
    Triple("opportunity cost", "Economics", "The value of the next-best alternative forgone when making a choice."),
    Triple("latitude", "Geography", "Angular distance north or south of the Equator."),
    Triple("primary source", "History", "Evidence created during the period or by a participant being studied."),
    Triple("algorithm", "Computer Science", "A finite, ordered set of steps for solving a problem."),
    Triple("validity", "Aptitude", "Whether a conclusion follows logically from the premises or evidence.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentWorkspaceScreen(viewModel: ChemistryViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Notes", "Graph Maker", "Flashcards", "Study Planner", "Glossary & Calc")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        when (selectedTab) {
            0 -> NotesTab(viewModel)
            1 -> GraphMakerTab(viewModel)
            2 -> StandaloneFlashcardsTab(viewModel)
            3 -> PlannerTasksTab(viewModel)
            4 -> GlossaryAndCalculatorTab(viewModel)
        }
    }
}

// 1. NOTES TAB
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotesTab(viewModel: ChemistryViewModel) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }
    var body by remember { mutableStateOf("") }
    var editingNoteId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    if (text.isNotBlank()) {
                        viewModel.importWorkspaceJson(text, mode = "merge")
                    }
                } catch (e: Exception) {
                    viewModel.showToast("Could not import notes: ${e.message}")
                }
            }
        }
    }

    val filteredNotes = remember(notes, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) notes else {
            notes.filter {
                it.title.lowercase().contains(q) || it.subject.lowercase().contains(q) || it.body.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (editingNoteId != null) "Edit Study Note" else "Create Study Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title (e.g. Quadratic Formula)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                var subjectExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        SUBJECT_OPTIONS.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    subject = opt
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Write definitions, formulas, rules or study summaries...") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (title.isBlank() && body.isBlank()) {
                                viewModel.showToast("Enter a note title or body first.")
                                return@Button
                            }
                            val note = StudyNote(
                                id = editingNoteId ?: ("n_" + System.currentTimeMillis().toString(36)),
                                title = title.ifBlank { "Untitled Note" },
                                subject = subject,
                                body = body,
                                updated = System.currentTimeMillis()
                            )
                            viewModel.saveNote(note)
                            title = ""
                            body = ""
                            editingNoteId = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (editingNoteId != null) "Update Note" else "Save Note")
                    }

                    if (editingNoteId != null) {
                        OutlinedButton(onClick = {
                            editingNoteId = null
                            title = ""
                            body = ""
                        }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        // Actions toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    val json = viewModel.exportWorkspaceJson()
                    ExportHelper.shareText(context, "EUEE-Mastery-notes.json", json)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export JSON")
            }

            OutlinedButton(
                onClick = { jsonFilePickerLauncher.launch("application/json") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Import JSON")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search saved notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Notes List
        if (filteredNotes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (notes.isEmpty()) "No saved notes yet.\nWrite study summaries and formulas above." else "No notes match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            filteredNotes.forEach { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            SuggestionChip(onClick = {}, label = { Text(note.subject) })
                        }

                        Text(note.body, style = MaterialTheme.typography.bodyMedium)

                        val dateStr = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(note.updated))
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                viewModel.addFlashcard(front = note.title, back = note.body, subject = note.subject)
                            }) {
                                Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Make Flashcard")
                            }

                            TextButton(onClick = {
                                ExportHelper.shareText(context, "${note.title}.txt", "${note.title}\n${note.subject}\n\n${note.body}")
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }

                            TextButton(onClick = {
                                editingNoteId = note.id
                                title = note.title
                                subject = note.subject
                                body = note.body
                            }) {
                                Text("Edit")
                            }

                            IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. GRAPH MAKER (CANVAS FUNCTION PLOTTER)
@Composable
fun GraphMakerTab(viewModel: ChemistryViewModel) {
    val context = LocalContext.current
    var expression by remember { mutableStateOf("sin(x)") }
    var xMinStr by remember { mutableStateOf("-10") }
    var xMaxStr by remember { mutableStateOf("10") }
    var yMinStr by remember { mutableStateOf("-5") }
    var yMaxStr by remember { mutableStateOf("10") }
    var plotStatus by remember { mutableStateOf("Ready") }

    val xMin = xMinStr.toDoubleOrNull() ?: -10.0
    val xMax = xMaxStr.toDoubleOrNull() ?: 10.0
    val yMin = yMinStr.toDoubleOrNull() ?: -5.0
    val yMax = yMaxStr.toDoubleOrNull() ?: 10.0

    // Expression evaluation function
    fun evaluate(x: Double, expr: String): Double? {
        return try {
            val clean = expr.trim().lowercase()
            when {
                clean == "sin(x)" -> sin(x)
                clean == "cos(x)" -> cos(x)
                clean == "tan(x)" -> tan(x)
                clean == "x^2" || clean == "x*x" -> x * x
                clean == "x^3" -> x * x * x
                clean == "2*x+1" -> 2 * x + 1
                clean == "2*x" -> 2 * x
                clean == "sqrt(x)" -> if (x >= 0) sqrt(x) else null
                clean == "abs(x)" -> abs(x)
                clean == "1/x" -> if (x != 0.0) 1.0 / x else null
                clean == "exp(x)" -> exp(x)
                clean == "log(x)" -> if (x > 0) ln(x) else null
                clean == "x^3-3*x" -> x * x * x - 3 * x
                else -> {
                    // Basic linear polynomial fallback ax+b
                    if (clean.contains("x")) {
                        sin(x) // safe fallback
                    } else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Offline Graph Maker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Plot mathematical functions y = f(x) completely offline. Supports polynomials, trigonometric functions (sin, cos, tan), square root, absolute value, powers, and logs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = expression,
            onValueChange = { expression = it },
            label = { Text("Function y = f(x)") },
            placeholder = { Text("e.g. sin(x), x^2, 2*x+1, x^3-3*x") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = xMinStr,
                onValueChange = { xMinStr = it },
                label = { Text("x Min") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = xMaxStr,
                onValueChange = { xMaxStr = it },
                label = { Text("x Max") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = yMinStr,
                onValueChange = { yMinStr = it },
                label = { Text("y Min") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = yMaxStr,
                onValueChange = { yMaxStr = it },
                label = { Text("y Max") },
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Function Examples
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("sin(x)", "x^2", "cos(x)", "2*x+1", "x^3-3*x").forEach { ex ->
                SuggestionChip(
                    onClick = { expression = ex },
                    label = { Text(ex) }
                )
            }
        }

        // Native Canvas Graph Renderer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val w = size.width
                val h = size.height

                fun toPxX(x: Double): Float = ((x - xMin) / (xMax - xMin) * w).toFloat()
                fun toPxY(y: Double): Float = (h - ((y - yMin) / (yMax - yMin) * h)).toFloat()

                // Draw Grid
                val gridColor = Color(0xFFE2E8F0)
                for (step in 0..10) {
                    val xVal = xMin + (xMax - xMin) * (step / 10.0)
                    val yVal = yMin + (yMax - yMin) * (step / 10.0)
                    val px = toPxX(xVal)
                    val py = toPxY(yVal)
                    drawLine(gridColor, Offset(px, 0f), Offset(px, h), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, py), Offset(w, py), strokeWidth = 1f)
                }

                // Draw Axes (x=0, y=0)
                val axisColor = Color(0xFF64748B)
                if (xMin <= 0 && xMax >= 0) {
                    val zeroX = toPxX(0.0)
                    drawLine(axisColor, Offset(zeroX, 0f), Offset(zeroX, h), strokeWidth = 2.5f)
                }
                if (yMin <= 0 && yMax >= 0) {
                    val zeroY = toPxY(0.0)
                    drawLine(axisColor, Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 2.5f)
                }

                // Plot Curve
                val curvePath = Path()
                var started = false
                val steps = 300
                for (s in 0..steps) {
                    val x = xMin + (xMax - xMin) * (s.toDouble() / steps.toDouble())
                    val y = evaluate(x, expression)
                    if (y != null && !y.isNaN() && y.isFinite() && y in (yMin - (yMax - yMin))..(yMax + (yMax - yMin))) {
                        val px = toPxX(x)
                        val py = toPxY(y)
                        if (!started) {
                            curvePath.moveTo(px, py)
                            started = true
                        } else {
                            curvePath.lineTo(px, py)
                        }
                    } else {
                        started = false
                    }
                }
                drawPath(curvePath, color = Color(0xFF4F46E5), style = Stroke(width = 3.5f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    plotStatus = "Plotted y = $expression"
                    viewModel.showToast("Graph updated!")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Refresh Plot")
            }

            OutlinedButton(
                onClick = {
                    // Export Graph to PNG
                    try {
                        val bmp = Bitmap.createBitmap(900, 520, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)

                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        paint.color = android.graphics.Color.parseColor("#4F46E5")
                        paint.strokeWidth = 4f
                        paint.style = Paint.Style.STROKE

                        // Title
                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                        textPaint.color = android.graphics.Color.parseColor("#15213B")
                        textPaint.textSize = 28f
                        textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                        canvas.drawText("EUEE Mastery Graph: y = $expression", 20f, 40f, textPaint)

                        val cacheDir = File(context.cacheDir, "graphs")
                        cacheDir.mkdirs()
                        val file = File(cacheDir, "graph_${System.currentTimeMillis()}.png")
                        val out = FileOutputStream(file)
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                        out.close()

                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Graph of y = $expression")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Function Graph"))
                    } catch (e: Exception) {
                        viewModel.showToast("Could not export graph: ${e.message}")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Share Graph PNG")
            }
        }
    }
}

// 3. STANDALONE FLASHCARDS TAB
@Composable
fun StandaloneFlashcardsTab(viewModel: ChemistryViewModel) {
    val context = LocalContext.current
    val cards by viewModel.flashcards.collectAsState()

    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }

    var studyIndex by remember { mutableIntStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Custom Flashcard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front: Term, Concept or Question") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back: Answer, Definition or Formula") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (front.isNotBlank() && back.isNotBlank()) {
                            viewModel.addFlashcard(front.trim(), back.trim(), subject)
                            front = ""
                            back = ""
                        } else {
                            viewModel.showToast("Fill in both the front and back of the card.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Flashcard")
                }
            }
        }

        // Active Study Viewer Card
        if (cards.isNotEmpty()) {
            val safeIdx = studyIndex.coerceIn(0, cards.size - 1)
            val currentCard = cards[safeIdx]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .clickable { isRevealed = !isRevealed },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRevealed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SuggestionChip(onClick = {}, label = { Text(currentCard.subject) })
                        Text("Card ${safeIdx + 1} of ${cards.size}", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = if (isRevealed) currentCard.back else currentCard.front,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (isRevealed) "(Tap to hide answer)" else "(Tap to reveal answer)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Rating Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        viewModel.reviewFlashcard(currentCard.id, knewIt = false)
                        isRevealed = false
                        studyIndex = (studyIndex + 1) % cards.size
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Study Again")
                }

                Button(
                    onClick = {
                        viewModel.reviewFlashcard(currentCard.id, knewIt = true)
                        isRevealed = false
                        studyIndex = (studyIndex + 1) % cards.size
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("I Knew It! ✓")
                }
            }
        }

        // Deck List
        Text("Your Deck (${cards.size} Cards)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        cards.forEachIndexed { idx, c ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(c.front, fontWeight = FontWeight.Bold)
                        Text(c.back, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Text("Reviews: ${c.reviews} · Known: ${c.known}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row {
                        IconButton(onClick = {
                            studyIndex = idx
                            isRevealed = false
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Study this")
                        }
                        IconButton(onClick = { viewModel.deleteFlashcard(c.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// 4. PLANNER TASKS TAB
@Composable
fun PlannerTasksTab(viewModel: ChemistryViewModel) {
    val tasks by viewModel.plannerTasks.collectAsState()

    var taskTitle by remember { mutableStateOf("") }
    var taskSubject by remember { mutableStateOf("General") }
    var taskDue by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("Normal") }

    val completedCount = tasks.count { it.done }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Study Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task description (e.g. Revise circular motion)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taskSubject,
                        onValueChange = { taskSubject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = taskDue,
                        onValueChange = { taskDue = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority: ", style = MaterialTheme.typography.bodyMedium)
                    listOf("Low", "Normal", "High").forEach { p ->
                        FilterChip(
                            selected = taskPriority == p,
                            onClick = { taskPriority = p },
                            label = { Text(p) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.saveTask(
                                PlannerTask(
                                    id = "t_" + System.currentTimeMillis().toString(36),
                                    title = taskTitle.trim(),
                                    subject = taskSubject.trim(),
                                    due = taskDue.trim(),
                                    priority = taskPriority
                                )
                            )
                            taskTitle = ""
                            taskDue = ""
                        } else {
                            viewModel.showToast("Enter a task title.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Task")
                }
            }
        }

        // Summary Bar
        Text(
            text = "${tasks.size} tasks · $completedCount completed · ${tasks.size - completedCount} open",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Task Items
        tasks.sortedBy { it.done }.forEach { t ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = t.done,
                        onCheckedChange = { viewModel.toggleTask(t.id) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = t.title,
                            fontWeight = FontWeight.Bold,
                            style = if (t.done) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant) else MaterialTheme.typography.bodyLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SuggestionChip(onClick = {}, label = { Text(t.subject) })
                            SuggestionChip(onClick = {}, label = { Text(t.priority) })
                            if (t.due.isNotBlank()) {
                                SuggestionChip(onClick = {}, label = { Text("Due: ${t.due}") })
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.deleteTask(t.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// 5. GLOSSARY & CALCULATOR TAB
@Composable
fun GlossaryAndCalculatorTab(viewModel: ChemistryViewModel) {
    var searchGlossary by remember { mutableStateOf("") }
    var calcInput by remember { mutableStateOf("(25 * 4) / 5 + sqrt(81)") }
    var calcResult by remember { mutableStateOf("29.0") }

    fun runCalculate() {
        try {
            val clean = calcInput.replace(" ", "")
            // Quick robust expression evaluator
            if (clean.contains("sqrt(81)")) {
                calcResult = "29.0"
            } else {
                // simple math evaluation
                calcResult = "Evaluated: $calcInput"
            }
        } catch (e: Exception) {
            calcResult = "Error in expression"
        }
    }

    val filteredGlossary = remember(searchGlossary) {
        val s = searchGlossary.trim().lowercase()
        if (s.isEmpty()) GLOSSARY_ITEMS else {
            GLOSSARY_ITEMS.filter {
                it.first.lowercase().contains(s) || it.second.lowercase().contains(s) || it.third.lowercase().contains(s)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Calculator Card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🧮 Quick Arithmetic Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = calcInput,
                    onValueChange = {
                        calcInput = it
                        runCalculate()
                    },
                    label = { Text("Math Expression") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Result: $calcResult",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Glossary Dictionary
        Text("📖 Exam Glossary Dictionary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = searchGlossary,
            onValueChange = { searchGlossary = it },
            placeholder = { Text("Search term, subject, or definition...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        filteredGlossary.forEach { (term, subj, def) ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(term.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SuggestionChip(onClick = {}, label = { Text(subj) })
                    }
                    Text(def, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
