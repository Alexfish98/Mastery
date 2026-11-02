package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FocusSession
import com.example.data.repository.IqaiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 5. LIFECYCLE-AWARE FOCUS TIMER (STRICT MODE)
 * Coroutine-driven StateFlow countdown with strict background penalty logging.
 */
class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = IqaiRepository(
        textbookDao = db.textbookDao(),
        bookmarkDao = db.bookmarkDao(),
        noteDao = db.offlineNoteDao(),
        focusDao = db.iqaiFocusSessionDao(),
        chatDao = db.chatHistoryDao()
    )

    private val _timeRemainingSeconds = MutableStateFlow(25 * 60)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _initialDurationMinutes = MutableStateFlow(25)
    val initialDurationMinutes: StateFlow<Int> = _initialDurationMinutes.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isPenaltyActive = MutableStateFlow(false)
    val isPenaltyActive: StateFlow<Boolean> = _isPenaltyActive.asStateFlow()

    private val _penaltyMessage = MutableStateFlow("")
    val penaltyMessage: StateFlow<String> = _penaltyMessage.asStateFlow()

    private val _sessionCompletedSuccess = MutableStateFlow(false)
    val sessionCompletedSuccess: StateFlow<Boolean> = _sessionCompletedSuccess.asStateFlow()

    private var countdownJob: Job? = null
    private var sessionStartTime: Long = 0L

    val successfulSessions: StateFlow<List<FocusSession>> = repository.successfulSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedSessions: StateFlow<List<FocusSession>> = repository.failedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val successfulCount: StateFlow<Int> = repository.successfulCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = repository.failedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setDuration(minutes: Int) {
        if (!_isTimerRunning.value) {
            _initialDurationMinutes.value = minutes
            _timeRemainingSeconds.value = minutes * 60
        }
    }

    fun startTimer() {
        if (_isTimerRunning.value) return

        _isTimerRunning.value = true
        _isPenaltyActive.value = false
        _sessionCompletedSuccess.value = false
        sessionStartTime = System.currentTimeMillis()

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                _timeRemainingSeconds.value -= 1
            }

            if (_timeRemainingSeconds.value <= 0) {
                // Completed successfully!
                _isTimerRunning.value = false
                _sessionCompletedSuccess.value = true
                repository.logFocusSession(
                    durationMinutes = _initialDurationMinutes.value,
                    wasSuccessful = true
                )
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        countdownJob?.cancel()
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        countdownJob?.cancel()
        _timeRemainingSeconds.value = _initialDurationMinutes.value * 60
        _sessionCompletedSuccess.value = false
    }

    /**
     * Strictly called when the application transitions to ON_STOP while the timer is running.
     * Cancels the countdown, flags wasSuccessful = false, logs the failure, and triggers penalty UI.
     */
    fun handleAppMinimizedPenalty() {
        if (!_isTimerRunning.value) return

        countdownJob?.cancel()
        _isTimerRunning.value = false

        val elapsedMillis = System.currentTimeMillis() - sessionStartTime
        val elapsedMinutes = maxOf(1, (elapsedMillis / (60 * 1000)).toInt())

        viewModelScope.launch {
            // Save failure record to Room database
            repository.logFocusSession(
                durationMinutes = elapsedMinutes,
                wasSuccessful = false
            )
        }

        _penaltyMessage.value = "Strict Mode Violated: You minimized or exited Mastery during an active focus block of ${_initialDurationMinutes.value} minutes. A failure penalty has been recorded to your history."
        _isPenaltyActive.value = true
        _timeRemainingSeconds.value = _initialDurationMinutes.value * 60
    }

    fun dismissPenalty() {
        _isPenaltyActive.value = false
        _penaltyMessage.value = ""
    }

    fun dismissSuccess() {
        _sessionCompletedSuccess.value = false
        resetTimer()
    }
}
