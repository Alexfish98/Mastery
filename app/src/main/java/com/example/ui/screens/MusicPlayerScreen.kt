package com.example.ui.screens

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AudioTrackEntity
import com.example.player.AmbientAudioGenerator
import com.example.viewmodel.ChemistryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: ChemistryViewModel
) {
    val context = LocalContext.current
    val player = viewModel.musicPlayerManager
    val tracks by viewModel.audioTracks.collectAsState()

    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val currentPosition by player.currentPosition.collectAsState()
    val duration by player.duration.collectAsState()
    val activeAmbient by player.activeAmbient.collectAsState()
    val isRepeat by player.isRepeat.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val sleepTimerMinutes by player.sleepTimerMinutes.collectAsState()

    var selectedPlaylist by remember { mutableStateOf("All") }
    val playlists = listOf("All", "Lo-fi", "Instrumental", "Classical", "Nature", "My Music")

    var showPlaylistAssignmentDialog by remember { mutableStateOf<AudioTrackEntity?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // SAF Audio Picker Launcher
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Retrieve track title and artist
                var title = "Audio Track"
                var artist = "Unknown Artist"
                var trackDuration = 0L

                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                        if (it.isNotBlank()) title = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                        if (it.isNotBlank()) artist = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                        trackDuration = it
                    }
                    retriever.release()
                } catch (e: Exception) {
                    // Fallback to OpenableColumns display name
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            title = cursor.getString(nameIndex)
                        }
                    }
                }

                viewModel.addAudioTrack(
                    title = title,
                    artist = artist,
                    uriString = uri.toString(),
                    durationMs = trackDuration,
                    playlist = "My Music"
                )
            }
            Toast.makeText(context, "Imported ${uris.size} tracks from phone", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredTracks = if (selectedPlaylist == "All") {
        tracks
    } else {
        tracks.filter { it.playlist.equals(selectedPlaylist, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                text = { Text("Import Music") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Now Playing Hero Player Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeAmbient != null) "OFFLINE SOUNDSCAPE" else "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Sleep timer chip
                        AssistChip(
                            onClick = { showSleepTimerDialog = true },
                            label = {
                                Text(
                                    text = if (sleepTimerMinutes != null) "⏱ ${sleepTimerMinutes}m" else "Sleep Timer",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = currentTrack?.title ?: "Select a track or ambient sound",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentTrack?.artist ?: "EUEE Mastery Study Audio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    // Progress Slider for Media Tracks
                    if (activeAmbient == null && duration > 0L) {
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { player.seekTo(it.toLong()) },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatDuration(currentPosition), style = MaterialTheme.typography.labelSmall)
                            Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Playback Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { player.toggleShuffle() }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { player.playPrevious() },
                            enabled = activeAmbient == null
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                        }

                        FloatingActionButton(
                            onClick = { player.togglePlayPause() },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        IconButton(
                            onClick = { player.playNext() },
                            enabled = activeAmbient == null
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next")
                        }

                        IconButton(onClick = { player.toggleRepeat() }) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Playlist tabs
            ScrollableTabRow(
                selectedTabIndex = playlists.indexOf(selectedPlaylist).coerceAtLeast(0),
                edgePadding = 0.dp,
                divider = {}
            ) {
                playlists.forEach { pl ->
                    Tab(
                        selected = selectedPlaylist == pl,
                        onClick = { selectedPlaylist = pl },
                        text = { Text(pl, fontWeight = if (selectedPlaylist == pl) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (selectedPlaylist == "Nature") {
                // Show built-in ambient soundscapes
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AmbientSoundCard(
                        title = "🌧️ Calm Rainfall",
                        desc = "Gentle rain noise for masking room distractions",
                        isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.RAIN && isPlaying,
                        onClick = { player.playAmbient(AmbientAudioGenerator.AmbientType.RAIN) }
                    )
                    AmbientSoundCard(
                        title = "🧠 Alpha Concentration Waves (10Hz)",
                        desc = "Stereo binaural beats designed to induce calm mental focus",
                        isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.ALPHA_WAVES && isPlaying,
                        onClick = { player.playAmbient(AmbientAudioGenerator.AmbientType.ALPHA_WAVES) }
                    )
                    AmbientSoundCard(
                        title = "📻 Deep Brown Noise",
                        desc = "Acoustic low frequencies to soothe racing thoughts",
                        isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.BROWN_NOISE && isPlaying,
                        onClick = { player.playAmbient(AmbientAudioGenerator.AmbientType.BROWN_NOISE) }
                    )
                    AmbientSoundCard(
                        title = "🌊 Forest Stream",
                        desc = "Tranquil water flow soundscape",
                        isSelected = activeAmbient == AmbientAudioGenerator.AmbientType.FOREST_STREAM && isPlaying,
                        onClick = { player.playAmbient(AmbientAudioGenerator.AmbientType.FOREST_STREAM) }
                    )
                }
            } else {
                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("No tracks in '$selectedPlaylist'", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Import instrumental study tracks or lo-fi audio directly from your phone's storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Select Audio Files")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isCurrent = currentTrack?.id == track.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { player.playTrack(track, filteredTracks) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.Equalizer else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text("${track.artist} • ${track.playlist}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(onClick = { showPlaylistAssignmentDialog = track }) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Change Playlist")
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteAudioTrack(track.id)
                                            Toast.makeText(context, "Track removed", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playlist Assignment Dialog
        showPlaylistAssignmentDialog?.let { track ->
            AlertDialog(
                onDismissRequest = { showPlaylistAssignmentDialog = null },
                title = { Text("Assign Playlist") },
                text = {
                    Column {
                        listOf("Lo-fi", "Instrumental", "Classical", "Nature", "My Music").forEach { pl ->
                            ListItem(
                                headlineContent = { Text(pl) },
                                leadingContent = {
                                    RadioButton(
                                        selected = track.playlist == pl,
                                        onClick = {
                                            viewModel.updateAudioTrackPlaylist(track.id, pl)
                                            showPlaylistAssignmentDialog = null
                                            Toast.makeText(context, "Assigned to $pl", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.updateAudioTrackPlaylist(track.id, pl)
                                    showPlaylistAssignmentDialog = null
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaylistAssignmentDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Music Sleep Timer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            Button(
                                onClick = {
                                    player.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                    Toast.makeText(context, "Music will pause in $mins minutes", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$mins Minutes")
                            }
                        }
                        if (sleepTimerMinutes != null) {
                            OutlinedButton(
                                onClick = {
                                    player.setSleepTimer(null)
                                    showSleepTimerDialog = false
                                    Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Turn Off Sleep Timer")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AmbientSoundCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onClick) {
                Text(if (isSelected) "Playing" else "Play")
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
