package com.example.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.KavachApplication
import com.example.data.database.IncidentEntity
import com.example.data.database.QuarantineEntity
import com.example.data.database.ScanHistoryEntity
import com.example.data.database.TrustedAllowlistEntity
import com.example.data.database.TrustedContactEntity
import com.example.data.database.VaultItemEntity
import com.example.data.preferences.RakshakSettings
import com.example.data.repositories.RakshakRepository
import com.example.detection.DemoSamples
import com.example.detection.conversation.ConversationSplitter
import com.example.detection.risk.CoercionPressureEstimator
import com.example.detection.risk.CrossSignalEngine
import com.example.domain.models.ChatSender
import com.example.domain.models.CoercionAnswers
import com.example.domain.models.CoercionAssessment
import com.example.domain.models.ConversationAnalysisResult
import com.example.domain.models.ConversationMessage
import com.example.domain.models.CrossSignalInput
import com.example.domain.models.CrossSignalReport
import com.example.domain.models.FeedbackOption
import com.example.domain.models.PreActionDecision
import com.example.domain.models.PreActionInput
import com.example.domain.models.PreActionType
import com.example.domain.models.RiskLevel
import com.example.domain.models.ScanInput
import com.example.domain.models.ScanResult
import com.example.domain.models.Source
import com.example.integrations.appscan.RemoteAccessAppScanner
import com.example.integrations.ocr.ScreenshotOcrProcessor
import com.example.integrations.share.ShareIntentsHelper
import com.example.integrations.tts.TtsHelper
import com.example.privacy.data_management.PrivacyAuditHelper
import com.example.privacy.data_management.RuntimePermissionAudit
import com.example.privacy.data_management.StorageStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class RakshakUiState(
    val settings: RakshakSettings = RakshakSettings(),
    val history: List<ScanHistoryEntity> = emptyList(),
    val vaultItems: List<VaultItemEntity> = emptyList(),
    val quarantineItems: List<QuarantineEntity> = emptyList(),
    val trustedContacts: List<TrustedContactEntity> = emptyList(),
    val incidents: List<IncidentEntity> = emptyList(),
    val allowlist: List<TrustedAllowlistEntity> = emptyList(),
    val messageInput: String = "",
    val languageHint: String = "auto",
    val unexpectedSenderHint: Boolean = false,
    val linkInput: String = "",
    val linkClaimedBrandId: String? = null,
    val qrInput: String = "",
    val conversationRawInput: String = "",
    val conversationMessages: List<ConversationMessage> = emptyList(),
    val callTranscriptInput: String = "",
    val isCallDemoRunning: Boolean = false,
    val callDemoRevealedLines: List<String> = emptyList(),
    val ocrExtractedText: String = "",
    val ocrQrPayloads: List<String> = emptyList(),
    val ocrImageUri: Uri? = null,
    val ocrEmptyWarning: Boolean = false,
    val isProcessingOcr: Boolean = false,
    val latestScanResult: ScanResult? = null,
    val latestConversationResult: ConversationAnalysisResult? = null,
    val preActionDecision: PreActionDecision? = null,
    val coercionAnswers: CoercionAnswers = CoercionAnswers(),
    val coercionAssessment: CoercionAssessment? = null,
    val crossSignalReport: CrossSignalReport = CrossSignalEngine.evaluate(CrossSignalInput()),
    val installedRemoteApps: List<String> = emptyList(),
    val permissionAudit: RuntimePermissionAudit? = null,
    val storageStats: StorageStats = StorageStats(0, 0, 0, 0, 0L),
    val offlineTestResults: List<Pair<String, ScanResult>> = emptyList(),
    val vaultUnlocked: Boolean = false,
    val toastMessage: String? = null,
    val pendingNavigationEvent: String? = null
)

