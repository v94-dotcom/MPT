package com.mpt.masterpasswordtrainer.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import com.mpt.masterpasswordtrainer.data.security.CryptoUtil
import com.mpt.masterpasswordtrainer.data.security.KeyInvalidatedException
import com.mpt.masterpasswordtrainer.data.security.KeystoreManager
import com.mpt.masterpasswordtrainer.ui.components.EntryStatus
import com.mpt.masterpasswordtrainer.ui.components.calculateStatus
import com.mpt.masterpasswordtrainer.ui.components.daysSinceLastVerified
import com.mpt.masterpasswordtrainer.ui.components.maskEmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardEntry(
    val entry: PasswordEntry,
    val maskedEmail: String,
    val status: EntryStatus,
    val daysElapsed: Int
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PasswordRepository(application)

    private val _entries = MutableStateFlow<List<DashboardEntry>>(emptyList())
    val entries: StateFlow<List<DashboardEntry>> = _entries.asStateFlow()

    private val _overdueCount = MutableStateFlow(0)
    val overdueCount: StateFlow<Int> = _overdueCount.asStateFlow()

    private val _keyInvalidated = MutableStateFlow(false)
    val keyInvalidated: StateFlow<Boolean> = _keyInvalidated.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val rawEntries = repository.getAllEntries()
                val key = try {
                    KeystoreManager.getOrCreateKey()
                } catch (_: KeyInvalidatedException) {
                    _keyInvalidated.value = true
                    return@withContext emptyList()
                } catch (_: Exception) {
                    null
                }

                rawEntries.map { entry ->
                    val daysElapsed = daysSinceLastVerified(entry)
                    val status = calculateStatus(daysElapsed, entry.reminderDays)

                    val maskedEmail = if (key != null) {
                        try {
                            val decrypted = CryptoUtil.decrypt(entry.encryptedEmail, entry.emailIV, key)
                            maskEmail(decrypted)
                        } catch (_: Exception) {
                            "•••@•••"
                        }
                    } else {
                        "•••@•••"
                    }

                    DashboardEntry(
                        entry = entry,
                        maskedEmail = maskedEmail,
                        status = status,
                        daysElapsed = daysElapsed
                    )
                }.sortedWith(
                    compareByDescending<DashboardEntry> { it.status == EntryStatus.RED }
                        .thenByDescending { it.status == EntryStatus.AMBER }
                        .thenByDescending { it.daysElapsed }
                )
            }

            _entries.value = result
            _overdueCount.value = result.count { it.status == EntryStatus.RED }
        }
    }

    /** Resets all data after a key invalidation — deletes entries and Keystore key. */
    fun resetAfterKeyInvalidation() {
        repository.deleteAllEntries()
        KeystoreManager.deleteKey()
        _keyInvalidated.value = false
        _entries.value = emptyList()
        _overdueCount.value = 0
    }

    fun deleteEntry(id: String) {
        repository.deleteEntry(id)
        loadEntries()
    }
}
