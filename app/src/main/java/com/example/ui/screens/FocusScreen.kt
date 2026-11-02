package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AmbientAudioGenerator
import com.example.util.DndHelper
import com.example.viewmodel.ChemistryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: ChemistryViewModel
) {
    val context = LocalContext.current
    val pomodoroSeconds by viewModel.pomodoroSeconds.collectAsState()
    val isRunning by viewModel.isPomodoroRunning.collectAsState()
    val isBreak by viewModel.isPomodoroBreak.collectAsState()
    val focusGoalMinutes by viewModel.dailyFocusGoalMinutes.collectAsState()
    val completedFocusMinutes by viewModel.todayCompletedFocusMinutes.collectAsState()
    val distractionLogs by viewModel.distractionLogs.collectAsState()
    val focusHistory by viewModel.focusHistory.collectAsState()

    // Player state
    val playerManager = viewModel.musicPlayerManager
    val isAudioPlaying by playerManager.isPlaying.collectAsState()
    val currentTrack by playerManager.currentTrack.collectAsState()
    val activeAmbient by playerManager.activeAmbient.collectAsState()

    var showDistractionDialog by remember { mutableStateOf(false) }
    var distractionNote by remember { mutableStateOf("") }
    var showDndExplainer by remember { mutableStateOf(false) }
    var dndEnabled by remember { mutableStateOf(false) }

    val hasDndPermission = remember { DndHelper.isDndPermissionGranted(context) }

    val minutes = pomodoroSeconds / 60
    val seconds = pomodoroSeconds % 60
    val progress = (pomodoroSeconds.toFloat() / (if (isBreak) 5 * 60 else 25 * 60)).coerceIn(0f, 1f)

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBreak) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBreak) "☕ RECOVERY BREAK" else "🎯 DEEP FOCUS SESSION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isBreak) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(Modifier.height(16.dp))

                        // Large circular timer display
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(190.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(190.dp),
                                strokeWidth = 10.dp,
                                color = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isRunning) "Focus in progress" else "Paused",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Timer action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.togglePomodoro()
                                    if (!isRunning && dndEnabled && hasDndPermission) {
                                        viewModel.activateDnd()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isRunning) "Pause" else "Start Focus")
                            }

                            FilledTonalButton(
                                onClick = { viewModel.resetPomodoro() }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Reset")
                            }

                            OutlinedButton(
                                onClick = { showDistractionDialog = true }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Jot Thought")
                            }
                        }
                    }
                }
            }

            // Daily Focus Progress
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daily Focus Goal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("$completedFocusMinutes / $focusGoalMinutes min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        val goalProgress = (completedFocusMinutes.toFloat() / focusGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            // Do Not Disturb & Digital Wellbeing Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DoNotDisturbOn,
                                    contentDescription = null,
                                    tint = if (dndEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Do Not Disturb (DND)", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (hasDndPermission) "Mute distractions automatically" else "Requires system access permission",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = dndEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !hasDndPermission) {
                                        showDndExplainer = true
                                    } else {
                                        dndEnabled = checked
                                        if (checked) {
                                            viewModel.activateDnd()
                                            Toast.makeText(context, "DND activated for focus", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.deactivateDnd()
                                            Toast.makeText(context, "DND deactivated", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { DndHelper.openDigitalWellbeingSettings(context) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Android Digital Wellbeing", fontWeight = FontWeight.Medium)
                                    Text("View app limits & system focus mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Study Audio & Ambient Player Mini-Controller
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎧 Study Soundscapes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            if (isAudioPlaying) {
                                AssistChip(
                                    onClick = { playerManager.togglePlayPause() },
                                    label = { Text("Playing", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }

                        if (currentTrack != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = currentTrack!!.title,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            Text(
                                text = currentTrack!!.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Quick Ambient Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AmbientChip(
                                title = "🌧️ Rain",
                                isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.RAIN && isAudioPlaying,
                                onClick = { playerManager.playAmbient(AmbientAudioGenerator.AmbientType.RAIN) },
                                modifier = Modifier.weight(1f)
                            )
                            AmbientChip(
                                title = "🌊 Stream",
                                isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.FOREST_STREAM && isAudioPlaying,
                                onClick = { playerManager.playAmbient(AmbientAudioGenerator.AmbientType.FOREST_STREAM) },
                                modifier = Modifier.weight(1f)
                            )
                            AmbientChip(
                                title = "🧠 10Hz Alpha",
                                isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.ALPHA_WAVES && isAudioPlaying,
                                onClick = { playerManager.playAmbient(AmbientAudioGenerator.AmbientType.ALPHA_WAVES) },
                                modifier = Modifier.weight(1f)
                            )
                            AmbientChip(
                                title = "📻 Brown Noise",
                                isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.BROWN_NOISE && isAudioPlaying,
                                onClick = { playerManager.playAmbient(AmbientAudioGenerator.AmbientType.BROWN_NOISE) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isAudioPlaying) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    onClick = { playerManager.stop() }
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Stop Sound")
                                }
                            }
                        }
                    }
                }
            }

            // Distraction Log Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Distraction Log", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (distractionLogs.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearDistractions() }) {
                            Text("Clear")
                        }
                    }
                }
            }

            if (distractionLogs.isEmpty()) {
                item {
                    Text(
                        text = "No distracting thoughts logged yet! Whenever a random urge or task pops into your head during study, tap 'Jot Thought' above to park it for later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(distractionLogs) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(text = note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
            }
        }

        // Distraction dialog
        if (showDistractionDialog) {
            AlertDialog(
                onDismissRequest = { showDistractionDialog = false },
                title = { Text("Park a Distracting Thought") },
                text = {
                    Column {
                        Text(
                            text = "Write down what's distracting you so your brain can let go and stay in flow:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = distractionNote,
                            onValueChange = { distractionNote = it },
                            placeholder = { Text("e.g. Check WhatsApp group, search phone prices...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (distractionNote.isNotBlank()) {
                                viewModel.logDistraction(distractionNote.trim())
                                distractionNote = ""
                            }
                            showDistractionDialog = false
                            Toast.makeText(context, "Thought parked. Return to your focus!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save & Refocus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDistractionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // DND Permission Dialog
        if (showDndExplainer) {
            AlertDialog(
                onDismissRequest = { showDndExplainer = false },
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                title = { Text("Do Not Disturb Access") },
                text = {
                    Text(
                        "Android requires explicit 'Do Not Disturb Access' to allow EUEE Mastery to mute notifications automatically during focus sessions and restore them when done.\n\nTap 'Open Settings' and enable EUEE Mastery in the list."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDndExplainer = false
                            DndHelper.openDndSettings(context)
                        }
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDndExplainer = false }) {
                        Text("Later")
                    }
                }
            )
        }
    }
}

@Composable
fun AmbientChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
