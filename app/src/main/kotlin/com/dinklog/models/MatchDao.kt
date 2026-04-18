package com.dinklog.models

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dinklog.utils.MatchColumns

@Dao
interface MatchDao {

    @Insert
    suspend fun insert(match: Match): Long

    @Insert
    suspend fun insertAll(matches: List<Match>)

    @Update
    suspend fun update(match: Match)

    @Delete
    suspend fun delete(match: Match)

    @Query(
        "SELECT * FROM ${MatchColumns.TABLE_NAME} " +
            "WHERE ${MatchColumns.USER_ID} = :userId " +
            "ORDER BY ${MatchColumns.DATE} DESC"
    )
    fun getMatchesByUserPaged(userId: Long): PagingSource<Int, Match>

    @Query(
        "SELECT * FROM ${MatchColumns.TABLE_NAME} " +
            "WHERE ${MatchColumns.ID} = :matchId LIMIT 1"
    )
    suspend fun getMatchById(matchId: Long): Match?

    @Query("SELECT COUNT(*) FROM ${MatchColumns.TABLE_NAME} WHERE ${MatchColumns.USER_ID} = :userId")
    suspend fun getTotalMatches(userId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM ${MatchColumns.TABLE_NAME} " +
            "WHERE ${MatchColumns.USER_ID} = :userId AND ${MatchColumns.IS_WIN} = 1"
    )
    suspend fun getWinCount(userId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM ${MatchColumns.TABLE_NAME} " +
            "WHERE ${MatchColumns.USER_ID} = :userId AND ${MatchColumns.IS_WIN} = 0"
    )
    suspend fun getLossCount(userId: Long): Int
}
