package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StudyAlarmEntity
import com.example.util.AlarmHelper
import com.example.util.AudioHapticHelper
import com.example.viewmodel.ChemistryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

data class StopwatchLap(
    val lapNumber: Int,
    val lapTimeMs: Long,
    val totalTimeMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerStopwatchScreen(
    viewModel: ChemistryViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Stopwatch", "Countdown", "Study Alarms")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        when (selectedTab) {
            0 -> StopwatchTab()
            1 -> CountdownTimerTab(viewModel)
            2 -> StudyAlarmsTab(viewModel)
        }
    }
}

@Composable
fun StopwatchTab() {
    val context = LocalContext.current
    val audioHaptic = remember { AudioHapticHelper(context) }
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<StopwatchLap>() }
    var lastLapTotalTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - elapsedTimeMs
            while (isRunning) {
                delay(30)
                elapsedTimeMs = System.currentTimeMillis() - startTime
            }
        }
    }

    val minutes = (elapsedTimeMs / 60000)
    val seconds = (elapsedTimeMs % 60000) / 1000
    val millis = (elapsedTimeMs % 1000) / 10

    val timeFormatted = String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)

    val bestLap = laps.minByOrNull { it.lapTimeMs }
    val slowestLap = laps.maxByOrNull { it.lapTimeMs }
    val avgLapMs = if (laps.isNotEmpty()) laps.map { it.lapTimeMs }.average().toLong() else 0L

    fun formatMs(ms: Long): String {
        val m = ms / 60000
        val s = (ms % 60000) / 1000
        val msTenth = (ms % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", m, s, msTenth)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏱️ PRECISION STOPWATCH",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (laps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Avg: ${formatMs(avgLapMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            bestLap?.let {
                                Text(
                                    text = "Best: #${it.lapNumber} (${formatMs(it.lapTimeMs)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset / Lap button
                        FilledTonalButton(
                            onClick = {
                                if (isRunning) {
                                    val currentLapTime = elapsedTimeMs - lastLapTotalTimeMs
                                    lastLapTotalTimeMs = elapsedTimeMs
                                    laps.add(0, StopwatchLap(laps.size + 1, currentLapTime, elapsedTimeMs))
                                    audioHaptic.vibrateAlert()
                                } else {
                                    // Reset
                                    elapsedTimeMs = 0L
                                    lastLapTotalTimeMs = 0L
                                    laps.clear()
                                }
                            },
                            enabled = isRunning || elapsedTimeMs > 0,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(if (isRunning) "Lap" else "Reset")
                        }

                        // Start / Stop button
                        Button(
                            onClick = {
                                isRunning = !isRunning
                                audioHaptic.vibrateAlert()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRunning) "Stop" else "Start")
                        }

                        // Export Laps button
                        if (laps.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val text = buildString {
                                        appendLine("=== EUEE Stopwatch Lap Summary ===")
                                        appendLine("Total Elapsed: $timeFormatted")
                                        appendLine("Laps Count: ${laps.size}")
                                        appendLine("Average Lap: ${formatMs(avgLapMs)}")
                                        appendLine("------------------------------")
                                        laps.reversed().forEach {
                                            appendLine("Lap ${it.lapNumber}: ${formatMs(it.lapTimeMs)} (Total: ${formatMs(it.totalTimeMs)})")
                                        }
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Lap Times"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Export")
                            }
                        }
                    }
                }
            }
        }

        if (laps.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Lap Time", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Overall Time", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                HorizontalDivider()
            }

            itemsIndexed(laps) { _, lap ->
                val isFastest = bestLap?.lapNumber == lap.lapNumber
                val isSlowest = slowestLap?.lapNumber == lap.lapNumber && laps.size > 1

                val lapColor = when {
                    isFastest -> Color(0xFF2E7D32)
                    isSlowest -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lap ${lap.lapNumber}",
                        color = lapColor,
                        fontWeight = if (isFastest || isSlowest) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = formatMs(lap.lapTimeMs),
                        fontFamily = FontFamily.Monospace,
                        color = lapColor,
                        fontWeight = if (isFastest) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = formatMs(lap.totalTimeMs),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownTimerTab(viewModel: ChemistryViewModel) {
    val context = LocalContext.current
    val audioHaptic = remember { AudioHapticHelper(context) }

    var isRunning by remember { mutableStateOf(false) }
    var totalSeconds by remember { mutableIntStateOf(1500) } // Default 25m
    var remainingSeconds by remember { mutableIntStateOf(1500) }

    val presets = listOf(
        Pair("Quick Sprint (15m)", 15 * 60),
        Pair("Pomodoro (25m)", 25 * 60),
        Pair("Reading (45m)", 45 * 60),
        Pair("Full Exam (60m)", 60 * 60),
        Pair("Deep Work (90m)", 90 * 60)
    )

    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        }
        if (isRunning && remainingSeconds == 0) {
            isRunning = false
            audioHaptic.playFeedback(isCorrect = true)
            audioHaptic.vibrateAlert()
            Toast.makeText(context, "⏰ Timer Completed!", Toast.LENGTH_LONG).show()
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏳ STUDY COUNTDOWN TIMER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(20.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isRunning) "TICKING" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = {
                                isRunning = false
                                remainingSeconds = totalSeconds
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Reset")
                        }

                        Button(
                            onClick = {
                                if (remainingSeconds == 0) remainingSeconds = totalSeconds
                                isRunning = !isRunning
                                audioHaptic.vibrateAlert()
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRunning) "Pause" else "Start")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Timer Duration Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(presets) { (label, durationSecs) ->
            Card(
                onClick = {
                    isRunning = false
                    totalSeconds = durationSecs
                    remainingSeconds = durationSecs
                    audioHaptic.vibrateAlert()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalSeconds == durationSecs) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text("${durationSecs / 60} minutes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (totalSeconds == durationSecs) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAlarmsTab(viewModel: ChemistryViewModel) {
    val alarms by viewModel.studyAlarms.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var alarmTitle by remember { mutableStateOf("Morning EUEE Study") }
    var alarmHour by remember { mutableIntStateOf(6) }
    var alarmMinute by remember { mutableIntStateOf(0) }
    var vibrate by remember { mutableStateOf(true) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.AddAlarm, contentDescription = "Add Alarm")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Scheduled Study Alarms",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Wake up, start revision, and countdown to exam day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (alarms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AlarmOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "No study alarms set yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Tap '+' to schedule a daily morning or revision alarm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = alarm.title,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Daily recurring • ${if (alarm.vibrate) "Vibrate on" else "Sound only"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleAlarmEnabled(alarm.id, checked)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.deleteAlarm(alarm.id)
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Alarm", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Study Alarm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = alarmTitle,
                        onValueChange = { alarmTitle = it },
                        label = { Text("Alarm Label / Objective") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = alarmHour.toString(),
                            onValueChange = { it.toIntOrNull()?.let { h -> if (h in 0..23) alarmHour = h } },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = alarmMinute.toString(),
                            onValueChange = { it.toIntOrNull()?.let { m -> if (m in 0..59) alarmMinute = m } },
                            label = { Text("Minute (0-59)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = vibrate, onCheckedChange = { vibrate = it })
                        Text("Vibrate on alarm", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAlarm = StudyAlarmEntity(
                            id = "alarm_${System.currentTimeMillis()}",
                            title = alarmTitle.ifBlank { "Study Session" },
                            hour = alarmHour,
                            minute = alarmMinute,
                            isEnabled = true,
                            vibrate = vibrate
                        )
                        viewModel.addStudyAlarm(newAlarm)
                        showAddDialog = false
                    }
                ) {
                    Text("Save Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
