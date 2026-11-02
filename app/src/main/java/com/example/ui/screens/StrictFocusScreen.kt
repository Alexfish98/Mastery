package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.viewmodel.FocusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 5. LIFECYCLE-AWARE FOCUS TIMER (STRICT MODE)
 * Uses DisposableEffect + LifecycleEventObserver to detect ON_STOP and log failures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrictFocusScreen(
    focusViewModel: FocusViewModel,
    onBack: () -> Unit = {}
) {
    val timeRemaining by focusViewModel.timeRemainingSeconds.collectAsState()
    val initialMinutes by focusViewModel.initialDurationMinutes.collectAsState()
    val isRunning by focusViewModel.isTimerRunning.collectAsState()
    val isPenaltyActive by focusViewModel.isPenaltyActive.collectAsState()
    val penaltyMessage by focusViewModel.penaltyMessage.collectAsState()
    val sessionSuccess by focusViewModel.sessionCompletedSuccess.collectAsState()

    val successfulCount by focusViewModel.successfulCount.collectAsState()
    val failedCount by focusViewModel.failedCount.collectAsState()
    val failedSessions by focusViewModel.failedSessions.collectAsState()
    val successfulSessions by focusViewModel.successfulSessions.collectAsState()

    // STRICT MODE LIFECYCLE OBSERVER:
    // If the application lifecycle triggers an 'ON_STOP' state, instantly flag session as failed
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isRunning) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isRunning) {
                focusViewModel.handleAppMinimizedPenalty()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val totalSeconds = initialMinutes * 60
    val progress = if (totalSeconds > 0) (totalSeconds - timeRemaining).toFloat() / totalSeconds.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Strict Focus Timer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (isRunning) "🛡️ Background Detection Armed" else "Idle • Select Duration",
                            fontSize = 11.sp,
                            color = if (isRunning) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Strict Mode Warning Card
                Surface(
                    color = if (isRunning) Color(0x33EF4444) else Color(0x226366F1),
                    border = BorderStroke(1.dp, if (isRunning) Color(0xFFEF4444) else Color(0x66818CF8)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(if (isRunning) "🚨" else "🔒", fontSize = 20.sp)
                        Column {
                            Text(
                                text = if (isRunning) "STRICT LOCKDOWN ACTIVE" else "STRICT DISCIPLINE MODE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isRunning) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Exiting the app, checking notifications, or minimizing will instantly abort the session and log a disciplinary failure strike.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Preset Duration Selectors
                if (!isRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45, 60).forEach { minsPreset ->
                            FilterChip(
                                selected = initialMinutes == minsPreset,
                                onClick = { focusViewModel.setDuration(minsPreset) },
                                label = { Text("${minsPreset}m", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Main Circular Countdown Display
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (isRunning) Color(0xFF31151E) else Color(0xFF1E293B),
                                    Color(0xFF0F172A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(220.dp),
                        strokeWidth = 10.dp,
                        color = if (isRunning) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF334155)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedTime,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (isRunning) "FOCUS LOCK" else "READY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) Color(0xFFF87171) else Color(0xFF94A3B8),
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isRunning) {
                        Button(
                            onClick = { focusViewModel.startTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Focus Block", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { focusViewModel.pauseTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Pause")
                        }

                        Button(
                            onClick = { focusViewModel.resetTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Abort")
                        }
                    }
                }

                // Session Discipline Statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFF059669))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("SUCCESSFUL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            Text("$successfulCount Sessions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFFDC2626))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("FAILED STRIKES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                            Text("$failedCount Violations", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Recent Discipline Log
                Text(
                    text = "Recent Discipline History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                val combinedSessions = remember(successfulSessions, failedSessions) {
                    (successfulSessions + failedSessions).sortedByDescending { it.timestamp }.take(6)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(combinedSessions) { session ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (session.wasSuccessful) Color(0x2210B981) else Color(0x22EF4444),
                            border = BorderStroke(1.dp, if (session.wasSuccessful) Color(0x4410B981) else Color(0x44EF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (session.wasSuccessful) "✅" else "❌")
                                    Column {
                                        Text(
                                            text = if (session.wasSuccessful) "Completed ${session.durationMinutes}m Session" else "Violated after ${session.durationMinutes}m",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        val timeStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
                                        Text(timeStr, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (session.wasSuccessful) Color(0xFF10B981) else Color(0xFFEF4444)
                                ) {
                                    Text(
                                        text = if (session.wasSuccessful) "PASSED" else "PENALTY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // VISUAL PENALTY SCREEN OVERLAY UPON APP RESUME
            if (isPenaltyActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE6450A0A))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F0707)),
                        border = BorderStroke(2.dp, Color(0xFFEF4444))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }

                            Text(
                                text = "STRICT FOCUS FAILED!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF87171),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = penaltyMessage,
                                fontSize = 14.sp,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            HorizontalDivider(color = Color(0xFF7F1D1D))

                            Text(
                                text = "A disciplinary strike was recorded to your local Room database.\nDiscipline is the bridge between goals and accomplishment.",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )

                            Button(
                                onClick = { focusViewModel.dismissPenalty() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Acknowledge & Recommit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Success Dialog
            if (sessionSuccess) {
                AlertDialog(
                    onDismissRequest = { focusViewModel.dismissSuccess() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🏆", fontSize = 24.sp)
                            Text("Focus Block Mastered!")
                        }
                    },
                    text = {
                        Text("Outstanding discipline! You completed a $initialMinutes minute strict focus block without leaving the app. Saved to Room DB.")
                    },
                    confirmButton = {
                        Button(onClick = { focusViewModel.dismissSuccess() }) {
                            Text("Claim Victory")
                        }
                    }
                )
            }
        }
    }
}
