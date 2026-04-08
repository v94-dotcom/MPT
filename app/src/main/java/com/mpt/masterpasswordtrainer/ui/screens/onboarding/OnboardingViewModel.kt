package com.mpt.masterpasswordtrainer.ui.screens.onboarding

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mpt_prefs", Context.MODE_PRIVATE)

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val totalPages = 4

    fun setPage(page: Int) {
        _currentPage.value = page.coerceIn(0, totalPages - 1)
    }

    fun nextPage() {
        _currentPage.value = (_currentPage.value + 1).coerceAtMost(totalPages - 1)
    }

    fun skipToEnd() {
        _currentPage.value = totalPages - 1
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }
}
