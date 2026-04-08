package com.mpt.masterpasswordtrainer.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Thrown when the Keystore key has been permanently invalidated
 * (e.g., user changed device lock settings). All encrypted data is unrecoverable.
 */
class KeyInvalidatedException(cause: Throwable? = null) :
    Exception("Security key invalidated. Encrypted data must be reset.", cause)

object KeystoreManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "mpt_master_key"

    /**
     * Retrieves the existing AES-256-GCM key from Android Keystore, or creates one if none exists.
     * @throws KeyInvalidatedException if the key was permanently invalidated (e.g., device lock changed).
     */
    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        try {
            keyStore.getEntry(KEY_ALIAS, null)?.let { entry ->
                return (entry as KeyStore.SecretKeyEntry).secretKey
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw KeyInvalidatedException(e)
        } catch (e: UnrecoverableKeyException) {
            throw KeyInvalidatedException(e)
        }

        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val specBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        // Prefer StrongBox (hardware security module) on supported devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                specBuilder.setIsStrongBoxBacked(true)
            } catch (_: Exception) {
                // StrongBox not available, fall back to TEE-backed key
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        try {
            keyGenerator.init(specBuilder.build())
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            // If StrongBox init failed, retry without it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val fallbackSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(fallbackSpec)
                return keyGenerator.generateKey()
            }
            throw e
        }
    }

    fun keyExists(): Boolean {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.containsAlias(KEY_ALIAS)
    }

    fun deleteKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}
