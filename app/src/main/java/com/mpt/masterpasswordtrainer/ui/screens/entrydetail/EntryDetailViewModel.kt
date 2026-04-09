package com.mpt.masterpasswordtrainer.ui.screens.entrydetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import com.mpt.masterpasswordtrainer.data.model.VerificationRecord
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class DayStatus(
    val date: LocalDate,
    val hasSuccess: Boolean,
    val hasFailureOnly: Boolean
)

data class VersionSuccessCount(
    val label: String,
    val count: Int
)

data class EntryStats(
    val totalChecks: Int,
    val successRate: Float,
    val currentStreak: Int,
    val bestStreak: Int,
    val averageIntervalDays: Float,
    val createdAt: Long,
    val versionSuccessCounts: List<VersionSuccessCount> = emptyList()
)

data class RecentActivity(
    val timestamp: Long,
    val success: Boolean
)

class EntryDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PasswordRepository(application)

    private val _entry = MutableStateFlow<PasswordEntry?>(null)
    val entry: StateFlow<PasswordEntry?> = _entry.asStateFlow()

    private val _calendarDays = MutableStateFlow<Map<LocalDate, DayStatus>>(emptyMap())
    val calendarDays: StateFlow<Map<LocalDate, DayStatus>> = _calendarDays.asStateFlow()

    private val _stats = MutableStateFlow<EntryStats?>(null)
    val stats: StateFlow<EntryStats?> = _stats.asStateFlow()

    private val _recentActivity = MutableStateFlow<List<RecentActivity>>(emptyList())
    val recentActivity: StateFlow<List<RecentActivity>> = _recentActivity.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private var allHistory: List<VerificationRecord> = emptyList()

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val loaded = repository.getEntry(entryId) ?: return@withContext
                _entry.value = loaded

                allHistory = repository.getHistoryForEntry(entryId)
                    .sortedByDescending { it.timestamp }

                computeCalendarDays()
                computeStats(loaded)
                computeRecentActivity()
            }
        }
    }

    fun navigateMonth(delta: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(delta.toLong())
        viewModelScope.launch(Dispatchers.Default) {
            computeCalendarDays()
        }
    }

    private fun computeCalendarDays() {
        val month = _currentMonth.value
        val zone = ZoneId.systemDefault()

        val dayMap = mutableMapOf<LocalDate, DayStatus>()

        // Group history records by day
        val recordsByDay = allHistory.groupBy { record ->
            Instant.ofEpochMilli(record.timestamp)
                .atZone(zone)
                .toLocalDate()
        }

        // Process only days in current month
        val startOfMonth = month.atDay(1)
        val endOfMonth = month.atEndOfMonth()

        for ((date, records) in recordsByDay) {
            if (date.isBefore(startOfMonth) || date.isAfter(endOfMonth)) continue

            val hasSuccess = records.any { it.success }
            val hasFailureOnly = !hasSuccess && records.any { !it.success }

            dayMap[date] = DayStatus(
                date = date,
                hasSuccess = hasSuccess,
                hasFailureOnly = hasFailureOnly
            )
        }

        _calendarDays.value = dayMap
    }

    private fun computeStats(entry: PasswordEntry) {
        val totalChecks = entry.totalAttempts
        val successRate = if (totalChecks > 0) {
            entry.successfulAttempts.toFloat() / totalChecks
        } else 0f

        // Compute average interval between verifications
        val successTimestamps = allHistory
            .filter { it.success }
            .map { it.timestamp }
            .sorted()

        val avgInterval = if (successTimestamps.size >= 2) {
            val intervals = successTimestamps.zipWithNext { a, b -> b - a }
            val avgMs = intervals.average()
            (avgMs / TimeUnit.DAYS.toMillis(1)).toFloat()
        } else 0f

        // Per-version success counts
        val versionCounts = if (entry.passwordVersions.size > 1) {
            allHistory
                .filter { it.success && it.matchedVersionLabel.isNotEmpty() }
                .groupBy { it.matchedVersionLabel }
                .map { (label, records) -> VersionSuccessCount(label, records.size) }
                .sortedByDescending { it.count }
        } else emptyList()

        _stats.value = EntryStats(
            totalChecks = totalChecks,
            successRate = successRate,
            currentStreak = entry.streak,
            bestStreak = entry.bestStreak,
            averageIntervalDays = avgInterval,
            createdAt = entry.createdAt,
            versionSuccessCounts = versionCounts
        )
    }

    private fun computeRecentActivity() {
        _recentActivity.value = allHistory
            .take(10)
            .map { RecentActivity(timestamp = it.timestamp, success = it.success) }
    }
}
