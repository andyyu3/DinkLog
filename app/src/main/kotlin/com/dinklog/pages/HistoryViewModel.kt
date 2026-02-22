package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinklog.DinkLogApp
import com.dinklog.models.Match
import com.dinklog.repositories.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    private val matchRepository = MatchRepository(DinkLogApp.getDatabase().matchDao())

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches

    fun loadMatches(userId: Long) {
        viewModelScope.launch {
            matchRepository.getMatchesForUser(userId).collectLatest { matchList ->
                _matches.value = matchList
            }
        }
    }
}