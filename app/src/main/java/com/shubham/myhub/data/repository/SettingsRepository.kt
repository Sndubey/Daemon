package com.shubham.myhub.data.repository

import com.shubham.myhub.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val userPreferences: UserPreferences) {

    val isServiceEnabled: Flow<Boolean> = userPreferences.isServiceEnabled
    val logRetentionDays: Flow<Int> = userPreferences.logRetentionDays
    val autoDismissMuted: Flow<Boolean> = userPreferences.autoDismissMuted

    suspend fun setServiceEnabled(enabled: Boolean) {
        userPreferences.setServiceEnabled(enabled)
    }

    suspend fun setLogRetentionDays(days: Int) {
        userPreferences.setLogRetentionDays(days)
    }

    suspend fun setAutoDismissMuted(dismiss: Boolean) {
        userPreferences.setAutoDismissMuted(dismiss)
    }
}
