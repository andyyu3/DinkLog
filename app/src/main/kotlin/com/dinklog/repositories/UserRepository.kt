package com.dinklog.repositories

import com.dinklog.models.User
import com.dinklog.models.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class UserRepository(private val userDao: UserDao) {

    suspend fun login(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            val user = userDao.findByEmail(email)
                ?: return@withContext Result.failure(Exception("No account found with that email"))

            val hash = hashPassword(password)
            if (user.passwordHash != hash) {
                return@withContext Result.failure(Exception("Incorrect password"))
            }

            Result.success(user)
        }

    suspend fun signup(email: String, name: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            if (userDao.emailExists(email)) {
                return@withContext Result.failure(Exception("Email already in use"))
            }

            val user = User(
                email = email,
                name = name,
                passwordHash = hashPassword(password)
            )
            val userId = userDao.insert(user)

            Result.success(user.copy(userId = userId))
        }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
