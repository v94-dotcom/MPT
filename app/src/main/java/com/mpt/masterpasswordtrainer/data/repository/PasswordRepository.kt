package com.mpt.masterpasswordtrainer.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Manages password entries in EncryptedSharedPreferences. All data is stored locally and encrypted at rest. */
class PasswordRepository(context: Context) {

    companion object {
        private const val PREFS_FILE = "mpt_encrypted_prefs"
        private const val KEY_ENTRIES = "password_entries"
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
            json.decodeFromString<List<PasswordEntry>>(jsonString)
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
    }

    fun updateVerification(id: String, success: Boolean) {
        val entries = getAllEntries().toMutableList()
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return

        val entry = entries[index]
        val updatedEntry = if (success) {
            entry.copy(
                lastVerified = System.currentTimeMillis(),
                streak = entry.streak + 1,
                totalAttempts = entry.totalAttempts + 1,
                successfulAttempts = entry.successfulAttempts + 1
            )
        } else {
            entry.copy(
                totalAttempts = entry.totalAttempts + 1
            )
        }

        entries[index] = updatedEntry
        persistEntries(entries)
    }

    fun resetStreak(id: String) {
        val entries = getAllEntries().toMutableList()
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return

        entries[index] = entries[index].copy(streak = 0)
        persistEntries(entries)
    }

    fun deleteAllEntries() {
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private fun persistEntries(entries: List<PasswordEntry>) {
        val jsonString = json.encodeToString(entries)
        prefs.edit().putString(KEY_ENTRIES, jsonString).apply()
    }
}
