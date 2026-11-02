package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.ChemistryMasteryTheme
import com.example.viewmodel.ChemistryViewModel
import com.example.viewmodel.FocusViewModel
import com.example.viewmodel.IqaiViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChemistryViewModel = viewModel()
            val prefs by viewModel.prefs.collectAsState()
            val activeSession by viewModel.activeSession.collectAsState()
            val toastMessage by viewModel.toast.collectAsState()
            val context = LocalContext.current
            var showSplash by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(toastMessage) {
                toastMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            ChemistryMasteryTheme(
                themeMode = prefs.theme,
                accentColor = prefs.accent,
                fontSelection = prefs.selectedFont
            ) {
                if (showSplash) {
                    SplashScreen(
                        onDismiss = { showSplash = false }
                    )
                } else if (activeSession != null) {
                    BackHandler {
                        viewModel.exitStudySession()
                    }
                    StudyWorkspaceScreen(
                        session = activeSession!!,
                        viewModel = viewModel,
                        onExit = { viewModel.exitStudySession() }
                    )
                } else {
                    MainNavigationShell(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationShell(viewModel: ChemistryViewModel) {
    val iqaiViewModel: IqaiViewModel = viewModel()
    val focusViewModel: FocusViewModel = viewModel()

    val currentNav by viewModel.currentNav.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    val pomodoroSeconds by viewModel.pomodoroSeconds.collectAsState()
    val isPomodoroRunning by viewModel.isPomodoroRunning.collectAsState()

    var activePdfDocument by remember { mutableStateOf<com.example.data.local.PdfDocumentEntity?>(null) }

    if (activePdfDocument != null) {
        BackHandler {
            activePdfDocument = null
        }
        PdfReaderScreen(
            document = activePdfDocument!!,
            viewModel = viewModel,
            onClose = { activePdfDocument = null }
        )
        return
    }

    // Handle back button when not on dashboard
    BackHandler(enabled = currentNav != "dash") {
        viewModel.navigateTo("dash")
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val initials = remember(prefs.studentName) {
                            prefs.studentName.split(" ").filter { it.isNotBlank() }.mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase().ifEmpty { "AF" }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = prefs.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "BUILDER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = prefs.studentGrade,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "📞 ${prefs.studentPhone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "✉️ ${prefs.studentEmail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()

                Text(
                    text = "MASTERY OFFLINE FOUNDATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                    label = { Text("Mastery Assistant & Matrix") },
                    selected = currentNav == "iqai_chat",
                    onClick = {
                        viewModel.navigateTo("iqai_chat")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text("Local Textbook Reader") },
                    selected = currentNav == "iqai_textbook",
                    onClick = {
                        viewModel.navigateTo("iqai_textbook")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("Active Recall (Anki SM-2)") },
                    selected = currentNav == "iqai_quiz",
                    onClick = {
                        viewModel.navigateTo("iqai_quiz")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    label = { Text("Wiki Notes (FTS4)") },
                    selected = currentNav == "iqai_notes",
                    onClick = {
                        viewModel.navigateTo("iqai_notes")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.LocalLibrary, contentDescription = null) },
                    label = { Text("Reference Archive") },
                    selected = currentNav == "iqai_archive",
                    onClick = {
                        viewModel.navigateTo("iqai_archive")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    label = { Text("Strict Focus Timer") },
                    selected = currentNav == "iqai_focus",
                    onClick = {
                        viewModel.navigateTo("iqai_focus")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    label = { Text("Timetable & Song Alarms") },
                    selected = currentNav == "timetable",
                    onClick = {
                        viewModel.navigateTo("timetable")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                    label = { Text("Analytics & Progress Engine") },
                    selected = currentNav == "analytics",
                    onClick = {
                        viewModel.navigateTo("analytics")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Spellcheck, contentDescription = null) },
                    label = { Text("Offline Syllabus Dictionary") },
                    selected = currentNav == "dictionary",
                    onClick = {
                        viewModel.navigateTo("dictionary")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                    label = { Text("Graph Maker & Calculator") },
                    selected = currentNav == "graph_calc",
                    onClick = {
                        viewModel.navigateTo("graph_calc")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                    label = { Text("MCQ Question Studio (CRUD)") },
                    selected = currentNav == "question_studio",
                    onClick = {
                        viewModel.navigateTo("question_studio")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("About Developer (Alexander)") },
                    selected = currentNav == "about_developer",
                    onClick = {
                        viewModel.navigateTo("about_developer")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "WORKSPACE & STUDY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = currentNav == "dash",
                    onClick = {
                        viewModel.navigateTo("dash")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Workspaces, contentDescription = null) },
                    label = { Text("Student Workspace") },
                    selected = currentNav == "workspace",
                    onClick = {
                        viewModel.navigateTo("workspace")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    label = { Text("Syllabus Mastery Tracker") },
                    selected = currentNav == "syllabus",
                    onClick = {
                        viewModel.navigateTo("syllabus")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    label = { Text("Question Library") },
                    selected = currentNav == "lib",
                    onClick = {
                        viewModel.navigateTo("lib")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Book, contentDescription = null) },
                    label = { Text("Incorrect Notebook") },
                    selected = currentNav == "note",
                    onClick = {
                        viewModel.navigateTo("note")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    label = { Text("Mock Exam & Sprint") },
                    selected = currentNav == "exam",
                    onClick = {
                        viewModel.navigateTo("exam")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Style, contentDescription = null) },
                    label = { Text("SRS Flashcard Deck") },
                    selected = currentNav == "flashcards",
                    onClick = {
                        viewModel.navigateTo("flashcards")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    label = { Text("Daily Plan & Goals") },
                    selected = currentNav == "plan",
                    onClick = {
                        viewModel.navigateTo("plan")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    label = { Text("Timers, Stopwatch & Alarms") },
                    selected = currentNav == "timer",
                    onClick = {
                        viewModel.navigateTo("timer")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.HourglassTop, contentDescription = null) },
                    label = { Text("Focus & Wellbeing") },
                    selected = currentNav == "focus",
                    onClick = {
                        viewModel.navigateTo("focus")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    label = { Text("PDF Textbooks Reader") },
                    selected = currentNav == "pdf",
                    onClick = {
                        viewModel.navigateTo("pdf")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Text(
                    text = "SCIENCE & UTILITY TOOLKITS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    label = { Text("Science & Math Toolkit") },
                    selected = currentNav == "science_tools",
                    onClick = {
                        viewModel.navigateTo("science_tools")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("ToolNest (50+ Utilities)") },
                    selected = currentNav == "utilities",
                    onClick = {
                        viewModel.navigateTo("utilities")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Science, contentDescription = null) },
                    label = { Text("Chemistry Lab Tools") },
                    selected = currentNav == "tools",
                    onClick = {
                        viewModel.navigateTo("tools")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Insights & Analytics") },
                    selected = currentNav == "ins",
                    onClick = {
                        viewModel.navigateTo("ins")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                    label = { Text("Study Ambience & Audio") },
                    selected = currentNav == "music",
                    onClick = {
                        viewModel.navigateTo("music")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = { Text("Study Media Sandbox") },
                    selected = currentNav == "media_sandbox",
                    onClick = {
                        viewModel.navigateTo("media_sandbox")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PermDeviceInformation, contentDescription = null) },
                    label = { Text("Device Diagnostics") },
                    selected = currentNav == "device_info",
                    onClick = {
                        viewModel.navigateTo("device_info")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Text(
                    text = "CONTENT & APP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("All-Subject Studio") },
                    selected = currentNav == "gen",
                    onClick = {
                        viewModel.navigateTo("gen")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ImportExport, contentDescription = null) },
                    label = { Text("Import & Export") },
                    selected = currentNav == "imp",
                    onClick = {
                        viewModel.navigateTo("imp")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text("Backup Center & ZIP") },
                    selected = currentNav == "backup",
                    onClick = {
                        viewModel.navigateTo("backup")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = currentNav == "set",
                    onClick = {
                        viewModel.navigateTo("set")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        var showGlobalSearch by remember { mutableStateOf(false) }
        var showFontDialog by remember { mutableStateOf(false) }

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    title = {
                        Text(
                            text = "Mastery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("nav_drawer_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Quick Font Switcher (English + Amharic)
                        IconButton(onClick = { showFontDialog = true }) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "A/አ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Global Quick Search
                        IconButton(onClick = { showGlobalSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Quick Search")
                        }

                        // Focus Pomodoro Timer Button
                        val min = pomodoroSeconds / 60
                        val sec = pomodoroSeconds % 60
                        AssistChip(
                            onClick = { viewModel.togglePomodoro() },
                            label = {
                                Text(
                                    text = "${if (isPomodoroRunning) "⏸" else "🍅"} ${String.format(Locale.US, "%02d:%02d", min, sec)}",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.testTag("pomodoro_button")
                        )

                        // Theme Toggle Button
                        IconButton(
                            onClick = {
                                val nextTheme = if (prefs.theme == "dark") "light" else "dark"
                                viewModel.updateTheme(nextTheme)
                            }
                        ) {
                            Icon(
                                imageVector = if (prefs.theme == "dark") Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                // Strictly ICON-ONLY Navigation Bar to maximize screen space
                NavigationBar(
                    windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    val navItems = listOf(
                        Triple("dash", "Home", Icons.Default.Home),
                        Triple("question_studio", "MCQ Practice", Icons.Default.Quiz),
                        Triple("timetable", "Timetable", Icons.Default.CalendarToday),
                        Triple("iqai_chat", "Assistant", Icons.Default.SmartToy)
                    )

                    navItems.forEach { (id, label, icon) ->
                        NavigationBarItem(
                            selected = currentNav == id,
                            onClick = { viewModel.navigateTo(id) },
                            icon = { Icon(icon, contentDescription = label) },
                            alwaysShowLabel = false,
                            modifier = Modifier.testTag("bottom_nav_$id")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentNav) {
                    "dash" -> DashboardScreen(viewModel = viewModel)
                    "timetable" -> TimetablePlannerScreen(viewModel = iqaiViewModel, onBack = { viewModel.navigateTo("dash") })
                    "analytics" -> AnalyticsProgressScreen(viewModel = iqaiViewModel, onBack = { viewModel.navigateTo("dash") })
                    "dictionary" -> DictionaryScreen(viewModel = iqaiViewModel, onBack = { viewModel.navigateTo("dash") })
                    "graph_calc" -> ScientificGraphScreen(onBack = { viewModel.navigateTo("dash") })
                    "question_studio", "gen" -> QuestionStudioScreen(viewModel = iqaiViewModel, onBack = { viewModel.navigateTo("dash") })
                    "about_developer" -> AboutDeveloperScreen(onBack = { viewModel.navigateTo("dash") })
                    "media_sandbox" -> StudyMediaViewerScreen(onBack = { viewModel.navigateTo("dash") })
                    "iqai_chat" -> IqaiChatScreen(viewModel = iqaiViewModel, onBack = { viewModel.navigateTo("dash") })
                    "iqai_textbook" -> TextbookViewerScreen(viewModel = iqaiViewModel, onBackToMain = { viewModel.navigateTo("dash") })
                    "iqai_quiz" -> QuizScreen(viewModel = iqaiViewModel, onBack = { viewModel.navigateTo("dash") })
                    "iqai_notes" -> WikiNotesScreen(viewModel = iqaiViewModel, onBack = { viewModel.navigateTo("dash") })
                    "iqai_archive" -> OfflineArchiveViewer(onBack = { viewModel.navigateTo("dash") })
                    "iqai_focus" -> StrictFocusScreen(focusViewModel = focusViewModel, onBack = { viewModel.navigateTo("dash") })
                    "workspace" -> StudentWorkspaceScreen(viewModel = viewModel)
                    "syllabus" -> SyllabusTrackerScreen(viewModel = viewModel)
                    "lib" -> LibraryScreen(viewModel = viewModel)
                    "note" -> NotebookScreen(viewModel = viewModel)
                    "exam" -> MockExamScreen(viewModel = viewModel)
                    "flashcards" -> FlashcardTrainerScreen(viewModel = viewModel)
                    "plan" -> DailyPlanScreen(viewModel = viewModel)
                    "timer" -> TimerStopwatchScreen(viewModel = viewModel)
                    "focus" -> FocusScreen(viewModel = viewModel)
                    "pdf" -> PdfLibraryScreen(viewModel = viewModel, onOpenReader = { activePdfDocument = it })
                    "science_tools" -> ScienceToolsScreen()
                    "utilities" -> UtilityCenterScreen()
                    "device_info" -> DeviceDiagnosticsScreen()
                    "ins" -> InsightsScreen(viewModel = viewModel)
                    "tools" -> ChemistryToolsScreen()
                    "music" -> MusicPlayerScreen(viewModel = viewModel)
                    "imp" -> ImportExportScreen(viewModel = viewModel)
                    "backup" -> BackupCenterScreen(viewModel = viewModel)
                    "set" -> SettingsScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel)
                }
            }
        }

        // Font Adaptability Selection Dialog
        if (showFontDialog) {
            val fontOptions = listOf(
                Pair("Modern Sans", "ዘመናዊ ሳንስ (Noto Sans Ethiopic + Clean Latin)"),
                Pair("Ethiopic Serif", "ኢትዮጵያዊ ሰሪፍ (Noto Serif Ethiopic Classical)"),
                Pair("Classic Serif", "ክላሲክ ሰሪፍ (Classic Literature Serif)"),
                Pair("Scientific Mono", "ቀመሮች ሞኖስፔስ (Roboto Mono Math & Equations)")
            )
            AlertDialog(
                onDismissRequest = { showFontDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔤", fontSize = 20.sp)
                        Text("Font Adaptability (English & አማርኛ)")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Choose dynamic font rendering for all questions, headers, formulas, and dashboards:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        fontOptions.forEach { (fontKey, fontLabel) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (prefs.selectedFont == fontKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (prefs.selectedFont == fontKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateFont(fontKey)
                                        showFontDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fontKey, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(fontLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (prefs.selectedFont == fontKey) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showGlobalSearch) {
            var searchTxt by remember { mutableStateOf("") }
            val searchDestinations = listOf(
                Triple("dash", "Dashboard Overview", "Home, daily progress, quick launcher"),
                Triple("syllabus", "Syllabus Tracker", "Grades 11-12 curriculum mastery across all 6 subjects"),
                Triple("science_tools", "Science & Math Tools", "Calculators, physics kinematics, chemistry, biology & logic"),
                Triple("utilities", "ToolNest 50+ Utilities", "Unit converters, text tools, crypto, number bases, hashes"),
                Triple("timer", "Timers, Stopwatch & Alarms", "Precision stopwatch, countdown presets, recurring study alarms"),
                Triple("flashcards", "SRS Flashcard Deck", "Spaced repetition active recall with 3D flip cards"),
                Triple("exam", "Mock Exam & Sprint", "Timed EUEE examinations, instant scoring and diagnosis"),
                Triple("pdf", "PDF Textbooks Reader", "Native offline PDF reader with pinch-to-zoom and annotations"),
                Triple("workspace", "Student Workspace", "Subject notes, to-do task planner, formula book"),
                Triple("focus", "Focus & Wellbeing", "Deep study modes, Pomodoro timer, ambient audio"),
                Triple("device_info", "Device Diagnostics", "Hardware, Android OS, battery, storage, display metrics"),
                Triple("tools", "Chemistry Lab Tools", "Periodic table of 118 elements, reaction balancer, molar mass"),
                Triple("music", "Study Ambience & Audio", "Binaural tones, nature sounds, audio player"),
                Triple("backup", "Backup Center", "Complete JSON export/restore, ZIP backups")
            )
            val filteredDestinations = searchDestinations.filter {
                searchTxt.isBlank() || it.second.contains(searchTxt, ignoreCase = true) || it.third.contains(searchTxt, ignoreCase = true)
            }

            AlertDialog(
                onDismissRequest = { showGlobalSearch = false },
                title = { Text("🔍 Global Quick Navigation") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchTxt,
                            onValueChange = { searchTxt = it },
                            placeholder = { Text("Jump to any tool, subject or feature...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredDestinations) { (route, title, desc) ->
                                Card(
                                    onClick = {
                                        viewModel.navigateTo(route)
                                        showGlobalSearch = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showGlobalSearch = false }) { Text("Close") }
                }
            )
        }
    }
}
