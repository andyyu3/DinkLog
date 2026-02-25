package com.dinklog.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.dinklog.utils.MatchColumns
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Insert
    suspend fun insert(match: Match): Long

    @Delete
    suspend fun delete(match: Match)

    @Query(
        "SELECT * FROM ${MatchColumns.TABLE_NAME} " +
            "WHERE ${MatchColumns.USER_ID} = :userId " +
            "ORDER BY ${MatchColumns.DATE} DESC"
    )
    fun getMatchesByUser(userId: Long): Flow<List<Match>>

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
