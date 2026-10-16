package com.shubham.myhub.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    companion object {
        val IS_SERVICE_ENABLED = booleanPreferencesKey("is_service_enabled")
        val LOG_RETENTION_DAYS = intPreferencesKey("log_retention_days")
        val AUTO_DISMISS_MUTED = booleanPreferencesKey("auto_dismiss_muted")
    }

    val isServiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_SERVICE_ENABLED] ?: true
    }

    val logRetentionDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LOG_RETENTION_DAYS] ?: 30
    }

    val autoDismissMuted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_DISMISS_MUTED] ?: true
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setLogRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[LOG_RETENTION_DAYS] = days
        }
    }

    suspend fun setAutoDismissMuted(dismiss: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_DISMISS_MUTED] = dismiss
        }
    }
}
