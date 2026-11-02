package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.example.data.local.AudioTrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val ambientGenerator = AmbientAudioGenerator()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _currentTrack = MutableStateFlow<AudioTrackEntity?>(null)
    val currentTrack: StateFlow<AudioTrackEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _activeAmbient = MutableStateFlow<AmbientAudioGenerator.AmbientType?>(null)
    val activeAmbient: StateFlow<AmbientAudioGenerator.AmbientType?> = _activeAmbient.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var currentPlaylist: List<AudioTrackEntity> = emptyList()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
        }
    }

    init {
        // Tracker is started on-demand only when audio playback begins
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    fun playTrack(track: AudioTrackEntity, playlist: List<AudioTrackEntity>) {
        // Stop any active ambient sound
        stopAmbient()

        currentPlaylist = playlist
        _currentTrack.value = track

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        requestAudioFocus()

        try {
            val uri = Uri.parse(track.uriString)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = mp.duration.toLong()
                    startProgressTracker()
                }
                setOnCompletionListener {
                    if (_isRepeat.value) {
                        it.start()
                    } else {
                        playNext()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    progressJob?.cancel()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun playAmbient(type: AmbientAudioGenerator.AmbientType) {
        // Stop device media player
        pause()
        _currentTrack.value = AudioTrackEntity(
            id = "ambient_" + type.name,
            title = type.displayName,
            artist = type.description,
            playlist = "Nature",
            isBuiltIn = true
        )
        ambientGenerator.start(type)
        _activeAmbient.value = type
        _isPlaying.value = true
        _duration.value = 0L
        _currentPosition.value = 0L
    }

    fun stopAmbient() {
        if (_activeAmbient.value != null) {
            ambientGenerator.stop()
            _activeAmbient.value = null
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        if (_activeAmbient.value != null) {
            if (ambientGenerator.isPlaying) {
                ambientGenerator.stop()
                _isPlaying.value = false
            } else {
                ambientGenerator.start(_activeAmbient.value!!)
                _isPlaying.value = true
            }
            return
        }

        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            } else {
                requestAudioFocus()
                mp.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        }
    }

    fun pause() {
        if (_activeAmbient.value != null) {
            ambientGenerator.stop()
            _isPlaying.value = false
            return
        }

        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        stopAmbient()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        abandonAudioFocus()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            mp.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    fun playNext() {
        if (currentPlaylist.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = currentPlaylist.indexOfFirst { it.id == current?.id }

        val nextIndex = if (_isShuffle.value) {
            currentPlaylist.indices.random()
        } else {
            if (currentIndex in 0 until currentPlaylist.size - 1) currentIndex + 1 else 0
        }
        playTrack(currentPlaylist[nextIndex], currentPlaylist)
    }

    fun playPrevious() {
        if (currentPlaylist.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = currentPlaylist.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentPlaylist.size - 1
        playTrack(currentPlaylist[prevIndex], currentPlaylist)
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()

        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60_000L)
                    remaining -= 1
                    _sleepTimerMinutes.value = remaining
                }
                // Sleep timer finished -> pause
                pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    private fun startProgressTracker() {
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (_isPlaying.value && mp.isPlaying) {
                        _currentPosition.value = mp.currentPosition.toLong()
                        _duration.value = mp.duration.toLong()
                    }
                }
                delay(1000L)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        stop()
    }
}
