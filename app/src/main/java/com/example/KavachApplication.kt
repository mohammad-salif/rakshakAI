package com.example

import android.app.Application
import com.example.data.database.RakshakDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repositories.RakshakRepository
import com.example.detection.engine.RuleBasedScamEngine
import com.example.integrations.AssetRuleSource
import com.example.integrations.notifications.NotificationsHelper

class KavachApplication : Application() {
    val database by lazy { RakshakDatabase.getInstance(this) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val scamEngine by lazy { RuleBasedScamEngine(AssetRuleSource(this)) }
    val repository by lazy {
        RakshakRepository(
            context = this,
            dao = database.rakshakDao(),
            prefs = preferencesRepository,
            engine = scamEngine
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationsHelper.ensureChannel(this)
    }
}
