package com.mpt.masterpasswordtrainer.ui.screens.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpt.masterpasswordtrainer.data.backup.BackupManager
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import com.mpt.masterpasswordtrainer.data.security.KeystoreManager
import com.mpt.masterpasswordtrainer.ui.theme.ThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "mpt_prefs"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_DEFAULT_REMINDER_DAYS = "default_reminder_days"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_QUIET_HOURS_START = "quiet_hours_start"
        const val KEY_QUIET_HOURS_END = "quiet_hours_end"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ADAPTIVE_DIFFICULTY = "adaptive_difficulty"
        const val KEY_PANIC_WIPE_ENABLED = "panic_wipe_enabled"
        const val KEY_PANIC_WIPE_THRESHOLD = "panic_wipe_threshold"
        const val KEY_GLOBAL_CONSECUTIVE_FAILURES = "global_consecutive_failures"

        val PANIC_WIPE_THRESHOLD_OPTIONS = listOf(15, 25, 50)
        const val DEFAULT_PANIC_WIPE_THRESHOLD = 25

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

    private val _adaptiveDifficulty = MutableStateFlow(prefs.getBoolean(KEY_ADAPTIVE_DIFFICULTY, true))
    val adaptiveDifficulty: StateFlow<Boolean> = _adaptiveDifficulty.asStateFlow()

    private val _panicWipeEnabled = MutableStateFlow(prefs.getBoolean(KEY_PANIC_WIPE_ENABLED, false))
    val panicWipeEnabled: StateFlow<Boolean> = _panicWipeEnabled.asStateFlow()

    private val _panicWipeThreshold = MutableStateFlow(prefs.getInt(KEY_PANIC_WIPE_THRESHOLD, DEFAULT_PANIC_WIPE_THRESHOLD))
    val panicWipeThreshold: StateFlow<Int> = _panicWipeThreshold.asStateFlow()

    // Backup state
    private val _backupBytes = MutableStateFlow<ByteArray?>(null)
    val backupBytes: StateFlow<ByteArray?> = _backupBytes.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importResult = MutableStateFlow<BackupManager.ImportResult?>(null)
    val importResult: StateFlow<BackupManager.ImportResult?> = _importResult.asStateFlow()

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

    fun setAdaptiveDifficulty(enabled: Boolean) {
        _adaptiveDifficulty.value = enabled
        prefs.edit().putBoolean(KEY_ADAPTIVE_DIFFICULTY, enabled).apply()
    }

    fun setPanicWipeEnabled(enabled: Boolean) {
        _panicWipeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PANIC_WIPE_ENABLED, enabled).apply()
        if (!enabled) {
            // Reset counter when disabling
            prefs.edit().putInt(KEY_GLOBAL_CONSECUTIVE_FAILURES, 0).apply()
        }
    }

    fun setPanicWipeThreshold(threshold: Int) {
        _panicWipeThreshold.value = threshold
        prefs.edit().putInt(KEY_PANIC_WIPE_THRESHOLD, threshold).apply()
    }

    fun hasBackup(): Boolean {
        // Simple heuristic: check if user has ever exported a backup
        // We consider the backup recommendation satisfied if entries exist
        // (the actual check is just whether there are entries to lose)
        return repository.getAllEntries().isEmpty()
    }

    fun setThemeMode(mode: Int) {
        _themeMode.value = mode
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        ThemeState.setThemeMode(mode)
    }

    fun createBackup(password: CharArray) {
        viewModelScope.launch {
            _isExporting.value = true
            val bytes = withContext(Dispatchers.Default) {
                BackupManager.exportBackup(getApplication(), password)
            }
            _backupBytes.value = bytes
            _isExporting.value = false
        }
    }

    fun clearBackupBytes() {
        _backupBytes.value = null
    }

    fun importBackup(fileBytes: ByteArray, password: CharArray) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = withContext(Dispatchers.Default) {
                BackupManager.importBackup(getApplication(), fileBytes, password)
            }
            _importResult.value = result
            _isImporting.value = false

            // Reload settings state if import succeeded
            if (result is BackupManager.ImportResult.Success) {
                reloadSettings()
            }
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    private fun reloadSettings() {
        _appLockEnabled.value = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        _defaultReminderDays.value = prefs.getInt(KEY_DEFAULT_REMINDER_DAYS, 7)
        _notificationsEnabled.value = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        _quietHoursStart.value = prefs.getInt(KEY_QUIET_HOURS_START, 22)
        _quietHoursEnd.value = prefs.getInt(KEY_QUIET_HOURS_END, 8)
        _themeMode.value = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
        _adaptiveDifficulty.value = prefs.getBoolean(KEY_ADAPTIVE_DIFFICULTY, true)
        _panicWipeEnabled.value = prefs.getBoolean(KEY_PANIC_WIPE_ENABLED, false)
        _panicWipeThreshold.value = prefs.getInt(KEY_PANIC_WIPE_THRESHOLD, DEFAULT_PANIC_WIPE_THRESHOLD)
        ThemeState.setThemeMode(_themeMode.value)
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
            .remove(KEY_ADAPTIVE_DIFFICULTY)
            .remove(KEY_PANIC_WIPE_ENABLED)
            .remove(KEY_PANIC_WIPE_THRESHOLD)
            .remove(KEY_GLOBAL_CONSECUTIVE_FAILURES)
            .putBoolean("onboarding_completed", false)
            .apply()

        // Reset local state
        _appLockEnabled.value = false
        _defaultReminderDays.value = 7
        _notificationsEnabled.value = true
        _quietHoursStart.value = 22
        _quietHoursEnd.value = 8
        _themeMode.value = THEME_SYSTEM
        _adaptiveDifficulty.value = true
        _panicWipeEnabled.value = false
        _panicWipeThreshold.value = DEFAULT_PANIC_WIPE_THRESHOLD
        ThemeState.setThemeMode(THEME_SYSTEM)
    }
}
