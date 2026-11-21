package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rakshakDataStore: DataStore<Preferences> by preferencesDataStore(name = "rakshakai_prefs")

data class RakshakSettings(
    val onboardingCompleted: Boolean = false,
    val appLanguage: String = "en", // "en" or "hi"
    val elderlyMode: Boolean = false,
    val historyEnabled: Boolean = true,
    val smsAutoScanEnabled: Boolean = false, // OFF by default
    val notificationThreshold: Int = 60,
    val biometricVaultLock: Boolean = false,
    val flagSecureEnabled: Boolean = false,
    val reducedMotion: Boolean = false,
    val bookmarkedTips: Set<String> = emptySet(),
    val simulatedRemotePrompt: Boolean = false,
    val simulatedScreenShare: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ELDERLY_MODE = booleanPreferencesKey("elderly_mode")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val SMS_AUTO_SCAN = booleanPreferencesKey("sms_auto_scan")
        val NOTIF_THRESHOLD = intPreferencesKey("notif_threshold")
        val BIOMETRIC_VAULT = booleanPreferencesKey("biometric_vault")
        val FLAG_SECURE = booleanPreferencesKey("flag_secure")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val BOOKMARKED_TIPS = stringSetPreferencesKey("bookmarked_tips")
        val SIM_REMOTE_PROMPT = booleanPreferencesKey("sim_remote_prompt")
        val SIM_SCREEN_SHARE = booleanPreferencesKey("sim_screen_share")
    }

    val settingsFlow: Flow<RakshakSettings> = context.rakshakDataStore.data.map { p ->
        RakshakSettings(
            onboardingCompleted = p[Keys.ONBOARDING_DONE] ?: false,
            appLanguage = p[Keys.APP_LANGUAGE] ?: "en",
            elderlyMode = p[Keys.ELDERLY_MODE] ?: false,
            historyEnabled = p[Keys.HISTORY_ENABLED] ?: true,
            smsAutoScanEnabled = p[Keys.SMS_AUTO_SCAN] ?: false,
            notificationThreshold = p[Keys.NOTIF_THRESHOLD] ?: 60,
            biometricVaultLock = p[Keys.BIOMETRIC_VAULT] ?: false,
            flagSecureEnabled = p[Keys.FLAG_SECURE] ?: false,
            reducedMotion = p[Keys.REDUCED_MOTION] ?: false,
            bookmarkedTips = p[Keys.BOOKMARKED_TIPS] ?: emptySet(),
            simulatedRemotePrompt = p[Keys.SIM_REMOTE_PROMPT] ?: false,
            simulatedScreenShare = p[Keys.SIM_SCREEN_SHARE] ?: false
        )
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.rakshakDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setAppLanguage(langCode: String) {
        context.rakshakDataStore.edit { it[Keys.APP_LANGUAGE] = langCode }
    }

    suspend fun setElderlyMode(enabled: Boolean) {
        context.rakshakDataStore.edit { it[Keys.ELDERLY_MODE] = enabled }
    }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.rakshakDataStore.edit { it[Keys.HISTORY_ENABLED] = enabled }
    }

    suspend fun setSmsAutoScanEnabled(enabled: Boolean) {
        context.rakshakDataStore.edit { it[Keys.SMS_AUTO_SCAN] = enabled }
    }

    suspend fun setNotificationThreshold(threshold: Int) {
        context.rakshakDataStore.edit { it[Keys.NOTIF_THRESHOLD] = threshold }
    }

    suspend fun setBiometricVaultLock(enabled: Boolean) {
        context.rakshakDataStore.edit { it[Keys.BIOMETRIC_VAULT] = enabled }
    }

    suspend fun setFlagSecureEnabled(enabled: Boolean) {
        context.rakshakDataStore.edit { it[Keys.FLAG_SECURE] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.rakshakDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun toggleBookmarkTip(tipId: String) {
        context.rakshakDataStore.edit { prefs ->
            val curr = prefs[Keys.BOOKMARKED_TIPS] ?: emptySet()
            prefs[Keys.BOOKMARKED_TIPS] = if (tipId in curr) curr - tipId else curr + tipId
        }
    }

    suspend fun setSimulatedSignals(remotePrompt: Boolean, screenShare: Boolean) {
        context.rakshakDataStore.edit {
            it[Keys.SIM_REMOTE_PROMPT] = remotePrompt
            it[Keys.SIM_SCREEN_SHARE] = screenShare
        }
    }

    suspend fun clearAllPreferences() {
        context.rakshakDataStore.edit { it.clear() }
    }
}
