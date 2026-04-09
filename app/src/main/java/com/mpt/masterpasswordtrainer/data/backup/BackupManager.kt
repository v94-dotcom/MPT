package com.mpt.masterpasswordtrainer.data.backup

import android.content.Context
import com.mpt.masterpasswordtrainer.data.model.BackupData
import com.mpt.masterpasswordtrainer.data.model.BackupEntry
import com.mpt.masterpasswordtrainer.data.model.BackupPasswordVersion
import com.mpt.masterpasswordtrainer.data.model.BackupSettings
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import com.mpt.masterpasswordtrainer.data.model.PasswordVersion
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import com.mpt.masterpasswordtrainer.data.security.CryptoUtil
import com.mpt.masterpasswordtrainer.data.security.KeystoreManager
import com.mpt.masterpasswordtrainer.ui.screens.settings.SettingsViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles encrypted backup export and import.
 *
 * Backup file format: [32 bytes salt][12 bytes IV][AES-256-GCM encrypted JSON]
 *
 * The JSON payload contains all entries (with plaintext emails), verification history,
 * and app settings. The backup password is used to derive an AES-256 key via PBKDF2.
 */
object BackupManager {

    private const val SALT_LENGTH = 32
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH = 256

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Creates an encrypted backup of all app data.
     *
     * @return the encrypted backup as a ByteArray, or null if export fails
     */
    fun exportBackup(context: Context, backupPassword: CharArray): ByteArray? {
        return try {
            val repository = PasswordRepository(context)
            val prefs = context.getSharedPreferences("mpt_prefs", Context.MODE_PRIVATE)
            val keystoreKey = KeystoreManager.getOrCreateKey()

            // Build backup entries with decrypted (plaintext) emails
            val entries = repository.getAllEntries()
            val backupEntries = entries.map { entry ->
                val plaintextEmail = if (entry.encryptedEmail.isNotEmpty()) {
                    try {
                        CryptoUtil.decrypt(entry.encryptedEmail, entry.emailIV, keystoreKey)
                    } catch (_: Exception) {
                        ""
                    }
                } else {
                    ""
                }

                BackupEntry(
                    id = entry.id,
                    serviceName = entry.serviceName,
                    serviceColor = entry.serviceColor,
                    serviceIcon = entry.serviceIcon,
                    email = plaintextEmail,
                    passwordVersions = entry.passwordVersions.map { v ->
                        BackupPasswordVersion(
                            id = v.id,
                            label = v.label,
                            passwordHash = v.passwordHash,
                            passwordSalt = v.passwordSalt,
                            createdAt = v.createdAt
                        )
                    },
                    passwordHint = entry.passwordHint,
                    reminderDays = entry.reminderDays,
                    lastVerified = entry.lastVerified,
                    createdAt = entry.createdAt,
                    streak = entry.streak,
                    bestStreak = entry.bestStreak,
                    totalAttempts = entry.totalAttempts,
                    successfulAttempts = entry.successfulAttempts,
                    customReminderMessage = entry.customReminderMessage
                )
            }

            val history = repository.getAllHistory()

            val settings = BackupSettings(
                defaultInterval = prefs.getInt(SettingsViewModel.KEY_DEFAULT_REMINDER_DAYS, 7),
                appLockEnabled = prefs.getBoolean(SettingsViewModel.KEY_APP_LOCK_ENABLED, false),
                theme = prefs.getInt(SettingsViewModel.KEY_THEME_MODE, SettingsViewModel.THEME_SYSTEM),
                notificationsEnabled = prefs.getBoolean(SettingsViewModel.KEY_NOTIFICATIONS_ENABLED, true),
                quietHoursStart = prefs.getInt(SettingsViewModel.KEY_QUIET_HOURS_START, 22),
                quietHoursEnd = prefs.getInt(SettingsViewModel.KEY_QUIET_HOURS_END, 8),
                adaptiveDifficulty = prefs.getBoolean(SettingsViewModel.KEY_ADAPTIVE_DIFFICULTY, true)
            )

            val backupData = BackupData(
                version = 1,
                exportDate = System.currentTimeMillis(),
                entries = backupEntries,
                history = history,
                settings = settings
            )

            val jsonPayload = json.encodeToString(backupData)
            encrypt(jsonPayload.toByteArray(Charsets.UTF_8), backupPassword)
        } catch (_: Exception) {
            null
        } finally {
            backupPassword.fill('\u0000')
        }
    }

    /**
     * Result of an import operation.
     */
    sealed class ImportResult {
        data class Success(val entryCount: Int) : ImportResult()
        data object WrongPassword : ImportResult()
        data object CorruptedFile : ImportResult()
        data object Error : ImportResult()
    }

