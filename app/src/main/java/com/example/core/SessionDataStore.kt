package com.example.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kodegit_session")

data class EditorPreferences(
    val fontSizeSp: Int = 13,
    val wordWrap: Boolean = false
)

data class PendingOAuthState(
    val state: String,
    val codeVerifier: String
)

class SessionDataStore(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("github_access_token")
        val OAUTH_STATE = stringPreferencesKey("oauth_state")
        val OAUTH_VERIFIER = stringPreferencesKey("oauth_code_verifier")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val EDITOR_WORD_WRAP = booleanPreferencesKey("editor_word_wrap")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCESS_TOKEN]
    }

    val editorPreferencesFlow: Flow<EditorPreferences> = context.dataStore.data.map { prefs ->
        EditorPreferences(
            fontSizeSp = (prefs[Keys.EDITOR_FONT_SIZE] ?: 13).coerceIn(10, 22),
            wordWrap = prefs[Keys.EDITOR_WORD_WRAP] ?: false
        )
    }

    suspend fun getAccessToken(): String? = accessTokenFlow.first()

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = token
            prefs.remove(Keys.OAUTH_STATE)
            prefs.remove(Keys.OAUTH_VERIFIER)
        }
    }

    suspend fun savePendingOAuth(state: String, codeVerifier: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OAUTH_STATE] = state
            prefs[Keys.OAUTH_VERIFIER] = codeVerifier
        }
    }

    suspend fun getPendingOAuth(): PendingOAuthState? {
        val prefs = context.dataStore.data.first()
        val state = prefs[Keys.OAUTH_STATE] ?: return null
        val verifier = prefs[Keys.OAUTH_VERIFIER] ?: return null
        return PendingOAuthState(state = state, codeVerifier = verifier)
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.OAUTH_STATE)
            prefs.remove(Keys.OAUTH_VERIFIER)
        }
    }

    suspend fun updateEditorFontSize(sizeSp: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EDITOR_FONT_SIZE] = sizeSp.coerceIn(10, 22)
        }
    }

    suspend fun updateWordWrap(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EDITOR_WORD_WRAP] = enabled
        }
    }
}
