package com.mpt.masterpasswordtrainer.ui.screens.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import com.mpt.masterpasswordtrainer.data.security.KeystoreManager
import com.mpt.masterpasswordtrainer.ui.theme.ThemeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "mpt_prefs"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_DEFAULT_REMINDER_DAYS = "default_reminder_days"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_QUIET_HOURS_START = "quiet_hours_start"
        const val KEY_QUIET_HOURS_END = "quiet_hours_end"
        const val KEY_THEME_MODE = "theme_mode"

        // Theme modes
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val repository = PasswordRepository(application)

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _defaultReminderDays = MutableStateFlow(prefs.getInt(KEY_DEFAULT_REMINDER_DAYS, 7))
    val defaultReminderDays: StateFlow<Int> = _defaultReminderDays.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Quiet hours stored as hour-of-day (0-23), default 22:00 - 08:00
    private val _quietHoursStart = MutableStateFlow(prefs.getInt(KEY_QUIET_HOURS_START, 22))
    val quietHoursStart: StateFlow<Int> = _quietHoursStart.asStateFlow()

    private val _quietHoursEnd = MutableStateFlow(prefs.getInt(KEY_QUIET_HOURS_END, 8))
    val quietHoursEnd: StateFlow<Int> = _quietHoursEnd.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM))
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    fun setAppLockEnabled(enabled: Boolean) {
        _appLockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun setDefaultReminderDays(days: Int) {
        _defaultReminderDays.value = days
        prefs.edit().putInt(KEY_DEFAULT_REMINDER_DAYS, days).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun setQuietHoursStart(hour: Int) {
        _quietHoursStart.value = hour
        prefs.edit().putInt(KEY_QUIET_HOURS_START, hour).apply()
    }

    fun setQuietHoursEnd(hour: Int) {
        _quietHoursEnd.value = hour
        prefs.edit().putInt(KEY_QUIET_HOURS_END, hour).apply()
    }

    fun setThemeMode(mode: Int) {
        _themeMode.value = mode
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        ThemeState.setThemeMode(mode)
    }

    fun deleteAllData() {
        repository.deleteAllEntries()
        KeystoreManager.deleteKey()
        prefs.edit()
            .remove(KEY_APP_LOCK_ENABLED)
            .remove(KEY_DEFAULT_REMINDER_DAYS)
            .remove(KEY_NOTIFICATIONS_ENABLED)
            .remove(KEY_QUIET_HOURS_START)
            .remove(KEY_QUIET_HOURS_END)
            .remove(KEY_THEME_MODE)
            .putBoolean("onboarding_completed", false)
            .apply()

        // Reset local state
        _appLockEnabled.value = false
        _defaultReminderDays.value = 7
        _notificationsEnabled.value = true
        _quietHoursStart.value = 22
        _quietHoursEnd.value = 8
        _themeMode.value = THEME_SYSTEM
        ThemeState.setThemeMode(THEME_SYSTEM)
    }
}
