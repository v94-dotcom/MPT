package com.mpt.masterpasswordtrainer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportDate: Long,
    val entries: List<BackupEntry>,
    val history: List<VerificationRecord>,
    val settings: BackupSettings
)

@Serializable
data class BackupPasswordVersion(
    val id: String,
    val label: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long
)

@Serializable
data class BackupEntry(
    val id: String,
    val serviceName: String,
    val serviceColor: Long,
    val serviceIcon: String,
    val email: String, // plaintext email — backup file itself is encrypted
    // Legacy fields for backward compatibility with pre-Upgrade-6 backups
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val passwordVersions: List<BackupPasswordVersion> = emptyList(),
    val passwordHint: String = "",
    val reminderDays: Int,
    val lastVerified: Long,
    val createdAt: Long,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val customReminderMessage: String = ""
)

@Serializable
data class BackupSettings(
    val defaultInterval: Int = 7,
    val appLockEnabled: Boolean = false,
    val theme: Int = 0,
    val notificationsEnabled: Boolean = true,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 8,
    val adaptiveDifficulty: Boolean = true
)
