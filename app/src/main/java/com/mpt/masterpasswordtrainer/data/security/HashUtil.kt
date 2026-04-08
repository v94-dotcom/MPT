package com.mpt.masterpasswordtrainer.data.security

import android.util.Base64
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom

object HashUtil {

    private const val SALT_LENGTH = 32
    private const val HASH_LENGTH = 32
    private const val TIME_COST = 3
    private const val MEMORY_COST = 65536 // 64 MB
    private const val PARALLELISM = 4

    private val argon2 = Argon2Kt()

    /** Generates a 32-byte cryptographically random salt, returned as Base64. */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /** Hashes [password] with Argon2id using the given [salt]. Zeros the password array after use. */
    fun hashPassword(password: CharArray, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val passwordBytes = charArrayToBytes(password)

        try {
            val result = argon2.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passwordBytes,
                salt = saltBytes,
                tCostInIterations = TIME_COST,
                mCostInKibibyte = MEMORY_COST,
                parallelism = PARALLELISM,
                hashLengthInBytes = HASH_LENGTH
            )

            return Base64.encodeToString(result.rawHashAsByteArray(), Base64.NO_WRAP)
        } finally {
            passwordBytes.fill(0)
            password.fill('\u0000')
        }
    }

    /** Verifies [password] against [expectedHash] using the stored [salt]. Zeros the password array after use. */
    fun verifyPassword(password: CharArray, salt: String, expectedHash: String): Boolean {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val expectedHashBytes = Base64.decode(expectedHash, Base64.NO_WRAP)
        val passwordBytes = charArrayToBytes(password)

        try {
            val result = argon2.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passwordBytes,
                salt = saltBytes,
                tCostInIterations = TIME_COST,
                mCostInKibibyte = MEMORY_COST,
                parallelism = PARALLELISM,
                hashLengthInBytes = HASH_LENGTH
            )

            val computedHash = result.rawHashAsByteArray()
            return constantTimeEquals(computedHash, expectedHashBytes)
        } finally {
            passwordBytes.fill(0)
            password.fill('\u0000')
        }
    }

    private fun charArrayToBytes(chars: CharArray): ByteArray {
        val bytes = ByteArray(chars.size * 2)
        for (i in chars.indices) {
            bytes[i * 2] = (chars[i].code shr 8).toByte()
            bytes[i * 2 + 1] = (chars[i].code and 0xFF).toByte()
        }
        return bytes
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
