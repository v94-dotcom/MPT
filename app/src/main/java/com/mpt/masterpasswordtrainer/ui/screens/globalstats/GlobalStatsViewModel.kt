package com.mpt.masterpasswordtrainer.ui.screens.globalstats

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
import java.util.concurrent.TimeUnit

data class GlobalStats(
    val totalEntries: Int,
    val totalChecks: Int,
    val globalSuccessRate: Float,
    val bestActiveStreak: Int,
    val bestActiveStreakService: String,
    val weeklyActivity: List<DayActivity>,
    val monthlyTrend: List<MonthActivity>,
    val entryComparisons: List<EntryComparison>,
    val funStats: FunStats
)

data class DayActivity(
    val label: String,
    val date: LocalDate,
    val successes: Int,
    val failures: Int
)

data class MonthActivity(
    val label: String,
    val yearMonth: YearMonth,
    val total: Int
)

data class EntryComparison(
    val entry: PasswordEntry,
    val successRate: Float,
    val totalChecks: Int
)

data class FunStats(
    val mostPracticedName: String,
    val mostPracticedCount: Int,
    val mostReliableName: String,
    val mostReliableRate: Float,
    val needsWorkName: String,
    val needsWorkRate: Float,
    val totalDaysTrained: Int,
    val longestGapDays: Int,
    val longestGapService: String
)

class GlobalStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PasswordRepository(application)

    private val _stats = MutableStateFlow<GlobalStats?>(null)
    val stats: StateFlow<GlobalStats?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.Default) {
                val entries = repository.getAllEntries()
                val allHistory = repository.getAllHistory()
                _stats.value = computeGlobalStats(entries, allHistory)
            }
            _isLoading.value = false
        }
    }

    private fun computeGlobalStats(
        entries: List<PasswordEntry>,
        allHistory: List<VerificationRecord>
    ): GlobalStats {
        val zone = ZoneId.systemDefault()

        val totalChecks = entries.sumOf { it.totalAttempts }
        val totalSuccesses = entries.sumOf { it.successfulAttempts }
        val globalSuccessRate = if (totalChecks > 0) totalSuccesses.toFloat() / totalChecks else 0f

        // Best active streak
        val bestActiveEntry = entries.maxByOrNull { it.streak }
        val bestActiveStreak = bestActiveEntry?.streak ?: 0
        val bestActiveStreakService = bestActiveEntry?.serviceName ?: ""

        // Weekly activity — last 7 days
        val today = LocalDate.now()
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val weeklyActivity = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dayRecords = allHistory.filter { record ->
                Instant.ofEpochMilli(record.timestamp)
                    .atZone(zone)
                    .toLocalDate() == date
            }
            DayActivity(
                label = dayLabels[date.dayOfWeek.value - 1],
                date = date,
                successes = dayRecords.count { it.success },
                failures = dayRecords.count { !it.success }
            )
        }

        // Monthly trend — last 6 months
        val currentMonth = YearMonth.now()
        val monthlyTrend = (5 downTo 0).map { monthsAgo ->
            val ym = currentMonth.minusMonths(monthsAgo.toLong())
            val monthRecords = allHistory.filter { record ->
                val recordYm = YearMonth.from(
                    Instant.ofEpochMilli(record.timestamp).atZone(zone).toLocalDate()
                )
                recordYm == ym
            }
            MonthActivity(
                label = ym.month.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() },
                yearMonth = ym,
                total = monthRecords.size
            )
        }

        // Per-entry comparison sorted by success rate (highest first)
        val entryComparisons = entries.map { entry ->
            val rate = if (entry.totalAttempts > 0) {
                entry.successfulAttempts.toFloat() / entry.totalAttempts
            } else 0f
            EntryComparison(
                entry = entry,
                successRate = rate,
                totalChecks = entry.totalAttempts
            )
        }.sortedByDescending { it.successRate }

        // Fun stats
        val mostPracticed = entries.maxByOrNull { it.totalAttempts }
        val mostReliable = entries
            .filter { it.totalAttempts >= 1 }
            .maxByOrNull { it.successfulAttempts.toFloat() / it.totalAttempts }
        val needsWork = entries
            .filter { it.totalAttempts >= 1 }
            .minByOrNull { it.successfulAttempts.toFloat() / it.totalAttempts }
        val needsWorkRate = if (needsWork != null && needsWork.totalAttempts > 0) {
            needsWork.successfulAttempts.toFloat() / needsWork.totalAttempts
        } else 1f

        // Total unique days with at least one verification
        val totalDaysTrained = allHistory
            .map { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .toSet()
            .size

        // Longest gap between verifications for any entry
        var longestGapDays = 0
        var longestGapService = ""
        for (entry in entries) {
            val timestamps = allHistory
                .filter { it.entryId == entry.id }
                .map { it.timestamp }
                .sorted()
            if (timestamps.size >= 2) {
                val maxGap = timestamps.zipWithNext { a, b -> b - a }.max()
                val gapDays = (maxGap / TimeUnit.DAYS.toMillis(1)).toInt()
                if (gapDays > longestGapDays) {
                    longestGapDays = gapDays
                    longestGapService = entry.serviceName
                }
            }
        }

        val funStats = FunStats(
            mostPracticedName = mostPracticed?.serviceName ?: "",
            mostPracticedCount = mostPracticed?.totalAttempts ?: 0,
            mostReliableName = mostReliable?.serviceName ?: "",
            mostReliableRate = if (mostReliable != null && mostReliable.totalAttempts > 0)
                mostReliable.successfulAttempts.toFloat() / mostReliable.totalAttempts else 0f,
            needsWorkName = if (needsWorkRate < 0.8f) needsWork?.serviceName ?: "" else "",
            needsWorkRate = needsWorkRate,
            totalDaysTrained = totalDaysTrained,
            longestGapDays = longestGapDays,
            longestGapService = longestGapService
        )

        return GlobalStats(
            totalEntries = entries.size,
            totalChecks = totalChecks,
            globalSuccessRate = globalSuccessRate,
            bestActiveStreak = bestActiveStreak,
            bestActiveStreakService = bestActiveStreakService,
            weeklyActivity = weeklyActivity,
            monthlyTrend = monthlyTrend,
            entryComparisons = entryComparisons,
            funStats = funStats
        )
    }
}
