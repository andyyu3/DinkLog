package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dinklog.DinkLogApp
import com.dinklog.models.Match
import com.dinklog.repositories.MatchRepository
import com.dinklog.utils.PreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel : ViewModel() {

    private val matchRepository = MatchRepository(DinkLogApp.getDatabase().matchDao())
    private val preferencesManager = PreferencesManager(DinkLogApp.instance)

    private val _userId = MutableStateFlow(preferencesManager.getUserId())

    val matches: Flow<PagingData<Match>> = _userId
        .flatMapLatest { userId ->
            if (userId != null) matchRepository.getPagedMatchesForUser(userId)
            else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    fun loadMatches() {
        _userId.value = preferencesManager.getUserId()
    }

    fun deleteMatch(match: Match) {
        viewModelScope.launch {
            matchRepository.deleteMatch(match)
                .onFailure { /* deletion failed silently; list will not update */ }
        }
    }

    fun undoDelete(match: Match) {
        viewModelScope.launch {
            matchRepository.addMatch(
                userId = match.userId,
                opponent1Name = match.opponent1Name,
                opponent2Name = match.opponent2Name,
                partnerName = match.partnerName,
                teamScore = match.teamScore,
                opponentScore = match.opponentScore,
                locationName = match.locationName,
                notes = match.notes
            )
        }
    }

    fun seedDebugMatches(onComplete: (Int) -> Unit) {
        val userId = preferencesManager.getUserId() ?: return
        viewModelScope.launch {
            matchRepository.seedDummyMatches(userId)
                .onSuccess { onComplete(it) }
        }
    }
}
