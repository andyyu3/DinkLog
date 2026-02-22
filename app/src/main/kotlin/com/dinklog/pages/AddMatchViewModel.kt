package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinklog.DinkLogApp
import com.dinklog.repositories.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddMatchViewModel : ViewModel() {

    private val matchRepository = MatchRepository(DinkLogApp.getDatabase().matchDao())

    private val _state = MutableStateFlow<SaveState>(SaveState.Idle)
    val state: StateFlow<SaveState> = _state

    fun saveMatch(
        userId: Long,
        opponent1: String,
        opponent2: String,
        partner: String,
        teamScore: String,
        opponentScore: String,
        location: String,
        notes: String
    ) {
        val validationError = validateInput(opponent1, teamScore, opponentScore, location)
        if (validationError != null) {
            _state.value = SaveState.Error(validationError)
            return
        }

        _state.value = SaveState.Saving
        viewModelScope.launch {
            matchRepository.addMatch(
                userId = userId,
                opponent1Name = opponent1.trim(),
                opponent2Name = opponent2.trim().ifBlank { null },
                partnerName = partner.trim().ifBlank { null },
                teamScore = teamScore.toInt(),
                opponentScore = opponentScore.toInt(),
                locationName = location.trim(),
                notes = notes.trim().ifBlank { null }
            )
                .onSuccess { _state.value = SaveState.Success }
                .onFailure { error -> _state.value = SaveState.Error(error.message ?: "Save failed") }
        }
    }

    private fun validateInput(
        opponent1: String,
        teamScore: String,
        opponentScore: String,
        location: String
    ): String? = when {
        opponent1.isBlank() -> "Opponent 1 name is required"
        teamScore.isBlank() || opponentScore.isBlank() -> "Both scores are required"
        teamScore.toIntOrNull() == null || opponentScore.toIntOrNull() == null -> "Scores must be numbers"
        teamScore.toInt() < 0 || opponentScore.toInt() < 0 -> "Scores cannot be negative"
        location.isBlank() -> "Location is required"
        else -> null
    }

    fun resetState() {
        _state.value = SaveState.Idle
    }
}

sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data object Success : SaveState()
    data class Error(val message: String) : SaveState()
}
