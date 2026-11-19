package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "s_entrance_quiz_prefs")

class ApiKeyPreferences(private val context: Context) {

    private val apiKeyPref = stringPreferencesKey("gemini_api_key")
    private val lastTopicPref = stringPreferencesKey("last_topic")
    private val darkModePref = booleanPreferencesKey("dark_mode_enabled")

    val savedApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[apiKeyPref].orEmpty()
    }

    val savedLastTopic: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[lastTopicPref].orEmpty()
    }

    val darkModeOverride: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[darkModePref]
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[apiKeyPref] = apiKey.trim()
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { prefs ->
            prefs.remove(apiKeyPref)
        }
    }

    suspend fun saveLastTopic(topic: String) {
        context.dataStore.edit { prefs ->
            prefs[lastTopicPref] = topic.trim()
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[darkModePref] = enabled
        }
    }
}
