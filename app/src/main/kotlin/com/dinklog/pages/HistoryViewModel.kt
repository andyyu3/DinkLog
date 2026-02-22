package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinklog.DinkLogApp
import com.dinklog.models.Match
import com.dinklog.repositories.MatchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel : ViewModel() {

    private val matchRepository = MatchRepository(DinkLogApp.getDatabase().matchDao())

    private val _userId = MutableStateFlow<Long?>(null)

    val matches: StateFlow<List<Match>> = _userId
        .flatMapLatest { userId ->
            if (userId != null) matchRepository.getMatchesForUser(userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadMatches(userId: Long) {
        _userId.value = userId
    }
}
