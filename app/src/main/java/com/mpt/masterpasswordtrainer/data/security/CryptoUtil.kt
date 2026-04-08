package com.mpt.masterpasswordtrainer.data.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoUtil {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    /** Encrypts [plaintext] with AES-256-GCM. Returns (ciphertext, IV) as Base64-encoded strings. */
    fun encrypt(plaintext: String, key: SecretKey): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        return Pair(ciphertextBase64, ivBase64)
    }

    /** Decrypts Base64-encoded [ciphertext] using the given [iv] and [key]. */
    fun decrypt(ciphertext: String, iv: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val ciphertextBytes = Base64.decode(ciphertext, Base64.NO_WRAP)
        val plaintext = cipher.doFinal(ciphertextBytes)

        return String(plaintext, Charsets.UTF_8)
    }
}