    /**
     * Imports and restores data from an encrypted backup file.
     * Replaces ALL current data.
     */
    fun importBackup(context: Context, fileBytes: ByteArray, backupPassword: CharArray): ImportResult {
        return try {
            if (fileBytes.size < SALT_LENGTH + IV_LENGTH + 1) {
                return ImportResult.CorruptedFile
            }

            val decryptedBytes = try {
                decrypt(fileBytes, backupPassword)
            } catch (_: Exception) {
                return ImportResult.WrongPassword
            }

            val jsonPayload = String(decryptedBytes, Charsets.UTF_8)
            val backupData = try {
                json.decodeFromString<BackupData>(jsonPayload)
            } catch (_: Exception) {
                return ImportResult.CorruptedFile
            }

            // Get or create a Keystore key for re-encrypting emails on this device
            val keystoreKey = KeystoreManager.getOrCreateKey()

            // Convert backup entries to PasswordEntry, re-encrypting emails
            val restoredEntries = backupData.entries.map { backup ->
                val (encryptedEmail, emailIV) = if (backup.email.isNotEmpty()) {
                    CryptoUtil.encrypt(backup.email, keystoreKey)
                } else {
                    Pair("", "")
                }

                // Migrate old backups: if no passwordVersions but has passwordHash, create one
                val versions = if (backup.passwordVersions.isNotEmpty()) {
                    backup.passwordVersions.map { v ->
                        PasswordVersion(
                            id = v.id,
                            label = v.label,
                            passwordHash = v.passwordHash,
                            passwordSalt = v.passwordSalt,
                            createdAt = v.createdAt
                        )
                    }
                } else if (backup.passwordHash.isNotEmpty()) {
                    listOf(
                        PasswordVersion(
                            id = java.util.UUID.randomUUID().toString(),
                            label = "Current",
                            passwordHash = backup.passwordHash,
                            passwordSalt = backup.passwordSalt,
                            createdAt = backup.createdAt
                        )
                    )
                } else {
                    emptyList()
                }

                PasswordEntry(
                    id = backup.id,
                    serviceName = backup.serviceName,
                    serviceColor = backup.serviceColor,
                    serviceIcon = backup.serviceIcon,
                    encryptedEmail = encryptedEmail,
                    emailIV = emailIV,
                    passwordVersions = versions,
                    passwordHint = backup.passwordHint,
                    reminderDays = backup.reminderDays,
                    lastVerified = backup.lastVerified,
                    createdAt = backup.createdAt,
                    streak = backup.streak,
                    bestStreak = backup.bestStreak,
                    totalAttempts = backup.totalAttempts,
                    successfulAttempts = backup.successfulAttempts,
                    customReminderMessage = backup.customReminderMessage
                )
            }

            // Clear all existing data and replace
            val repository = PasswordRepository(context)
            repository.deleteAllEntries()
            repository.replaceAllData(restoredEntries, backupData.history)

            // Restore settings
            val prefs = context.getSharedPreferences("mpt_prefs", Context.MODE_PRIVATE)
            val s = backupData.settings
            prefs.edit()
                .putInt(SettingsViewModel.KEY_DEFAULT_REMINDER_DAYS, s.defaultInterval)
                .putBoolean(SettingsViewModel.KEY_APP_LOCK_ENABLED, s.appLockEnabled)
                .putInt(SettingsViewModel.KEY_THEME_MODE, s.theme)
                .putBoolean(SettingsViewModel.KEY_NOTIFICATIONS_ENABLED, s.notificationsEnabled)
                .putInt(SettingsViewModel.KEY_QUIET_HOURS_START, s.quietHoursStart)
                .putInt(SettingsViewModel.KEY_QUIET_HOURS_END, s.quietHoursEnd)
                .putBoolean(SettingsViewModel.KEY_ADAPTIVE_DIFFICULTY, s.adaptiveDifficulty)
                .putBoolean("onboarding_completed", true)
                .apply()

            ImportResult.Success(restoredEntries.size)
        } catch (_: Exception) {
            ImportResult.Error
        } finally {
            backupPassword.fill('\u0000')
        }
    }

    // --- Encryption internals ---

    private fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12 bytes generated by cipher
        val encrypted = cipher.doFinal(data)

        // File format: salt(32) + iv(12) + encrypted data
        return salt + iv + encrypted
    }

    private fun decrypt(fileBytes: ByteArray, password: CharArray): ByteArray {
        val salt = fileBytes.copyOfRange(0, SALT_LENGTH)
        val iv = fileBytes.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val encrypted = fileBytes.copyOfRange(SALT_LENGTH + IV_LENGTH, fileBytes.size)

        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val secret = factory.generateSecret(spec)
        val keySpec = SecretKeySpec(secret.encoded, "AES")
        spec.clearPassword()
        return keySpec
    }
}
