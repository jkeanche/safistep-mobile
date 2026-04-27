package com.safistep.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safistep_prefs")

@Singleton
class SafiStepPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.dataStore

    companion object {
        val KEY_TOKEN               = stringPreferencesKey("auth_token")
        val KEY_USER_ID             = longPreferencesKey("user_id")
        val KEY_USER_PHONE          = stringPreferencesKey("user_phone")
        val KEY_USER_NAME           = stringPreferencesKey("user_name")
        val KEY_SUB_STATUS          = stringPreferencesKey("sub_status")
        val KEY_SUB_EXPIRES         = stringPreferencesKey("sub_expires")
        val KEY_ONBOARDING_DONE     = booleanPreferencesKey("onboarding_done")
        val KEY_TEMP_TOKEN          = stringPreferencesKey("temp_token")
        val KEY_TEMP_PHONE          = stringPreferencesKey("temp_phone")
        val KEY_PROTECTION_ENABLED  = booleanPreferencesKey("protection_enabled")
        val KEY_LAST_SYNC           = longPreferencesKey("last_sync")
    }

    val token: Flow<String?> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_TOKEN] }

    val isLoggedIn: Flow<Boolean> = token.map { !it.isNullOrBlank() }

    val userPhone: Flow<String?> = ds.data.map { it[KEY_USER_PHONE] }

    val subscriptionStatus: Flow<String> = ds.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SUB_STATUS] ?: "inactive" }

    val subscriptionExpires: Flow<String?> = ds.data.map { it[KEY_SUB_EXPIRES] }

    val onboardingDone: Flow<Boolean> = ds.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    val protectionEnabled: Flow<Boolean> = ds.data.map { it[KEY_PROTECTION_ENABLED] ?: true }

    suspend fun saveAuthSession(token: String, userId: Long, phone: String, name: String?) {
        ds.edit { prefs ->
            prefs[KEY_TOKEN]      = token
            prefs[KEY_USER_ID]    = userId
            prefs[KEY_USER_PHONE] = phone
            if (name != null) prefs[KEY_USER_NAME] = name
        }
    }

    suspend fun saveSubscription(status: String, expiresAt: String?) {
        ds.edit { prefs ->
            prefs[KEY_SUB_STATUS] = status
            if (expiresAt != null) prefs[KEY_SUB_EXPIRES] = expiresAt
            else prefs.remove(KEY_SUB_EXPIRES)
        }
    }

    suspend fun saveTempSession(tempToken: String, phone: String) {
        ds.edit { prefs ->
            prefs[KEY_TEMP_TOKEN] = tempToken
            prefs[KEY_TEMP_PHONE] = phone
        }
    }

    suspend fun getTempToken(): String? = ds.data.map { it[KEY_TEMP_TOKEN] }.catch { emit(null) }
        .let { flow ->
            var result: String? = null
            flow.collect { result = it }
            result
        }

    suspend fun getTempPhone(): String? = ds.data.map { it[KEY_TEMP_PHONE] }.catch { emit(null) }
        .let { flow ->
            var result: String? = null
            flow.collect { result = it }
            result
        }

    suspend fun clearTempSession() {
        ds.edit { prefs ->
            prefs.remove(KEY_TEMP_TOKEN)
            prefs.remove(KEY_TEMP_PHONE)
        }
    }

    suspend fun setOnboardingDone() {
        ds.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        ds.edit { it[KEY_PROTECTION_ENABLED] = enabled }
    }

    suspend fun setLastSync(timestamp: Long) {
        ds.edit { it[KEY_LAST_SYNC] = timestamp }
    }

    suspend fun clearSession() {
        ds.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_PHONE)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_SUB_STATUS)
            prefs.remove(KEY_SUB_EXPIRES)
        }
    }

    // Synchronous reads for use in non-coroutine contexts (services)
    suspend fun getTokenSync(): String? = ds.data.map { it[KEY_TOKEN] }.catch { emit(null) }
        .let { flow ->
            var result: String? = null
            flow.collect { result = it }
            result
        }

    suspend fun getSubStatusSync(): String = ds.data.map { it[KEY_SUB_STATUS] ?: "inactive" }
        .catch { emit("inactive") }
        .let { flow ->
            var result = "inactive"
            flow.collect { result = it }
            result
        }
}
