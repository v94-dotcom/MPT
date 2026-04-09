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
import com.mpt.masterpasswordtrainer.data.model.PasswordVersion
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
    var emailEnabled by mutableStateOf(true)
        private set
    var password by mutableStateOf(charArrayOf())
        private set
    var confirmPassword by mutableStateOf(charArrayOf())
        private set
    var reminderDays by mutableIntStateOf(7)
        private set
    var passwordHint by mutableStateOf("")
        private set
    var customReminderMessage by mutableStateOf("")
        private set

    // Additional password versions (index 0 is version 2, index 1 is version 3)
    data class ExtraVersion(
        val id: String = UUID.randomUUID().toString(),
        val label: String = "Previous",
        val password: CharArray = charArrayOf(),
        val confirmPassword: CharArray = charArrayOf()
    )

    var extraVersions by mutableStateOf<List<ExtraVersion>>(emptyList())
        private set
    var showExtraVersions by mutableStateOf(false)

    // In edit mode, existing password versions (read-only display)
    var existingVersions by mutableStateOf<List<PasswordVersion>>(emptyList())
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
        passwordHint = entry.passwordHint
        customReminderMessage = entry.customReminderMessage
        existingVersions = entry.passwordVersions

        // Decrypt email to pre-fill
        try {
            val key = KeystoreManager.getOrCreateKey()
            val decrypted = CryptoUtil.decrypt(entry.encryptedEmail, entry.emailIV, key)
            if (decrypted.isEmpty()) {
                emailEnabled = false
                email = ""
            } else {
                emailEnabled = true
                email = decrypted
            }
        } catch (_: Exception) {
            email = ""
            emailEnabled = false
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

    fun updateEmailEnabled(enabled: Boolean) {
        emailEnabled = enabled
        if (!enabled) {
            email = ""
            clearError("email")
        }
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

    fun updatePasswordHint(value: String) {
        passwordHint = value.take(100)
    }

    fun updateCustomReminderMessage(value: String) {
        customReminderMessage = value.take(80)
    }

    fun addExtraVersion() {
        val totalVersions = (if (isEditMode) existingVersions.size else 1) + extraVersions.size
        if (totalVersions >= 3) return
        val defaultLabel = when (extraVersions.size) {
            0 -> if (isEditMode && existingVersions.size >= 2) "Legacy" else "Previous"
            else -> "Legacy"
        }
        extraVersions = extraVersions + ExtraVersion(label = defaultLabel)
    }

    fun removeExtraVersion(id: String) {
        val version = extraVersions.find { it.id == id }
        version?.password?.fill('\u0000')
        version?.confirmPassword?.fill('\u0000')
        extraVersions = extraVersions.filter { it.id != id }
    }

    fun updateExtraVersionLabel(id: String, label: String) {
        extraVersions = extraVersions.map {
            if (it.id == id) it.copy(label = label.take(20)) else it
        }
    }

    fun updateExtraVersionPassword(id: String, value: CharArray) {
        extraVersions = extraVersions.map {
            if (it.id == id) {
                it.password.fill('\u0000')
                it.copy(password = value)
            } else it
        }
        clearError("extraVersion_${id}_password")
        clearError("extraVersion_${id}_confirmPassword")
    }

    fun updateExtraVersionConfirmPassword(id: String, value: CharArray) {
        extraVersions = extraVersions.map {
            if (it.id == id) {
                it.confirmPassword.fill('\u0000')
                it.copy(confirmPassword = value)
            } else it
        }
        clearError("extraVersion_${id}_confirmPassword")
    }

    fun removeExistingVersion(versionId: String) {
        // Cannot delete the last remaining version
        if (existingVersions.size <= 1) return
        existingVersions = existingVersions.filter { it.id != versionId }
    }

    fun canAddMoreVersions(): Boolean {
        val totalVersions = (if (isEditMode) existingVersions.size else 1) + extraVersions.size
        return totalVersions < 3
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

                    // Build extra password versions (new versions added in this save)
                    val newExtraVersions = extraVersions.filter { it.password.isNotEmpty() }.map { ev ->
                        val salt = HashUtil.generateSalt()
                        val hash = HashUtil.hashPassword(ev.password.copyOf(), salt)
                        PasswordVersion(
                            id = ev.id,
                            label = ev.label.trim().ifEmpty { "Version" },
                            passwordHash = hash,
                            passwordSalt = salt,
                            createdAt = System.currentTimeMillis()
                        )
                    }

                    if (isEditMode && existingEntry != null) {
                        val existing = existingEntry!!

                        // Start with existing versions (possibly filtered by removals)
                        val keptVersions = existingVersions.toMutableList()

                        // If primary password field was filled, update the first version's hash
                        if (password.isNotEmpty() && keptVersions.isNotEmpty()) {
                            val salt = HashUtil.generateSalt()
                            val hash = HashUtil.hashPassword(password.copyOf(), salt)
                            keptVersions[0] = keptVersions[0].copy(
                                passwordHash = hash,
                                passwordSalt = salt
                            )
                        }

                        val finalVersions = keptVersions + newExtraVersions

                        existing.copy(
                            serviceName = serviceName.trim(),
                            serviceColor = serviceColor,
                            serviceIcon = serviceIcon,
                            encryptedEmail = encryptedEmail,
                            emailIV = emailIV,
                            passwordHash = "",
                            passwordSalt = "",
                            passwordVersions = finalVersions,
                            reminderDays = reminderDays,
                            passwordHint = passwordHint.trim(),
                            customReminderMessage = customReminderMessage.trim()
                        )
                    } else {
                        val salt = HashUtil.generateSalt()
                        val passwordCopy = password.copyOf()
                        val hash = HashUtil.hashPassword(passwordCopy, salt)

                        val primaryVersion = PasswordVersion(
                            id = UUID.randomUUID().toString(),
                            label = "Current",
                            passwordHash = hash,
                            passwordSalt = salt,
                            createdAt = System.currentTimeMillis()
                        )

                        val allVersions = listOf(primaryVersion) + newExtraVersions

                        PasswordEntry(
                            id = UUID.randomUUID().toString(),
                            serviceName = serviceName.trim(),
                            serviceColor = serviceColor,
                            serviceIcon = serviceIcon,
                            encryptedEmail = encryptedEmail,
                            emailIV = emailIV,
                            passwordVersions = allVersions,
                            reminderDays = reminderDays,
                            lastVerified = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis(),
                            passwordHint = passwordHint.trim(),
                            customReminderMessage = customReminderMessage.trim()
                        )
                    }
                }

                repository.saveEntry(entry)

                // Wipe sensitive fields
                password.fill('\u0000')
                confirmPassword.fill('\u0000')
                password = charArrayOf()
                confirmPassword = charArrayOf()
                extraVersions.forEach { ev ->
                    ev.password.fill('\u0000')
                    ev.confirmPassword.fill('\u0000')
                }
                extraVersions = emptyList()
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

        if (emailEnabled && email.isBlank()) {
            errs["email"] = "Email / Username is required"
        }

        if (!isEditMode && password.isEmpty()) {
            errs["password"] = "Password is required"
        }

        if (password.isNotEmpty() && !password.contentEquals(confirmPassword)) {
            errs["confirmPassword"] = "Passwords don't match"
        }

        // Validate extra password versions
        for (ev in extraVersions) {
            if (ev.password.isEmpty()) {
                errs["extraVersion_${ev.id}_password"] = "Password is required"
            }
            if (ev.password.isNotEmpty() && !ev.password.contentEquals(ev.confirmPassword)) {
                errs["extraVersion_${ev.id}_confirmPassword"] = "Passwords don't match"
            }
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
        extraVersions.forEach { ev ->
            ev.password.fill('\u0000')
            ev.confirmPassword.fill('\u0000')
        }
    }
}
