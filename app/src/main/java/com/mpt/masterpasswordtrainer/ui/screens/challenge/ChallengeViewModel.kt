package com.mpt.masterpasswordtrainer.ui.screens.challenge

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
import com.mpt.masterpasswordtrainer.data.security.KeyInvalidatedException
import com.mpt.masterpasswordtrainer.data.security.KeystoreManager
import com.mpt.masterpasswordtrainer.ui.components.daysSinceLastVerified
import com.mpt.masterpasswordtrainer.ui.components.maskEmail
import com.mpt.masterpasswordtrainer.ui.screens.settings.SettingsViewModel
import com.mpt.masterpasswordtrainer.ui.screens.settings.SettingsViewModel.Companion.DEFAULT_PANIC_WIPE_THRESHOLD
import com.mpt.masterpasswordtrainer.widget.MPTWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VerificationResult {
    SUCCESS,
    EMAIL_WRONG,
    PASSWORD_WRONG,
    BOTH_WRONG
}

enum class DifficultyTier {
    NORMAL,
    INTERMEDIATE,
    ADVANCED;

    companion object {
        const val INTERMEDIATE_THRESHOLD = 5
        const val ADVANCED_THRESHOLD = 10
        const val ADVANCED_DELAY_SECONDS = 5

        fun fromStreak(streak: Int): DifficultyTier = when {
            streak >= ADVANCED_THRESHOLD -> ADVANCED
            streak >= INTERMEDIATE_THRESHOLD -> INTERMEDIATE
            else -> NORMAL
        }
    }
}

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PasswordRepository(application)
    private val prefs = application.getSharedPreferences("mpt_prefs", Context.MODE_PRIVATE)

    // Entry data
    var entry by mutableStateOf<PasswordEntry?>(null)
        private set
    var daysElapsed by mutableIntStateOf(0)
        private set

    // Difficulty state
    var difficultyTier by mutableStateOf(DifficultyTier.NORMAL)
        private set
    var delayCountdown by mutableIntStateOf(0)
        private set
    var isDelayActive by mutableStateOf(false)
        private set

    // Input state
    var emailInput by mutableStateOf("")
        private set
    var passwordInput by mutableStateOf(charArrayOf())
        private set

    // Verification state
    var verificationResult by mutableStateOf<VerificationResult?>(null)
        private set
    var isVerifying by mutableStateOf(false)
        private set
    var consecutiveFailures by mutableIntStateOf(0)
        private set
    var sessionAttempts by mutableIntStateOf(0)
        private set

    // Lock state
    var isLocked by mutableStateOf(false)
        private set
    var lockTimeRemaining by mutableLongStateOf(0L)
        private set

    // Animation triggers
    var shakeEmail by mutableStateOf(false)
        private set
    var shakePassword by mutableStateOf(false)
        private set
    var showSuccess by mutableStateOf(false)
        private set
    var showHintOption by mutableStateOf(false)
        private set
    var maskedEmailHint by mutableStateOf<String?>(null)
        private set
    var hasEmail by mutableStateOf(true)
        private set
    var hasPasswordHint by mutableStateOf(false)
        private set
    var passwordHintRevealed by mutableStateOf<String?>(null)
        private set

    // Panic wipe triggered — ChallengeScreen navigates to onboarding
    var panicWipeTriggered by mutableStateOf(false)
        private set

    // Result message
    var resultMessage by mutableStateOf<String?>(null)
        private set

    fun loadEntry(entryId: String) {
        val loaded = repository.getEntry(entryId) ?: return
        entry = loaded
        daysElapsed = daysSinceLastVerified(loaded)

        // Check if entry has email stored
        try {
            val key = KeystoreManager.getOrCreateKey()
            val decryptedEmail = CryptoUtil.decrypt(loaded.encryptedEmail, loaded.emailIV, key)
            hasEmail = decryptedEmail.isNotEmpty()
        } catch (_: Exception) {
            hasEmail = true // Assume has email on decryption failure
        }

        hasPasswordHint = loaded.passwordHint.isNotEmpty()

        // Determine difficulty tier
        val adaptiveEnabled = prefs.getBoolean(SettingsViewModel.KEY_ADAPTIVE_DIFFICULTY, true)
        difficultyTier = if (adaptiveEnabled) DifficultyTier.fromStreak(loaded.streak) else DifficultyTier.NORMAL

        // Start delay countdown for advanced mode
        if (difficultyTier == DifficultyTier.ADVANCED) {
            startDelayCountdown()
        }
    }

    private fun startDelayCountdown() {
        isDelayActive = true
        delayCountdown = DifficultyTier.ADVANCED_DELAY_SECONDS
        viewModelScope.launch {
            while (delayCountdown > 0) {
                delay(1000)
                delayCountdown--
            }
            isDelayActive = false
        }
    }

    fun updateEmailInput(value: String) {
        emailInput = value
        // Clear result state when user types
        if (verificationResult != null) {
            verificationResult = null
            resultMessage = null
        }
    }

    fun updatePasswordInput(value: CharArray) {
        passwordInput.fill('\u0000')
        passwordInput = value
        if (verificationResult != null) {
            verificationResult = null
            resultMessage = null
        }
    }

    fun verify() {
        val currentEntry = entry ?: return
        if (isLocked || isVerifying) return

        isVerifying = true
        verificationResult = null
        resultMessage = null
        shakeEmail = false
        shakePassword = false

        viewModelScope.launch {
            try {
                data class VerifyResult(val emailCorrect: Boolean, val passwordCorrect: Boolean, val matchedVersionLabel: String)

                val verifyResult = withContext(Dispatchers.Default) {
                    val key = KeystoreManager.getOrCreateKey()

                    // Compare email (skip if no email stored)
                    val emailMatch = if (hasEmail) {
                        val storedEmail = CryptoUtil.decrypt(
                            currentEntry.encryptedEmail,
                            currentEntry.emailIV,
                            key
                        )
                        storedEmail.trim().equals(emailInput.trim(), ignoreCase = true)
                    } else {
                        true
                    }

                    // Check password against ALL stored versions
                    var passwordMatch = false
                    var matchedLabel = ""
                    for (version in currentEntry.passwordVersions) {
                        val passwordCopy = passwordInput.copyOf()
                        if (HashUtil.verifyPassword(passwordCopy, version.passwordSalt, version.passwordHash)) {
                            passwordMatch = true
                            matchedLabel = version.label
                            break
                        }
                    }

                    VerifyResult(emailMatch, passwordMatch, matchedLabel)
                }

                sessionAttempts++

                val result = when {
                    verifyResult.emailCorrect && verifyResult.passwordCorrect -> VerificationResult.SUCCESS
                    !verifyResult.emailCorrect && verifyResult.passwordCorrect -> VerificationResult.EMAIL_WRONG
                    verifyResult.emailCorrect && !verifyResult.passwordCorrect -> VerificationResult.PASSWORD_WRONG
                    else -> VerificationResult.BOTH_WRONG
                }

                verificationResult = result

                when (result) {
                    VerificationResult.SUCCESS -> {
                        consecutiveFailures = 0
                        resetGlobalFailureCounter()
                        repository.updateVerification(currentEntry.id, true, verifyResult.matchedVersionLabel)
                        // Reload entry to get updated streak
                        entry = repository.getEntry(currentEntry.id)
                        showSuccess = true
                        val hasMultipleVersions = currentEntry.passwordVersions.size > 1
                        resultMessage = if (hasMultipleVersions) {
                            "Correct! (matched: ${verifyResult.matchedVersionLabel})"
                        } else {
                            "Perfect! ✓"
                        }
                        MPTWidgetUpdater.updateAll(getApplication())
                    }

                    VerificationResult.EMAIL_WRONG -> {
                        consecutiveFailures++
                        repository.updateVerification(currentEntry.id, false)
                        shakeEmail = true
                        resultMessage = "Password is correct, but the email doesn't match."
                        emailInput = ""
                        // Email-only failures do not increment panic wipe counter
                        checkFailureThresholds()
                    }

                    VerificationResult.PASSWORD_WRONG -> {
                        consecutiveFailures++
                        repository.updateVerification(currentEntry.id, false)
                        shakeEmail = true
                        shakePassword = true
                        resultMessage = "That's not right. Take a breath and try again."
                        emailInput = ""
                        passwordInput.fill('\u0000')
                        passwordInput = charArrayOf()
                        incrementGlobalFailureCounter()
                        checkFailureThresholds()
                    }

                    VerificationResult.BOTH_WRONG -> {
                        consecutiveFailures++
                        repository.updateVerification(currentEntry.id, false)
                        shakeEmail = true
                        shakePassword = true
                        resultMessage = "Neither matched. Think carefully and try again."
                        emailInput = ""
                        passwordInput.fill('\u0000')
                        passwordInput = charArrayOf()
                        incrementGlobalFailureCounter()
                        checkFailureThresholds()
                    }
                }
            } catch (_: KeyInvalidatedException) {
                resultMessage = "Security key invalidated. Please return to dashboard."
            } catch (_: Exception) {
                resultMessage = "Verification failed. Please try again."
            } finally {
                isVerifying = false
            }
        }
    }

    private fun resetGlobalFailureCounter() {
        prefs.edit().putInt(SettingsViewModel.KEY_GLOBAL_CONSECUTIVE_FAILURES, 0).apply()
    }

    private fun incrementGlobalFailureCounter() {
        val panicEnabled = prefs.getBoolean(SettingsViewModel.KEY_PANIC_WIPE_ENABLED, false)
        if (!panicEnabled) return

        val current = prefs.getInt(SettingsViewModel.KEY_GLOBAL_CONSECUTIVE_FAILURES, 0) + 1
        prefs.edit().putInt(SettingsViewModel.KEY_GLOBAL_CONSECUTIVE_FAILURES, current).apply()

        val threshold = prefs.getInt(SettingsViewModel.KEY_PANIC_WIPE_THRESHOLD, DEFAULT_PANIC_WIPE_THRESHOLD)
        if (current >= threshold) {
            triggerPanicWipe()
        }
    }

    private fun triggerPanicWipe() {
        // Silently wipe all data — app should appear as fresh install
        repository.deleteAllEntries()
        KeystoreManager.deleteKey()
        prefs.edit()
            .remove(SettingsViewModel.KEY_APP_LOCK_ENABLED)
            .remove(SettingsViewModel.KEY_DEFAULT_REMINDER_DAYS)
            .remove(SettingsViewModel.KEY_NOTIFICATIONS_ENABLED)
            .remove(SettingsViewModel.KEY_QUIET_HOURS_START)
            .remove(SettingsViewModel.KEY_QUIET_HOURS_END)
            .remove(SettingsViewModel.KEY_THEME_MODE)
            .remove(SettingsViewModel.KEY_ADAPTIVE_DIFFICULTY)
            .remove(SettingsViewModel.KEY_PANIC_WIPE_ENABLED)
            .remove(SettingsViewModel.KEY_PANIC_WIPE_THRESHOLD)
            .remove(SettingsViewModel.KEY_GLOBAL_CONSECUTIVE_FAILURES)
            .putBoolean("onboarding_completed", false)
            .apply()

        panicWipeTriggered = true
    }

    private fun checkFailureThresholds() {
        val currentEntry = entry ?: return

        // After 3 consecutive failures: offer hints
        if (consecutiveFailures >= 3 && (hasEmail || hasPasswordHint)) {
            showHintOption = true
        }

        // After 3 consecutive failures: reset streak
        if (consecutiveFailures >= 3) {
            repository.resetStreak(currentEntry.id)
            entry = repository.getEntry(currentEntry.id)
            updateDifficulty()
        }

        // After 5 consecutive failures: lock for 60 seconds
        if (consecutiveFailures >= 5) {
            startLockout()
        }
    }

    private fun updateDifficulty() {
        val adaptiveEnabled = prefs.getBoolean(SettingsViewModel.KEY_ADAPTIVE_DIFFICULTY, true)
        val currentEntry = entry ?: return
        difficultyTier = if (adaptiveEnabled) DifficultyTier.fromStreak(currentEntry.streak) else DifficultyTier.NORMAL
    }

    fun showEmailHint() {
        if (!hasEmail) return
        val currentEntry = entry ?: return
        viewModelScope.launch {
            try {
                val key = KeystoreManager.getOrCreateKey()
                val email = CryptoUtil.decrypt(
                    currentEntry.encryptedEmail,
                    currentEntry.emailIV,
                    key
                )
                maskedEmailHint = maskEmailForHint(email)
            } catch (_: Exception) {
                maskedEmailHint = "Unable to show hint"
            }
        }
    }

    fun showPasswordHint() {
        val currentEntry = entry ?: return
        if (currentEntry.passwordHint.isNotEmpty()) {
            passwordHintRevealed = currentEntry.passwordHint
        }
    }

    private fun startLockout() {
        isLocked = true
        lockTimeRemaining = 60L

        viewModelScope.launch {
            while (lockTimeRemaining > 0) {
                delay(1000)
                lockTimeRemaining--
            }
            isLocked = false
            consecutiveFailures = 0
        }
    }

    fun onShakeEmailComplete() {
        shakeEmail = false
    }

    fun onShakePasswordComplete() {
        shakePassword = false
    }

    private fun maskEmailForHint(value: String): String = maskEmail(value)

    override fun onCleared() {
        super.onCleared()
        passwordInput.fill('\u0000')
    }
}
