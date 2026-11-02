package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TimetableSlot
import com.example.util.AlarmHelper
import com.example.viewmodel.IqaiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetablePlannerScreen(
    viewModel: IqaiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allSlots by viewModel.allTimetableSlots.collectAsState()
    val selectedDay by viewModel.selectedTimetableDay.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var slotToEdit by remember { mutableStateOf<TimetableSlot?>(null) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val fullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val currentDaySlots = remember(allSlots, selectedDay) {
        allSlots.filter { it.dayOfWeek == selectedDay }
            .sortedBy { it.timeSlot }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                title = {
                    Column {
                        Text(
                            text = "Timetable & Song Alarms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allSlots.size} study slots scheduled",
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
                        onClick = {
                            slotToEdit = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_timetable_button")
                    ) {
                        Icon(Icons.Default.AddAlarm, contentDescription = "Add Slot")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    slotToEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_timetable")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Timetable Slot")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Multi-Day Day Switcher Bar
            ScrollableTabRow(
                selectedTabIndex = selectedDay,
                edgePadding = 16.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                daysOfWeek.forEachIndexed { index, shortName ->
                    val daySlotCount = allSlots.count { it.dayOfWeek == index }
                    Tab(
                        selected = selectedDay == index,
                        onClick = { viewModel.selectTimetableDay(index) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(shortName, fontWeight = if (selectedDay == index) FontWeight.Bold else FontWeight.Normal)
                                if (daySlotCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedDay == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$daySlotCount",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedDay == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day Summary Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${fullDayNames[selectedDay]} Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${currentDaySlots.size} study slots scheduled for this day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            slotToEdit = null
                            showAddDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Slot", fontSize = 12.sp)
                    }
                }
            }

            // Slots List
            if (currentDaySlots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "No study slots for ${fullDayNames[selectedDay]}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap '+ Add Slot' to schedule targeted study intervals with custom song alarms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentDaySlots, key = { it.id }) { slot ->
                        TimetableSlotCard(
                            slot = slot,
                            onToggleAlarm = { enabled ->
                                if (enabled) {
                                    AlarmHelper.requestExactAlarmPermissionIfNeeded(context)
                                }
                                viewModel.toggleTimetableAlarm(slot.id, enabled)
                                val updatedSlot = slot.copy(isAlarmEnabled = enabled)
                                AlarmHelper.scheduleTimetableSlotAlarm(context, updatedSlot)
                            },
                            onEdit = {
                                slotToEdit = slot
                                showAddDialog = true
                            },
                            onDelete = {
                                viewModel.deleteTimetableSlot(slot.id)
                                AlarmHelper.cancelAlarm(context, "timetable_${slot.id}")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TimetableSlotDialog(
            initialSlot = slotToEdit,
            initialDayIndex = selectedDay,
            onDismiss = {
                showAddDialog = false
                slotToEdit = null
            },
            onSave = { savedSlot ->
                viewModel.saveTimetableSlot(savedSlot)
                if (savedSlot.isAlarmEnabled) {
                    AlarmHelper.requestExactAlarmPermissionIfNeeded(context)
                    AlarmHelper.scheduleTimetableSlotAlarm(context, savedSlot)
                }
                showAddDialog = false
                slotToEdit = null
            }
        )
    }
}

@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    onToggleAlarm: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isTestingAudio by remember { mutableStateOf(false) }

    val subjectColor = when (slot.subject.lowercase()) {
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = subjectColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = slot.subject,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = slot.timeSlot,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = slot.isAlarmEnabled,
                    onCheckedChange = onToggleAlarm,
                    modifier = Modifier.height(24.dp)
                )
            }

            Text(
                text = slot.topic.ifBlank { "Untitled Session" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (slot.notes.isNotBlank()) {
                Text(
                    text = slot.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Audio / Song indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (slot.audioUri != null) Icons.Default.MusicNote else Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = if (slot.audioUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = slot.audioTitle ?: if (slot.audioUri != null) "Custom Song" else "Default Alarm Tone",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (slot.audioUri != null) {
                        IconButton(
                            onClick = {
                                if (isTestingAudio) {
                                    isTestingAudio = false
                                } else {
                                    try {
                                        val mp = MediaPlayer().apply {
                                            setDataSource(context, Uri.parse(slot.audioUri))
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                isTestingAudio = false
                                                release()
                                            }
                                        }
                                        isTestingAudio = true
                                    } catch (_: Exception) {
                                        isTestingAudio = false
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isTestingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Preview Audio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableSlotDialog(
    initialSlot: TimetableSlot?,
    initialDayIndex: Int,
    onDismiss: () -> Unit,
    onSave: (TimetableSlot) -> Unit
) {
    val context = LocalContext.current
    var selectedDay by remember { mutableIntStateOf(initialSlot?.dayOfWeek ?: initialDayIndex) }
    var timeSlot by remember { mutableStateOf(initialSlot?.timeSlot ?: "08:00 - 09:30") }
    var subject by remember { mutableStateOf(initialSlot?.subject ?: "Chemistry") }
    var topic by remember { mutableStateOf(initialSlot?.topic ?: "") }
    var notes by remember { mutableStateOf(initialSlot?.notes ?: "") }
    var audioUri by remember { mutableStateOf(initialSlot?.audioUri) }
    var audioTitle by remember { mutableStateOf(initialSlot?.audioTitle) }
    var isAlarmEnabled by remember { mutableStateOf(initialSlot?.isAlarmEnabled ?: true) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val fullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val subjects = listOf("Chemistry", "Physics", "Biology", "Mathematics", "English", "General")

    // Audio File Picker targeting audio files
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            var fileName: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            audioUri = uri.toString()
            audioTitle = fileName ?: "Selected Song"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialSlot == null) "Add Study Slot" else "Edit Study Slot", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day selector
                Text("Select Day:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysOfWeek.forEachIndexed { idx, shortName ->
                        FilterChip(
                            selected = selectedDay == idx,
                            onClick = { selectedDay = idx },
                            label = { Text(shortName, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Subject picker
                Text("Subject:", style = MaterialTheme.typography.labelMedium)
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
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time Slot (e.g. 08:00 - 09:30)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Chapter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Notes / Targets") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Custom Song Picker Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alarm Song", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = audioTitle ?: "Default alert chime",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row {
                            if (audioUri != null) {
                                IconButton(
                                    onClick = {
                                        audioUri = null
                                        audioTitle = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Song", modifier = Modifier.size(16.dp))
                                }
                            }
                            FilledTonalButton(
                                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Choose Song", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Alarm Notification", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isAlarmEnabled, onCheckedChange = { isAlarmEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val slot = TimetableSlot(
                        id = initialSlot?.id ?: 0,
                        dayOfWeek = selectedDay,
                        dayName = fullDayNames[selectedDay],
                        timeSlot = timeSlot.trim().ifBlank { "08:00 - 09:30" },
                        subject = subject,
                        topic = topic.trim(),
                        audioUri = audioUri,
                        audioTitle = audioTitle,
                        isAlarmEnabled = isAlarmEnabled,
                        notes = notes.trim()
                    )
                    onSave(slot)
                }
            ) {
                Text("Save Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
