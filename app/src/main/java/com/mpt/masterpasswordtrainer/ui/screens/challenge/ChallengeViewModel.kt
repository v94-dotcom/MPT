package com.mpt.masterpasswordtrainer.ui.screens.challenge

import android.app.Application
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

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PasswordRepository(application)

    // Entry data
    var entry by mutableStateOf<PasswordEntry?>(null)
        private set
    var daysElapsed by mutableIntStateOf(0)
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

    // Result message
    var resultMessage by mutableStateOf<String?>(null)
        private set

    fun loadEntry(entryId: String) {
        val loaded = repository.getEntry(entryId) ?: return
        entry = loaded
        daysElapsed = daysSinceLastVerified(loaded)
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
                val (emailCorrect, passwordCorrect) = withContext(Dispatchers.Default) {
                    // Decrypt stored email and compare
                    val key = KeystoreManager.getOrCreateKey()
                    val storedEmail = CryptoUtil.decrypt(
                        currentEntry.encryptedEmail,
                        currentEntry.emailIV,
                        key
                    )
                    val emailMatch = storedEmail.trim().equals(emailInput.trim(), ignoreCase = true)

                    // Hash input password and compare
                    val passwordCopy = passwordInput.copyOf()
                    val passwordMatch = HashUtil.verifyPassword(
                        passwordCopy,
                        currentEntry.passwordSalt,
                        currentEntry.passwordHash
                    )
                    // passwordCopy is zeroed inside verifyPassword

                    Pair(emailMatch, passwordMatch)
                }

                sessionAttempts++

                val result = when {
                    emailCorrect && passwordCorrect -> VerificationResult.SUCCESS
                    !emailCorrect && passwordCorrect -> VerificationResult.EMAIL_WRONG
                    emailCorrect && !passwordCorrect -> VerificationResult.PASSWORD_WRONG
                    else -> VerificationResult.BOTH_WRONG
                }

                verificationResult = result

                when (result) {
                    VerificationResult.SUCCESS -> {
                        consecutiveFailures = 0
                        repository.updateVerification(currentEntry.id, true)
                        // Reload entry to get updated streak
                        entry = repository.getEntry(currentEntry.id)
                        showSuccess = true
                        resultMessage = "Perfect! ✓"
                    }

                    VerificationResult.EMAIL_WRONG -> {
                        consecutiveFailures++
                        repository.updateVerification(currentEntry.id, false)
                        shakeEmail = true
                        resultMessage = "Password is correct, but the email doesn't match."
                        emailInput = ""
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

    private fun checkFailureThresholds() {
        val currentEntry = entry ?: return

        // After 3 consecutive failures: offer email hint
        if (consecutiveFailures >= 3) {
            showHintOption = true
        }

        // After 3 consecutive failures: reset streak
        if (consecutiveFailures >= 3) {
            repository.resetStreak(currentEntry.id)
            entry = repository.getEntry(currentEntry.id)
        }

        // After 5 consecutive failures: lock for 60 seconds
        if (consecutiveFailures >= 5) {
            startLockout()
        }
    }

    fun showEmailHint() {
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

    private fun maskEmailForHint(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex < 1) return email

        val local = email.substring(0, atIndex)
        val domain = email.substring(atIndex + 1)
        val dotIndex = domain.lastIndexOf('.')
        if (dotIndex < 2) return email

        val domainName = domain.substring(0, dotIndex)
        val tld = domain.substring(dotIndex)

        val maskedLocal = if (local.length <= 1) {
            "$local•••"
        } else {
            "${local.first()}•••${local.last()}"
        }

        val maskedDomain = if (domainName.length <= 2) {
            "$domainName•••"
        } else {
            "${domainName.take(2)}•••"
        }

        return "$maskedLocal@$maskedDomain$tld"
    }

    override fun onCleared() {
        super.onCleared()
        passwordInput.fill('\u0000')
    }
}
