package com.dinklog.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dinklog.utils.UserColumns

@Entity(
    tableName = UserColumns.TABLE_NAME,
    indices = [Index(value = [UserColumns.EMAIL], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = UserColumns.ID)
    val userId: Long = 0,

    @ColumnInfo(name = UserColumns.EMAIL)
    val email: String,

    @ColumnInfo(name = UserColumns.NAME)
    val name: String,

    @ColumnInfo(name = UserColumns.PASSWORD_HASH)
    val passwordHash: String,

    @ColumnInfo(name = UserColumns.CREATED_AT)
    val createdAt: Long = System.currentTimeMillis()
)
