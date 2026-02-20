package com.dinklog.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dinklog.utils.MatchColumns
import com.dinklog.utils.UserColumns

@Entity(
    tableName = MatchColumns.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = [UserColumns.ID],
            childColumns = [MatchColumns.USER_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = [MatchColumns.USER_ID])]
)
data class Match(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = MatchColumns.ID)
    val matchId: Long = 0,

    @ColumnInfo(name = MatchColumns.USER_ID)
    val userId: Long,

    @ColumnInfo(name = MatchColumns.DATE)
    val matchDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = MatchColumns.PARTNER_NAME)
    val partnerName: String? = null,

    @ColumnInfo(name = MatchColumns.OPPONENT1_NAME)
    val opponent1Name: String,

    @ColumnInfo(name = MatchColumns.OPPONENT2_NAME)
    val opponent2Name: String? = null,

    @ColumnInfo(name = MatchColumns.TEAM_SCORE)
    val teamScore: Int,

    @ColumnInfo(name = MatchColumns.OPPONENT_SCORE)
    val opponentScore: Int,

    @ColumnInfo(name = MatchColumns.IS_WIN)
    val isWin: Boolean,

    @ColumnInfo(name = MatchColumns.LOCATION_NAME)
    val locationName: String,

    @ColumnInfo(name = MatchColumns.NOTES)
    val notes: String? = null
)
