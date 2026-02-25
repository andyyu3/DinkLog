package com.dinklog.repositories

import com.dinklog.models.Match
import com.dinklog.models.MatchDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MatchRepository(private val matchDao: MatchDao) {

    fun getMatchesForUser(userId: Long): Flow<List<Match>> =
        matchDao.getMatchesByUser(userId)

    suspend fun addMatch(
        userId: Long,
        opponent1Name: String,
        opponent2Name: String?,
        partnerName: String?,
        teamScore: Int,
        opponentScore: Int,
        locationName: String,
        notes: String?
    ): Result<Match> = withContext(Dispatchers.IO) {
        val match = Match(
            userId = userId,
            opponent1Name = opponent1Name,
            opponent2Name = opponent2Name,
            partnerName = partnerName,
            teamScore = teamScore,
            opponentScore = opponentScore,
            isWin = teamScore > opponentScore,
            locationName = locationName,
            notes = notes
        )
        val matchId = matchDao.insert(match)
        Result.success(match.copy(matchId = matchId))
    }

    suspend fun deleteMatch(match: Match) = withContext(Dispatchers.IO) {
        matchDao.delete(match)
    }

    suspend fun getStats(userId: Long): MatchStats = withContext(Dispatchers.IO) {
        val total = matchDao.getTotalMatches(userId)
        val wins = matchDao.getWinCount(userId)
        val losses = matchDao.getLossCount(userId)
        MatchStats(
            totalMatches = total,
            wins = wins,
            losses = losses
        )
    }
}

data class MatchStats(
    val totalMatches: Int,
    val wins: Int,
    val losses: Int
) {
    val winRate: Double
        get() = if (totalMatches > 0) wins.toDouble() / totalMatches * 100 else 0.0
}
