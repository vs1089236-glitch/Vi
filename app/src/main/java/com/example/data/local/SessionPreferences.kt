package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "commissionwala_session")

data class UserSession(
    val userId: String?,
    val workspaceMode: String // "USER" or "ADMIN"
)

class SessionPreferences(private val context: Context) {
    private val keyUserId = stringPreferencesKey("logged_in_user_id")
    private val keyWorkspaceMode = stringPreferencesKey("workspace_mode")

    val sessionFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        UserSession(
            userId = prefs[keyUserId],
            workspaceMode = prefs[keyWorkspaceMode] ?: "USER"
        )
    }

    suspend fun saveSession(userId: String?, workspaceMode: String = "USER") {
        context.dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(keyUserId)
                prefs[keyWorkspaceMode] = "USER"
            } else {
                prefs[keyUserId] = userId
                prefs[keyWorkspaceMode] = workspaceMode
            }
        }
    }

    suspend fun setWorkspaceMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[keyWorkspaceMode] = mode
        }
    }
}
