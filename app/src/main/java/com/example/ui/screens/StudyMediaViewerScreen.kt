package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.SandboxStorageHelper
import com.example.util.SandboxedFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMediaViewerScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var sandboxedFiles by remember { mutableStateOf(SandboxStorageHelper.getSandboxedFiles(context)) }
    var activePlayingFile by remember { mutableStateOf<SandboxedFile?>(null) }
    var isLooping by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    fun refreshFiles() {
        sandboxedFiles = SandboxStorageHelper.getSandboxedFiles(context)
    }

    // File picker for sandboxing study videos/media/docs
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var copiedCount = 0
            uris.forEach { uri ->
                val copied = SandboxStorageHelper.copyUriToSandbox(context, uri)
                if (copied != null) copiedCount++
            }
            refreshFiles()
            Toast.makeText(context, "Isolated $copiedCount files in Study Sandbox", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Study Media Sandbox", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "100% Offline Video & Audio Player",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
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
                            mediaPickerLauncher.launch(
                                arrayOf(
                                    "video/*",
                                    "audio/*",
                                    "application/pdf",
                                    "text/plain"
                                )
                            )
                        },
                        modifier = Modifier.testTag("import_media_sandbox_button")
                    ) {
                        Icon(Icons.Default.AddBox, contentDescription = "Import Study File")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Media Player Canvas / Video View or Audio Deck
            if (activePlayingFile != null && isVideoOrAudio(activePlayingFile!!.extension)) {
                val isAudio = activePlayingFile!!.extension.lowercase(Locale.getDefault()) in listOf("mp3", "m4a", "wav", "aac", "ogg")
                var isPlayingState by remember(activePlayingFile) { mutableStateOf(true) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(
                                    if (isAudio) Brush.verticalGradient(
                                        listOf(Color(0xFF0B0F19), Color(0xFF1E1B4B), Color(0xFF111827))
                                    ) else Brush.verticalGradient(
                                        listOf(Color.Black, Color.Black)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Video / Audio AndroidView backend
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        if (!isAudio) {
                                            val mediaController = MediaController(ctx)
                                            mediaController.setAnchorView(this)
                                            setMediaController(mediaController)
                                        }
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = isLooping
                                            try {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            start()
                                            isPlayingState = true
                                        }
                                        setOnCompletionListener {
                                            if (!isLooping) {
                                                isPlayingState = false
                                            }
                                        }
                                        videoViewRef = this
                                    }
                                },
                                update = { view ->
                                    val currentPath = activePlayingFile?.file?.absolutePath
                                    if (currentPath != null) {
                                        view.setVideoPath(currentPath)
                                    }
                                },
                                modifier = if (isAudio) Modifier.size(1.dp) else Modifier.fillMaxSize()
                            )

                            // Dedicated Audio Deck Visualizer UI
                            if (isAudio) {
                                val infiniteTransition = rememberInfiniteTransition(label = "audio_anim")
                                val rotationAngle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 6000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "vinyl_rotate"
                                )
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 0.96f,
                                    targetValue = 1.04f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse_aura"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Spinning Vinyl Record
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(150.dp)
                                            .rotate(if (isPlayingState) rotationAngle else 0f)
                                    ) {
                                        // Outer vinyl disc
                                        Surface(
                                            modifier = Modifier.size(150.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF1E293B),
                                            border = BorderStroke(3.dp, Color(0xFF334155))
                                        ) {}
                                        // Concentric vinyl grooves
                                        Surface(
                                            modifier = Modifier.size(120.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF0F172A),
                                            border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.5f))
                                        ) {}
                                        Surface(
                                            modifier = Modifier.size(90.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF1E1B4B),
                                            border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                                        ) {}
                                        // Center spindle & label
                                        Surface(
                                            modifier = Modifier.size(54.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            shadowElevation = 6.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = "Audio Disc",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Track Info & Equalizer Spectrum
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 16.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "HI-FI AUDIO DECK",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = activePlayingFile?.name ?: "Audio Track",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 2
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Codec: ${activePlayingFile?.extension?.uppercase(Locale.getDefault())} • Offline Local",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Animated Equalizer Frequency Bars
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.Bottom,
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            val barHeights = listOf(0.4f, 0.85f, 0.6f, 1.0f, 0.7f, 0.9f, 0.5f)
                                            barHeights.forEachIndexed { index, weight ->
                                                val barHeightAnim by infiniteTransition.animateFloat(
                                                    initialValue = if (isPlayingState) 8f * weight else 6f,
                                                    targetValue = if (isPlayingState) 26f * weight else 6f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(
                                                            durationMillis = 350 + (index * 80),
                                                            easing = FastOutSlowInEasing
                                                        ),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "eq_bar_$index"
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .width(5.dp)
                                                        .height(barHeightAnim.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(
                                                                    MaterialTheme.colorScheme.primary,
                                                                    MaterialTheme.colorScheme.tertiary
                                                                )
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Playback Speed & Controls Bar
                        Surface(
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activePlayingFile?.name ?: "Media",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${formatFileSize(activePlayingFile?.sizeBytes ?: 0)} • Speed: ${playbackSpeed}x • ${if (isPlayingState) "Playing" else "Paused"}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Play / Pause Toggle
                                    IconButton(
                                        onClick = {
                                            videoViewRef?.let {
                                                if (it.isPlaying) {
                                                    it.pause()
                                                    isPlayingState = false
                                                } else {
                                                    it.start()
                                                    isPlayingState = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingState) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // 10s Rewind
                                    IconButton(
                                        onClick = {
                                            videoViewRef?.let {
                                                val pos = (it.currentPosition - 10000).coerceAtLeast(0)
                                                it.seekTo(pos)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }

                                    // 10s Forward
                                    IconButton(
                                        onClick = {
                                            videoViewRef?.let {
                                                val pos = (it.currentPosition + 10000).coerceAtMost(it.duration)
                                                it.seekTo(pos)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }

                                    // Loop Toggle
                                    IconButton(
                                        onClick = { isLooping = !isLooping },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Repeat,
                                            contentDescription = "Loop",
                                            tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Speed Selector Dropdown
                                    var showSpeedMenu by remember { mutableStateOf(false) }
                                    Box {
                                        TextButton(
                                            onClick = { showSpeedMenu = true },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("${playbackSpeed}x", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        DropdownMenu(
                                            expanded = showSpeedMenu,
                                            onDismissRequest = { showSpeedMenu = false }
                                        ) {
                                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                                                DropdownMenuItem(
                                                    text = { Text("${s}x") },
                                                    onClick = {
                                                        playbackSpeed = s
                                                        showSpeedMenu = false
                                                        try {
                                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                                                // Apply speed dynamically if playing
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Close Player
                                    IconButton(
                                        onClick = {
                                            videoViewRef?.stopPlayback()
                                            activePlayingFile = null
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Header & Storage Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Sandboxed Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val totalBytes = sandboxedFiles.sumOf { it.sizeBytes }
                        Text(
                            text = "${sandboxedFiles.size} offline files • ${formatFileSize(totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            mediaPickerLauncher.launch(
                                arrayOf(
                                    "video/*",
                                    "audio/*",
                                    "application/pdf",
                                    "text/plain"
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add File", fontSize = 12.sp)
                    }
                }
            }

            // Sandboxed Files List
            if (sandboxedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text("No Sandboxed Files Yet", fontWeight = FontWeight.Bold)
                        Text(
                            "Tap 'Add File' to copy video lectures, audio recordings, or PDF guides into your sandboxed offline repository.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sandboxedFiles, key = { it.file.absolutePath }) { item ->
                        SandboxedFileCard(
                            file = item,
                            isPlaying = activePlayingFile?.file?.absolutePath == item.file.absolutePath,
                            onPlay = {
                                activePlayingFile = item
                            },
                            onDelete = {
                                SandboxStorageHelper.deleteSandboxedFile(item.file)
                                if (activePlayingFile?.file?.absolutePath == item.file.absolutePath) {
                                    activePlayingFile = null
                                }
                                refreshFiles()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SandboxedFileCard(
    file: SandboxedFile,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val isMedia = isVideoOrAudio(file.extension)
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    file.extension in listOf("mp4", "mkv", "3gp", "webm") -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    file.extension in listOf("mp3", "m4a", "wav", "aac") -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                    file.extension == "pdf" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                },
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            file.extension in listOf("mp4", "mkv", "3gp", "webm") -> Icons.Default.SmartDisplay
                            file.extension in listOf("mp3", "m4a", "wav", "aac") -> Icons.Default.MusicNote
                            file.extension == "pdf" -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = when {
                            file.extension in listOf("mp4", "mkv", "3gp", "webm") -> Color(0xFFEF4444)
                            file.extension in listOf("mp3", "m4a", "wav", "aac") -> Color(0xFF3B82F6)
                            file.extension == "pdf" -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = "${formatFileSize(file.sizeBytes)} • ${dateFormat.format(Date(file.lastModified))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isMedia) {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun isVideoOrAudio(extension: String): Boolean {
    return extension in listOf("mp4", "mkv", "3gp", "webm", "mp3", "m4a", "wav", "aac", "ogg")
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}