class RakshakViewModel(
    application: Application,
    val repository: RakshakRepository
) : AndroidViewModel(application) {

    private val localState = MutableStateFlow(LocalWorkState())
    private val ttsHelper = TtsHelper(application)

    private data class LocalWorkState(
        val messageInput: String = "",
        val languageHint: String = "auto",
        val unexpectedSenderHint: Boolean = false,
        val linkInput: String = "",
        val linkClaimedBrandId: String? = null,
        val qrInput: String = "",
        val conversationRawInput: String = "",
        val conversationMessages: List<ConversationMessage> = emptyList(),
        val callTranscriptInput: String = "",
        val isCallDemoRunning: Boolean = false,
        val callDemoRevealedLines: List<String> = emptyList(),
        val ocrExtractedText: String = "",
        val ocrQrPayloads: List<String> = emptyList(),
        val ocrImageUri: Uri? = null,
        val ocrEmptyWarning: Boolean = false,
        val isProcessingOcr: Boolean = false,
        val latestScanResult: ScanResult? = null,
        val latestConversationResult: ConversationAnalysisResult? = null,
        val preActionDecision: PreActionDecision? = null,
        val coercionAnswers: CoercionAnswers = CoercionAnswers(),
        val coercionAssessment: CoercionAssessment? = null,
        val installedRemoteApps: List<String> = emptyList(),
        val permissionAudit: RuntimePermissionAudit? = null,
        val storageStats: StorageStats = StorageStats(0, 0, 0, 0, 0L),
        val offlineTestResults: List<Pair<String, ScanResult>> = emptyList(),
        val vaultUnlocked: Boolean = false,
        val toastMessage: String? = null,
        val pendingNavigationEvent: String? = null
    )

    private val dbFlow = combine(
        repository.historyFlow,
        repository.vaultFlow,
        repository.quarantineFlow,
        repository.contactsFlow,
        repository.incidentsFlow
    ) { history, vault, quarantine, contacts, incidents ->
        DbBundle(history, vault, quarantine, contacts, incidents)
    }

    private data class DbBundle(
        val history: List<ScanHistoryEntity>,
        val vault: List<VaultItemEntity>,
        val quarantine: List<QuarantineEntity>,
        val contacts: List<TrustedContactEntity>,
        val incidents: List<IncidentEntity>
    )

    val uiState: StateFlow<RakshakUiState> = combine(
        repository.settingsFlow,
        dbFlow,
        repository.allowlistFlow,
        localState
    ) { settings, db, allowlist, local ->
        val latestMsgScore = local.latestScanResult?.riskScore ?: 0
        val convScore = local.latestConversationResult?.cumulativeScore ?: 0
        val callScore = if (local.latestScanResult?.source == Source.TRANSCRIPT) local.latestScanResult.riskScore else 0
        val linkScore = local.latestScanResult?.urlResults?.maxOfOrNull { it.score } ?: 0
        val preScore = local.preActionDecision?.combinedRiskScore ?: 0

        val crossReport = CrossSignalEngine.evaluate(
            CrossSignalInput(
                messageRisk = latestMsgScore,
                conversationRisk = convScore,
                linkRisk = linkScore,
                callTranscriptRisk = callScore,
                installedRemoteApps = local.installedRemoteApps,
                simulatedRemoteAppPrompt = settings.simulatedRemotePrompt,
                simulatedScreenShareOn = settings.simulatedScreenShare,
                preActionRisk = preScore
            )
        )

        val stats = StorageStats(
            historyCount = db.history.size,
            vaultCount = db.vault.size,
            quarantineCount = db.quarantine.size,
            contactsCount = db.contacts.size,
            totalDiskBytes = PrivacyAuditHelper.calculateAppDataBytes(getApplication())
        )

        RakshakUiState(
            settings = settings,
            history = db.history,
            vaultItems = db.vault,
            quarantineItems = db.quarantine,
            trustedContacts = db.contacts,
            incidents = db.incidents,
            allowlist = allowlist,
            messageInput = local.messageInput,
            languageHint = local.languageHint,
            unexpectedSenderHint = local.unexpectedSenderHint,
            linkInput = local.linkInput,
            linkClaimedBrandId = local.linkClaimedBrandId,
            qrInput = local.qrInput,
            conversationRawInput = local.conversationRawInput,
            conversationMessages = local.conversationMessages,
            callTranscriptInput = local.callTranscriptInput,
            isCallDemoRunning = local.isCallDemoRunning,
            callDemoRevealedLines = local.callDemoRevealedLines,
            ocrExtractedText = local.ocrExtractedText,
            ocrQrPayloads = local.ocrQrPayloads,
            ocrImageUri = local.ocrImageUri,
            ocrEmptyWarning = local.ocrEmptyWarning,
            isProcessingOcr = local.isProcessingOcr,
            latestScanResult = local.latestScanResult,
            latestConversationResult = local.latestConversationResult,
            preActionDecision = local.preActionDecision,
            coercionAnswers = local.coercionAnswers,
            coercionAssessment = local.coercionAssessment,
            crossSignalReport = crossReport,
            installedRemoteApps = local.installedRemoteApps,
            permissionAudit = local.permissionAudit,
            storageStats = stats,
            offlineTestResults = local.offlineTestResults,
            vaultUnlocked = local.vaultUnlocked,
            toastMessage = local.toastMessage,
            pendingNavigationEvent = local.pendingNavigationEvent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RakshakUiState()
    )

    init {
        refreshAudits()
    }

    override fun onCleared() {
        ttsHelper.shutdown()
        super.onCleared()
    }

    fun refreshAudits() {
        val ctx = getApplication<Application>()
        val perms = PrivacyAuditHelper.inspectPermissions(ctx)
        val remoteApps = RemoteAccessAppScanner.scanInstalledRemoteApps(ctx)
        localState.update { it.copy(permissionAudit = perms, installedRemoteApps = remoteApps) }
    }

    // Settings & Onboarding
    fun completeOnboarding() = viewModelScope.launch { repository.prefs.setOnboardingCompleted(true) }
    fun setLanguage(code: String) = viewModelScope.launch { repository.prefs.setAppLanguage(code) }
    fun setElderlyMode(enabled: Boolean) = viewModelScope.launch { repository.prefs.setElderlyMode(enabled) }
    fun setHistoryEnabled(enabled: Boolean) = viewModelScope.launch { repository.prefs.setHistoryEnabled(enabled) }
    fun setSmsAutoScan(enabled: Boolean) = viewModelScope.launch { repository.prefs.setSmsAutoScanEnabled(enabled) }
    fun setNotificationThreshold(threshold: Int) = viewModelScope.launch { repository.prefs.setNotificationThreshold(threshold) }
    fun setBiometricVaultLock(enabled: Boolean) = viewModelScope.launch {
        repository.prefs.setBiometricVaultLock(enabled)
        if (!enabled) localState.update { it.copy(vaultUnlocked = true) }
    }
    fun setFlagSecure(enabled: Boolean) = viewModelScope.launch { repository.prefs.setFlagSecureEnabled(enabled) }
    fun setReducedMotion(enabled: Boolean) = viewModelScope.launch { repository.prefs.setReducedMotion(enabled) }
    fun toggleBookmarkTip(tipId: String) = viewModelScope.launch { repository.prefs.toggleBookmarkTip(tipId) }
    fun setSimulatedSignals(remotePrompt: Boolean, screenShare: Boolean) = viewModelScope.launch {
        repository.prefs.setSimulatedSignals(remotePrompt, screenShare)
    }

    // Input updates
    fun updateMessageInput(v: String) = localState.update { it.copy(messageInput = v) }
    fun updateLanguageHint(v: String) = localState.update { it.copy(languageHint = v) }
    fun updateUnexpectedSenderHint(v: Boolean) = localState.update { it.copy(unexpectedSenderHint = v) }
    fun updateLinkInput(v: String) = localState.update { it.copy(linkInput = v) }
    fun updateLinkClaimedBrand(id: String?) = localState.update { it.copy(linkClaimedBrandId = id) }
    fun updateQrInput(v: String) = localState.update { it.copy(qrInput = v) }
    fun updateConversationRawInput(v: String) = localState.update { it.copy(conversationRawInput = v) }
    fun updateCallTranscriptInput(v: String) = localState.update { it.copy(callTranscriptInput = v) }
    fun updateOcrText(v: String) = localState.update { it.copy(ocrExtractedText = v) }

    fun addConversationTurn(sender: ChatSender, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val msg = ConversationMessage(
            id = UUID.randomUUID().toString(),
            sender = sender,
            senderLabel = if (sender == ChatSender.THEM) "Them" else "Me",
            text = clean
        )
        localState.update { it.copy(conversationMessages = it.conversationMessages + msg) }
    }

    fun splitConversationFromRaw() {
        val raw = localState.value.conversationRawInput
        val msgs = ConversationSplitter.split(raw)
        localState.update { it.copy(conversationMessages = msgs) }
    }

    fun clearConversation() {
        localState.update {
            it.copy(conversationRawInput = "", conversationMessages = emptyList(), latestConversationResult = null)
        }
    }

    fun deleteConversationTurn(id: String) {
        localState.update { state ->
            state.copy(conversationMessages = state.conversationMessages.filterNot { it.id == id })
        }
    }

    // Core Scan Runners
    fun scanMessage(
        textOverride: String? = null,
        source: Source = Source.MANUAL,
        onComplete: (ScanResult) -> Unit
    ) {
        val text = textOverride ?: localState.value.messageInput
        viewModelScope.launch {
            val res = repository.analyzeAndMaybeRecord(
                ScanInput(
                    text = text,
                    source = source,
                    languageHint = localState.value.languageHint,
                    unexpectedSenderHint = localState.value.unexpectedSenderHint
                )
            )
            val coercion = CoercionPressureEstimator.estimate(
                stagesReached = res.stagesReached,
                tactics = res.manipulation,
                answers = localState.value.coercionAnswers,
                isHindiOutput = uiState.value.settings.appLanguage.startsWith("hi")
            )
            localState.update {
                it.copy(
                    messageInput = text,
                    latestScanResult = res,
                    coercionAssessment = coercion
                )
            }
            onComplete(res)
        }
    }

    fun scanLink(onComplete: (ScanResult) -> Unit) {
        val url = localState.value.linkInput.trim()
        val brandId = localState.value.linkClaimedBrandId
        viewModelScope.launch {
            val res = repository.analyzeUrlAndMaybeRecord(url, brandId)
            localState.update { it.copy(latestScanResult = res) }
            onComplete(res)
        }
    }

    fun scanQrPayload(payloadOverride: String? = null, onComplete: (ScanResult) -> Unit) {
        val payload = payloadOverride ?: localState.value.qrInput.trim()
        val brandId = localState.value.linkClaimedBrandId
        viewModelScope.launch {
            val res = repository.analyzeQrAndMaybeRecord(payload, brandId)
            localState.update { it.copy(qrInput = payload, latestScanResult = res) }
            onComplete(res)
        }
    }

    fun analyzeConversation(onComplete: (ConversationAnalysisResult) -> Unit) {
        viewModelScope.launch {
            val existing = localState.value.conversationMessages
            val messages = if (existing.isNotEmpty()) {
                existing
            } else {
                ConversationSplitter.split(localState.value.conversationRawInput)
            }
            if (messages.isEmpty()) {
                showToast("Paste or add at least one message to inspect.")
                return@launch
            }
            val convRes = repository.analyzeConversationAndMaybeRecord(messages, localState.value.coercionAnswers)
            localState.update {
                it.copy(
                    conversationMessages = messages,
                    latestConversationResult = convRes,
                    latestScanResult = convRes.combinedResult,
                    coercionAssessment = convRes.coercionAssessment
                )
            }
            onComplete(convRes)
        }
    }

    fun analyzeCallTranscript(onComplete: (ScanResult) -> Unit) {
        val text = localState.value.callTranscriptInput.trim()
        scanMessage(textOverride = text, source = Source.TRANSCRIPT, onComplete = onComplete)
    }

    fun runScriptedDemoCall() {
        if (localState.value.isCallDemoRunning) return
        viewModelScope.launch {
            localState.update {
                it.copy(isCallDemoRunning = true, callDemoRevealedLines = emptyList(), callTranscriptInput = "")
            }
            val accumulated = StringBuilder()
            for (line in DemoSamples.scriptedDemoCallLines) {
                accumulated.appendLine(line)
                val currentText = accumulated.toString().trim()
                val conv = repository.analyzeConversationAndMaybeRecord(
                    ConversationSplitter.split(currentText),
                    localState.value.coercionAnswers
                )
                localState.update { state ->
                    state.copy(
                        callDemoRevealedLines = state.callDemoRevealedLines + line,
                        callTranscriptInput = currentText,
                        latestConversationResult = conv,
                        latestScanResult = conv.combinedResult.copy(source = Source.TRANSCRIPT),
                        coercionAssessment = conv.coercionAssessment
                    )
                }
                delay(1100L)
            }
            localState.update { it.copy(isCallDemoRunning = false) }
        }
    }

    // OCR & Gallery QR
    fun inspectScreenshotUri(uri: Uri) {
        viewModelScope.launch {
            localState.update {
                it.copy(isProcessingOcr = true, ocrImageUri = uri, ocrEmptyWarning = false)
            }
            val output = ScreenshotOcrProcessor.inspectImageUri(getApplication(), uri)
            localState.update {
                it.copy(
                    isProcessingOcr = false,
                    ocrExtractedText = output.extractedText,
                    ocrQrPayloads = output.qrPayloads,
                    ocrEmptyWarning = output.ocrEmpty
                )
            }
            if (output.ocrEmpty) {
                showToast("No readable text or QR code found in this image. You can type the text manually.")
            }
        }
    }

    fun decodeQrFromImageUri(uri: Uri, onDecoded: (String) -> Unit) {
        viewModelScope.launch {
            val qrs = ScreenshotOcrProcessor.decodeQrFromGalleryUri(getApplication(), uri)
            if (qrs.isEmpty()) {
                showToast("No QR code detected in this image.")
            } else {
                val first = qrs.first()
                localState.update { it.copy(qrInput = first) }
                onDecoded(first)
            }
        }
    }

    // Share intent arrival
    fun handleIncomingSharedText(text: String) {
        localState.update {
            it.copy(
                messageInput = text,
                pendingNavigationEvent = "SCAN_MESSAGE_AUTO"
            )
        }
    }

    fun handleIncomingSharedImage(uri: Uri) {
        inspectScreenshotUri(uri)
        localState.update { it.copy(pendingNavigationEvent = "SCAN_SCREENSHOT") }
    }

    fun consumePendingNav() = localState.update { it.copy(pendingNavigationEvent = null) }

    // Pre-Action Safety Check & Coercion
    fun evaluatePreAction(input: PreActionInput) {
        viewModelScope.launch {
            val decision = repository.evaluatePreAction(input)
            localState.update { it.copy(preActionDecision = decision) }
        }
    }

    fun updateCoercionAnswers(answers: CoercionAnswers) {
        val currentResult = localState.value.latestScanResult
        val assessment = CoercionPressureEstimator.estimate(
            stagesReached = currentResult?.stagesReached.orEmpty(),
            tactics = currentResult?.manipulation.orEmpty(),
            answers = answers,
            isHindiOutput = uiState.value.settings.appLanguage.startsWith("hi")
        )
        localState.update { it.copy(coercionAnswers = answers, coercionAssessment = assessment) }
    }

    // Actions on Result
    fun submitFeedback(scanId: String, option: FeedbackOption, allowlistEntry: String? = null) {
        viewModelScope.launch {
            repository.saveFeedback(scanId, option, allowlistEntry)
            showToast("Feedback saved locally on your device.")
        }
    }

    fun saveLatestToVault(note: String, incidentTitle: String?, incidentId: String?) {
        val res = localState.value.latestScanResult ?: return
        viewModelScope.launch {
            repository.saveToVault(
                result = res,
                itemType = res.source.name,
                note = note,
                incidentId = incidentId,
                incidentTitle = incidentTitle,
                screenshotUri = if (res.source == Source.SCREENSHOT) localState.value.ocrImageUri else null
            )
            showToast("Encrypted & saved to Evidence Vault.")
        }
    }

    fun quarantineLatest() {
        val res = localState.value.latestScanResult ?: return
        viewModelScope.launch {
            repository.moveToQuarantine(res)
            showToast("Encrypted copy moved to RakshakAI Quarantine (original app message untouched).")
        }
    }

    fun restoreFromQuarantine(entity: QuarantineEntity, onRestored: () -> Unit) {
        val dec = repository.decryptQuarantineItem(entity)
        viewModelScope.launch {
            repository.deleteQuarantineItem(entity.id)
            localState.update { it.copy(messageInput = dec.decryptedText) }
            onRestored()
        }
    }

    fun deleteQuarantineItem(id: String) = viewModelScope.launch {
        repository.deleteQuarantineItem(id)
        showToast("Quarantined copy deleted permanently.")
    }

    fun deleteVaultItem(entity: VaultItemEntity) = viewModelScope.launch {
        repository.deleteVaultItem(entity)
        showToast("Vault item deleted.")
    }

    fun exportVaultZip(onReady: (File) -> Unit) = viewModelScope.launch {
        val file = repository.createVaultExportZip()
        onReady(file)
    }

    fun unlockVaultSession() = localState.update { it.copy(vaultUnlocked = true) }

    fun deleteHistoryItem(scanId: String) = viewModelScope.launch {
        repository.deleteHistoryItem(scanId)
        showToast("History entry removed.")
    }

    fun clearHistory() = viewModelScope.launch {
        repository.clearHistory()
        showToast("Scan history cleared.")
    }

    fun addTrustedContact(name: String, relationship: String, phone: String) = viewModelScope.launch {
        if (name.isBlank() || phone.isBlank()) {
            showToast("Enter both contact name and phone number.")
            return@launch
        }
        repository.addTrustedContact(name, relationship, phone)
        showToast("Trusted contact encrypted and saved locally.")
    }

    fun deleteTrustedContact(id: String) = viewModelScope.launch {
        repository.deleteTrustedContact(id)
        showToast("Trusted contact removed.")
    }

    fun removeAllowlist(key: String) = viewModelScope.launch {
        repository.removeAllowlistEntry(key)
    }

    fun deleteAllData() = viewModelScope.launch {
        repository.deleteEverything()
        localState.value = LocalWorkState()
        refreshAudits()
        showToast("All RakshakAI data, vault files, settings, and keys wiped.")
    }

    fun runOfflineSelfTestSuite() {
        viewModelScope.launch {
            val lang = uiState.value.settings.appLanguage
            val subset = listOf(
                DemoSamples.messageSamples[0],
                DemoSamples.messageSamples[2],
                DemoSamples.messageSamples[7],
                DemoSamples.messageSamples[8],
                DemoSamples.messageSamples[10],
                DemoSamples.messageSamples[13]
            )
            val results = subset.map { sample ->
                val res = repository.engine.analyze(
                    ScanInput(text = sample.payload, source = Source.MANUAL),
                    outputLanguage = lang
                )
                sample.title to res
            }
            localState.update { it.copy(offlineTestResults = results) }
        }
    }

    fun speakResultAloud(result: ScanResult) {
        val isHindi = uiState.value.settings.appLanguage.startsWith("hi")
        val textToSpeak = buildString {
            append(if (isHindi) "जोखिम स्तर ${result.riskLevel.name}, स्कोर ${result.riskScore}। " else "Risk level ${result.riskLevel.name}, score ${result.riskScore} out of 100. ")
            append(if (uiState.value.settings.elderlyMode && result.elderlyExplanation.isNotBlank()) result.elderlyExplanation else result.explanation)
            append(". ")
            if (result.recommendations.isNotEmpty()) {
                append(result.recommendations.take(2).joinToString(". "))
            }
        }
        ttsHelper.speak(textToSpeak, isHindi)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    fun dismissLatestResult() {
        localState.update { it.copy(latestScanResult = null) }
    }

    fun showToast(msg: String) = localState.update { it.copy(toastMessage = msg) }
    fun clearToast() = localState.update { it.copy(toastMessage = null) }

    class Factory(private val app: KavachApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return RakshakViewModel(app, app.repository) as T
        }
    }
}
