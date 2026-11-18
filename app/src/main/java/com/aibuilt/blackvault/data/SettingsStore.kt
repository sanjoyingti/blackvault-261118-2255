package com.aibuilt.blackvault.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "blackvault_settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_MOTION = booleanPreferencesKey("stealth_motion")
        private val KEY_WEBHOOK = stringPreferencesKey("webhook_url")
    }

    val stealthMotion: Flow<Boolean> = context.dataStore.data.map { it[KEY_MOTION] ?: true }
    val webhookUrl: Flow<String> = context.dataStore.data.map { it[KEY_WEBHOOK] ?: "" }

    suspend fun setStealthMotion(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MOTION] = enabled }
    }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { it[KEY_WEBHOOK] = url.trim() }
    }
}
