package com.dinklog.repositories

import com.dinklog.models.User
import com.dinklog.models.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserRepository(private val userDao: UserDao) {

    suspend fun login(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            val user = userDao.findByEmail(email)
                ?: return@withContext Result.failure(Exception("No account found with that email"))

            if (!verifyPassword(password, user.passwordHash)) {
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
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2(password, salt)
        return salt.toHex() + SEPARATOR + hash.toHex()
    }

    private fun verifyPassword(password: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) return false
        val salt = parts[0].hexToBytes()
        val expectedHash = parts[1].hexToBytes()
        val actualHash = pbkdf2(password, salt)
        return actualHash.contentEquals(expectedHash)
    }

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
        private const val SEPARATOR = ":"
    }
}
