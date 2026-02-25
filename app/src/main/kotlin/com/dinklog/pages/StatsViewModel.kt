package com.dinklog.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinklog.DinkLogApp
import com.dinklog.repositories.MatchRepository
import com.dinklog.repositories.MatchStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StatsViewModel : ViewModel() {

    private val matchRepository = MatchRepository(DinkLogApp.getDatabase().matchDao())

    private val _stats = MutableStateFlow<MatchStats?>(null)
    val stats: StateFlow<MatchStats?> = _stats

    fun loadStats(userId: Long) {
        viewModelScope.launch {
            _stats.value = matchRepository.getStats(userId)
        }
    }
}
