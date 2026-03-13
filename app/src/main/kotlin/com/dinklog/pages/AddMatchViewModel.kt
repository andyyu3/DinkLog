package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinklog.DinkLogApp
import com.dinklog.models.Match
import com.dinklog.repositories.MatchRepository
import com.dinklog.utils.PreferencesManager
import com.dinklog.utils.ValidationConstants
import com.dinklog.utils.ValidationMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddMatchViewModel : ViewModel() {

    private val matchRepository = MatchRepository(DinkLogApp.getDatabase().matchDao())
    private val preferencesManager = PreferencesManager(DinkLogApp.instance)

    private val _state = MutableStateFlow<SaveState>(SaveState.Idle)
    val state: StateFlow<SaveState> = _state

    private val _match = MutableStateFlow<Match?>(null)
    val match: StateFlow<Match?> = _match

    fun loadMatch(matchId: Long) {
        viewModelScope.launch {
            _match.value = matchRepository.getMatchById(matchId)
        }
    }

    fun editMatch(
        opponent1: String,
        opponent2: String,
        partner: String,
        teamScore: String,
        opponentScore: String,
        location: String,
        notes: String
    ) {
        val existing = _match.value ?: return
        val validationError = validateInput(opponent1, teamScore, opponentScore, location)
        if (validationError != null) {
            _state.value = SaveState.Error(validationError)
            return
        }

        val updated = existing.copy(
            opponent1Name = opponent1.trim(),
            opponent2Name = opponent2.trim().ifBlank { null },
            partnerName = partner.trim().ifBlank { null },
            teamScore = teamScore.toInt(),
            opponentScore = opponentScore.toInt(),
            locationName = location.trim(),
            notes = notes.trim().ifBlank { null }
        )

        _state.value = SaveState.Saving
        viewModelScope.launch {
            matchRepository.updateMatch(updated)
                .onSuccess { _state.value = SaveState.Success }
                .onFailure { error ->
                    _state.value = SaveState.Error(error.message ?: ValidationMessages.UPDATE_FAILED)
                }
        }
    }

    fun saveMatch(
        opponent1: String,
        opponent2: String,
        partner: String,
        teamScore: String,
        opponentScore: String,
        location: String,
        notes: String
    ) {
        val userId = preferencesManager.getUserId() ?: return
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
                .onFailure { error ->
                    _state.value = SaveState.Error(error.message ?: ValidationMessages.SAVE_FAILED)
                }
        }
    }

    private fun validateInput(
        opponent1: String,
        teamScore: String,
        opponentScore: String,
        location: String
    ): String? = when {
        opponent1.isBlank() -> ValidationMessages.OPPONENT1_REQUIRED
        teamScore.isBlank() || opponentScore.isBlank() -> ValidationMessages.SCORES_REQUIRED
        teamScore.toIntOrNull() == null || opponentScore.toIntOrNull() == null -> ValidationMessages.SCORES_NOT_NUMBERS
        teamScore.toInt() < ValidationConstants.MIN_SCORE || opponentScore.toInt() < ValidationConstants.MIN_SCORE -> ValidationMessages.SCORES_NEGATIVE
        location.isBlank() -> ValidationMessages.LOCATION_REQUIRED
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
