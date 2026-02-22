package com.dinklog.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dinklog.utils.UserColumns

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM ${UserColumns.TABLE_NAME} WHERE ${UserColumns.EMAIL} = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM ${UserColumns.TABLE_NAME} WHERE ${UserColumns.ID} = :userId LIMIT 1")
    suspend fun findById(userId: Long): User?

    @Query("SELECT COUNT(*) FROM ${UserColumns.TABLE_NAME} WHERE ${UserColumns.EMAIL} = :email")
    suspend fun emailExists(email: String): Boolean
}
