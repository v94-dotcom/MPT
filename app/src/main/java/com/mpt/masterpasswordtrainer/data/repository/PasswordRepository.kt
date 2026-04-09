package com.mpt.masterpasswordtrainer.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import com.mpt.masterpasswordtrainer.data.model.PasswordVersion
import com.mpt.masterpasswordtrainer.data.model.VerificationRecord
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Manages password entries in EncryptedSharedPreferences. All data is stored locally and encrypted at rest. */
class PasswordRepository(context: Context) {

    companion object {
        private const val PREFS_FILE = "mpt_encrypted_prefs"
        private const val KEY_ENTRIES = "password_entries"
        private const val KEY_HISTORY = "verification_history"
        private const val MAX_HISTORY_PER_ENTRY = 365
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAllEntries(): List<PasswordEntry> {
        val jsonString = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val entries = json.decodeFromString<List<PasswordEntry>>(jsonString)
            // Migrate pre-Upgrade-6 entries: single passwordHash/Salt → passwordVersions list
            val needsMigration = entries.any { it.passwordVersions.isEmpty() && it.passwordHash.isNotEmpty() }
            if (needsMigration) {
                val migrated = entries.map { entry ->
                    if (entry.passwordVersions.isEmpty() && entry.passwordHash.isNotEmpty()) {
                        entry.copy(
                            passwordVersions = listOf(
                                PasswordVersion(
                                    id = UUID.randomUUID().toString(),
                                    label = "Current",
                                    passwordHash = entry.passwordHash,
                                    passwordSalt = entry.passwordSalt,
                                    createdAt = entry.createdAt
                                )
                            ),
                            passwordHash = "",
                            passwordSalt = ""
                        )
                    } else entry
                }
                persistEntries(migrated)
                migrated
            } else {
                entries
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getEntry(id: String): PasswordEntry? {
        return getAllEntries().find { it.id == id }
    }

    fun saveEntry(entry: PasswordEntry) {
        val entries = getAllEntries().toMutableList()
        val existingIndex = entries.indexOfFirst { it.id == entry.id }
        if (existingIndex >= 0) {
            entries[existingIndex] = entry
        } else {
            entries.add(entry)
        }
        persistEntries(entries)
    }

    fun deleteEntry(id: String) {
        val entries = getAllEntries().filter { it.id != id }
        persistEntries(entries)
        deleteHistoryForEntry(id)
    }

    fun updateVerification(id: String, success: Boolean, matchedVersionLabel: String = "") {
        val entries = getAllEntries().toMutableList()
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return

        val entry = entries[index]
        val newStreak = if (success) entry.streak + 1 else entry.streak
        val newBestStreak = if (newStreak > entry.bestStreak) newStreak else entry.bestStreak

        val updatedEntry = if (success) {
            entry.copy(
                lastVerified = System.currentTimeMillis(),
                streak = newStreak,
                totalAttempts = entry.totalAttempts + 1,
                successfulAttempts = entry.successfulAttempts + 1,
                bestStreak = newBestStreak
            )
        } else {
            entry.copy(
                totalAttempts = entry.totalAttempts + 1
            )
        }

        entries[index] = updatedEntry
        persistEntries(entries)

        // Append verification record to history
        addVerificationRecord(
            VerificationRecord(
                entryId = id,
                timestamp = System.currentTimeMillis(),
                success = success,
                matchedVersionLabel = matchedVersionLabel
            )
        )
    }

    fun resetStreak(id: String) {
        val entries = getAllEntries().toMutableList()
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return

        entries[index] = entries[index].copy(streak = 0)
        persistEntries(entries)
    }

    fun deleteAllEntries() {
        prefs.edit().remove(KEY_ENTRIES).remove(KEY_HISTORY).apply()
    }

    fun replaceAllData(entries: List<PasswordEntry>, history: List<VerificationRecord>) {
        persistEntries(entries)
        persistHistory(history)
    }

    // --- Verification History ---

    fun getAllHistory(): List<VerificationRecord> {
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<VerificationRecord>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getHistoryForEntry(entryId: String): List<VerificationRecord> {
        return getAllHistory().filter { it.entryId == entryId }
    }

    private fun addVerificationRecord(record: VerificationRecord) {
        val history = getAllHistory().toMutableList()
        history.add(record)

        // Cap at MAX_HISTORY_PER_ENTRY per entry — remove oldest if exceeded
        val entryCounts = history.groupBy { it.entryId }.mapValues { it.value.size }
        val trimmed = if ((entryCounts[record.entryId] ?: 0) > MAX_HISTORY_PER_ENTRY) {
            val entryRecords = history.filter { it.entryId == record.entryId }
                .sortedBy { it.timestamp }
            val toRemove = entryRecords.take(entryRecords.size - MAX_HISTORY_PER_ENTRY).toSet()
            history.filterNot { it in toRemove }
        } else {
            history
        }

        persistHistory(trimmed)
    }

    fun deleteHistoryForEntry(entryId: String) {
        val history = getAllHistory().filter { it.entryId != entryId }
        persistHistory(history)
    }

    private fun persistHistory(history: List<VerificationRecord>) {
        val jsonString = json.encodeToString(history)
        prefs.edit().putString(KEY_HISTORY, jsonString).apply()
    }

    private fun persistEntries(entries: List<PasswordEntry>) {
        val jsonString = json.encodeToString(entries)
        prefs.edit().putString(KEY_ENTRIES, jsonString).apply()
    }
}
