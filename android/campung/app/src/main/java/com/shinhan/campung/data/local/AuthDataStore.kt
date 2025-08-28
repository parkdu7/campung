package com.shinhan.campung.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

class AuthDataStore(private val context: Context) {
    companion object {
        private val KEY_TOKEN     = stringPreferencesKey("auth_token")
        private val KEY_USER_ID   = stringPreferencesKey("user_id")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        private val KEY_NICKNAME  = stringPreferencesKey("nickname")   // ✅ 추가
    }

    val tokenFlow: Flow<String?>     = context.dataStore.data.map { it[KEY_TOKEN] }
    val userIdFlow: Flow<String?>    = context.dataStore.data.map { it[KEY_USER_ID] }
    val fcmTokenFlow: Flow<String?>  = context.dataStore.data.map { it[KEY_FCM_TOKEN] }
    val nicknameFlow: Flow<String?>  = context.dataStore.data.map { it[KEY_NICKNAME] } // ✅ 추가

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { it[KEY_USER_ID] = userId }
    }

    suspend fun saveFcmToken(fcmToken: String) {
        context.dataStore.edit { it[KEY_FCM_TOKEN] = fcmToken }
    }

    suspend fun saveNickname(nickname: String) {                       // ✅ 추가
        context.dataStore.edit { it[KEY_NICKNAME] = nickname }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USER_ID)
            it.remove(KEY_FCM_TOKEN)
            it.remove(KEY_NICKNAME)                                     // ✅ 추가
        }
    }
}

suspend fun logNicknameOnce(authDataStore: AuthDataStore) {
    val nickname = authDataStore.nicknameFlow.firstOrNull()
    Log.d("AuthDataStore", "nickname = ${nickname ?: "null"}")
}
