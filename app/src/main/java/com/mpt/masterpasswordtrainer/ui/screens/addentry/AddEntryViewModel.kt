package com.mpt.masterpasswordtrainer.ui.screens.addentry

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import com.mpt.masterpasswordtrainer.data.security.CryptoUtil
import com.mpt.masterpasswordtrainer.data.security.HashUtil
import com.mpt.masterpasswordtrainer.data.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AddEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PasswordRepository(application)

    // Edit mode state
    var isEditMode by mutableStateOf(false)
        private set
    private var existingEntry: PasswordEntry? = null

    // Form state
    var serviceName by mutableStateOf("")
        private set
    var serviceIcon by mutableStateOf("Shield")
        private set
    var serviceColor by mutableLongStateOf(0xFF1E88E5) // EntryBlue default
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf(charArrayOf())
        private set
    var confirmPassword by mutableStateOf(charArrayOf())
        private set
    var reminderDays by mutableIntStateOf(7)
        private set

    // UI state
    var isLoading by mutableStateOf(false)
        private set
    var isSaved by mutableStateOf(false)
        private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    // Autocomplete suggestions
    val serviceSuggestions = listOf(
        "Bitwarden", "1Password", "LastPass", "KeePass",
        "Dashlane", "Proton Pass", "NordPass"
    )

    fun loadEntry(entryId: String) {
        val entry = repository.getEntry(entryId) ?: return
        existingEntry = entry
        isEditMode = true

        serviceName = entry.serviceName
        serviceIcon = entry.serviceIcon
        serviceColor = entry.serviceColor
        reminderDays = entry.reminderDays

        // Decrypt email to pre-fill
        try {
            val key = KeystoreManager.getOrCreateKey()
            email = CryptoUtil.decrypt(entry.encryptedEmail, entry.emailIV, key)
        } catch (_: Exception) {
            email = ""
            errors = mapOf("general" to "Could not decrypt email. You can re-enter it.")
        }
    }

    fun updateServiceName(value: String) {
        serviceName = value.take(50)
        clearError("serviceName")
    }

    fun updateServiceIcon(value: String) {
        serviceIcon = value
    }

    fun updateServiceColor(value: Long) {
        serviceColor = value
    }

    fun updateEmail(value: String) {
        email = value
        clearError("email")
    }

    fun updatePassword(value: CharArray) {
        password.fill('\u0000')
        password = value
        clearError("password")
        clearError("confirmPassword")
    }

    fun updateConfirmPassword(value: CharArray) {
        confirmPassword.fill('\u0000')
        confirmPassword = value
        clearError("confirmPassword")
    }

    fun updateReminderDays(value: Int) {
        reminderDays = value
    }

    fun save(isFromOnboarding: Boolean, onSuccess: () -> Unit) {
        val validationErrors = validate()
        if (validationErrors.isNotEmpty()) {
            errors = validationErrors
            return
        }

        isLoading = true

        viewModelScope.launch {
            try {
                val entry = withContext(Dispatchers.Default) {
                    val key = KeystoreManager.getOrCreateKey()
                    val (encryptedEmail, emailIV) = CryptoUtil.encrypt(email.trim(), key)

                    if (isEditMode && existingEntry != null) {
                        val existing = existingEntry!!
                        val passwordChanged = password.isNotEmpty()

                        val finalHash: String
                        val finalSalt: String
                        if (passwordChanged) {
                            val salt = HashUtil.generateSalt()
                            val passwordCopy = password.copyOf()
                            finalHash = HashUtil.hashPassword(passwordCopy, salt)
                            finalSalt = salt
                        } else {
                            finalHash = existing.passwordHash
                            finalSalt = existing.passwordSalt
                        }

                        existing.copy(
                            serviceName = serviceName.trim(),
                            serviceColor = serviceColor,
                            serviceIcon = serviceIcon,
                            encryptedEmail = encryptedEmail,
                            emailIV = emailIV,
                            passwordHash = finalHash,
                            passwordSalt = finalSalt,
                            reminderDays = reminderDays
                        )
                    } else {
                        val salt = HashUtil.generateSalt()
                        val passwordCopy = password.copyOf()
                        val hash = HashUtil.hashPassword(passwordCopy, salt)

                        PasswordEntry(
                            id = UUID.randomUUID().toString(),
                            serviceName = serviceName.trim(),
                            serviceColor = serviceColor,
                            serviceIcon = serviceIcon,
                            encryptedEmail = encryptedEmail,
                            emailIV = emailIV,
                            passwordHash = hash,
                            passwordSalt = salt,
                            reminderDays = reminderDays,
                            lastVerified = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis()
                        )
                    }
                }

                repository.saveEntry(entry)

                // Wipe sensitive fields
                password.fill('\u0000')
                confirmPassword.fill('\u0000')
                password = charArrayOf()
                confirmPassword = charArrayOf()
                email = ""

                if (isFromOnboarding) {
                    val prefs = getApplication<Application>()
                        .getSharedPreferences("mpt_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                }

                isSaved = true
                onSuccess()
            } catch (_: Exception) {
                errors = mapOf("general" to "Failed to save entry. Please try again.")
            } finally {
                isLoading = false
            }
        }
    }

    private fun validate(): Map<String, String> {
        val errs = mutableMapOf<String, String>()

        if (serviceName.isBlank()) {
            errs["serviceName"] = "Service name is required"
        }

        if (email.isBlank()) {
            errs["email"] = "Email is required"
        } else if (!email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) {
            errs["email"] = "Enter a valid email address"
        }

        if (!isEditMode && password.isEmpty()) {
            errs["password"] = "Password is required"
        }

        if (password.isNotEmpty() && !password.contentEquals(confirmPassword)) {
            errs["confirmPassword"] = "Passwords don't match"
        }

        return errs
    }

    private fun clearError(key: String) {
        if (errors.containsKey(key)) {
            errors = errors - key
        }
    }

    override fun onCleared() {
        super.onCleared()
        password.fill('\u0000')
        confirmPassword.fill('\u0000')
    }
}
