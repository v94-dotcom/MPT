package com.mpt.masterpasswordtrainer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordEntry(
    val id: String,
    val serviceName: String,
    val serviceColor: Long,
    val serviceIcon: String,
    val encryptedEmail: String,
    val emailIV: String,
    val passwordHash: String,
    val passwordSalt: String,
    val reminderDays: Int,
    val lastVerified: Long,
    val createdAt: Long,
    val streak: Int = 0,
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val passwordHint: String = ""
)
