package com.dinklog.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.dinklog.models.Match
import com.dinklog.models.MatchDao
import com.dinklog.utils.DebugSeedConstants
import com.dinklog.utils.PagingConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MatchRepository(private val matchDao: MatchDao) {

    fun getPagedMatchesForUser(userId: Long): Flow<PagingData<Match>> =
        Pager(
            config = PagingConfig(
                pageSize = PagingConstants.PAGE_SIZE,
                prefetchDistance = PagingConstants.PREFETCH_DISTANCE,
                initialLoadSize = PagingConstants.INITIAL_LOAD_SIZE,
                maxSize = PagingConstants.MAX_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { matchDao.getMatchesByUserPaged(userId) }
        ).flow

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

    suspend fun getMatchById(matchId: Long): Match? = withContext(Dispatchers.IO) {
        matchDao.getMatchById(matchId)
    }

    suspend fun updateMatch(match: Match): Result<Unit> = withContext(Dispatchers.IO) {
        val updated = match.copy(isWin = match.teamScore > match.opponentScore)
        matchDao.update(updated)
        Result.success(Unit)
    }

    suspend fun deleteMatch(match: Match): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { matchDao.delete(match) }
    }

    suspend fun seedDummyMatches(
        userId: Long,
        count: Int = DebugSeedConstants.DEFAULT_SEED_COUNT
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val matches = List(count) { index -> buildSampleMatch(userId, index) }
            matchDao.insertAll(matches)
            matches.size
        }
    }

    private fun buildSampleMatch(userId: Long, indexFromNewest: Int): Match {
        val team = (DebugSeedConstants.MIN_SCORE..DebugSeedConstants.MAX_SCORE).random()
        val opp = (DebugSeedConstants.MIN_SCORE - 5..DebugSeedConstants.MAX_SCORE - 3).random()
        val timestamp = System.currentTimeMillis() -
            indexFromNewest.toLong() * DebugSeedConstants.HALF_DAY_MS
        return Match(
            userId = userId,
            matchDate = timestamp,
            opponent1Name = DebugSeedConstants.SAMPLE_NAMES.random(),
            opponent2Name = if (indexFromNewest % 3 == 0) DebugSeedConstants.SAMPLE_NAMES.random() else null,
            partnerName = if (indexFromNewest % 2 == 0) DebugSeedConstants.SAMPLE_NAMES.random() else null,
            teamScore = team,
            opponentScore = opp.coerceAtLeast(0),
            isWin = team > opp,
            locationName = DebugSeedConstants.SAMPLE_LOCATIONS.random(),
            notes = null
        )
    }

    suspend fun getStats(userId: Long): Result<MatchStats> = withContext(Dispatchers.IO) {
        runCatching {
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
}

data class MatchStats(
    val totalMatches: Int,
    val wins: Int,
    val losses: Int
) {
    val winRate: Double
        get() = if (totalMatches > 0) wins.toDouble() / totalMatches * 100 else 0.0
}
