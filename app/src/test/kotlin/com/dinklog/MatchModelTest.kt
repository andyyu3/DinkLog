package com.dinklog

import com.dinklog.models.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchModelTest {

    private fun buildMatch(
        teamScore: Int = 11,
        opponentScore: Int = 5,
        isWin: Boolean = true
    ) = Match(
        userId = 1L,
        opponent1Name = "Alice",
        teamScore = teamScore,
        opponentScore = opponentScore,
        isWin = isWin,
        locationName = "Test Courts"
    )

    @Test
    fun match_defaultMatchId_isZero() {
        val match = buildMatch()
        assertEquals(0L, match.matchId)
    }

    @Test
    fun match_optionalFields_defaultToNull() {
        val match = buildMatch()
        assertNull(match.partnerName)
        assertNull(match.opponent2Name)
        assertNull(match.notes)
    }

    @Test
    fun match_copy_updatesOpponentScore() {
        val original = buildMatch(teamScore = 11, opponentScore = 5, isWin = true)
        val updated = original.copy(opponentScore = 11, teamScore = 9, isWin = false)
        assertEquals(11, updated.opponentScore)
        assertEquals(9, updated.teamScore)
        assertTrue(!updated.isWin)
    }
}
