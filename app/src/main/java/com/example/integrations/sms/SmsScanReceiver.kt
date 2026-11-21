package com.example.integrations.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.KavachApplication
import com.example.domain.models.ScanInput
import com.example.domain.models.Source
import com.example.integrations.notifications.NotificationsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class SmsScanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val app = context.applicationContext as? KavachApplication
        if (app == null) {
            pendingResult.finish()
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = messages.joinToString("") { it.messageBody.orEmpty() }.trim()
        if (body.isBlank()) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = app.repository.settingsFlow.first()
                if (!settings.smsAutoScanEnabled) return@launch

                // Dedupe identical SMS bodies within 60 seconds
                val keyHash = body.hashCode()
                val now = System.currentTimeMillis()
                val prevTs = recentSmsHashes[keyHash]
                if (prevTs != null && now - prevTs < 60_000L) {
                    return@launch
                }
                recentSmsHashes[keyHash] = now
                if (recentSmsHashes.size > 50) {
                    recentSmsHashes.entries.removeIf { now - it.value > 120_000L }
                }

                val result = app.repository.analyzeAndMaybeRecord(
                    ScanInput(text = body, source = Source.SMS, unexpectedSenderHint = true)
                )
                if (result.detectorAvailable && result.riskScore >= settings.notificationThreshold) {
                    NotificationsHelper.showSmsScamAlert(
                        context = context,
                        riskScore = result.riskScore,
                        riskLevel = result.riskLevel,
                        categoryName = result.scamCategory ?: "Suspicious Message",
                        previewTextForInAppResult = result.sourceText
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val recentSmsHashes = ConcurrentHashMap<Int, Long>()
    }
}
