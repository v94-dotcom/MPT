package com.mpt.masterpasswordtrainer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordVersion(
    val id: String,
    val label: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long
)

@Serializable
data class PasswordEntry(
    val id: String,
    val serviceName: String,
    val serviceColor: Long,
    val serviceIcon: String,
    val encryptedEmail: String,
    val emailIV: String,
    // Legacy fields kept for migration from pre-Upgrade-6 data
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val passwordVersions: List<PasswordVersion> = emptyList(),
    val reminderDays: Int,
    val lastVerified: Long,
    val createdAt: Long,
    val streak: Int = 0,
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val passwordHint: String = "",
    val bestStreak: Int = 0,
    val customReminderMessage: String = ""
)

@Serializable
data class VerificationRecord(
    val entryId: String,
    val timestamp: Long,
    val success: Boolean,
    val matchedVersionLabel: String = ""
)
