# RakshakAI Architecture

## Overview

RakshakAI follows **Clean Architecture + MVVM** with a **pure-Kotlin, rule-based detection engine** isolated from Android framework classes so every analyzer, scorer, and forecaster can be unit-tested on the JVM without an emulator.

## Module & Package Layout (`app/src/main/java/com/example/`)

- **`domain/models/ScamModels.kt`**
  - Immutable `@Serializable` models (`ScanInput`, `ScanResult`, `Evidence`, `UrlResult`, `UpiAnalysis`, `ScoreComponents`, `ConversationAnalysisResult`, `PreActionDecision`, `CoercionAssessment`, `CrossSignalReport`).
- **`detection/` (Pure Kotlin, Zero Android Imports)**
  - `rules/RuleLoader.kt`: Loads & validates 10 JSON files via `RuleSource` (`AssetRuleSource` on Android, `FileSystemRuleSource` in JUnit). Returns `RuleLoadResult.Unavailable` if missing/corrupt rather than faking safety.
  - `rules/PhraseMatcher.kt`: Boundary-aware matcher supporting English, Devanagari Hindi, Hinglish, OTP negation guards (to avoid false-flagging "Do not share OTP" bank templates), and optimal-string-alignment distance-1 misspelling tolerance.
  - `engine/TextPreprocessor.kt`: Truncates at 5,000 chars, NFKC-normalizes, strips zero-width/directional characters, converts Devanagari digits (`०-९` → `0-9`), builds a deobfuscated matching copy (`0→o`, `@→a`, `$→s`, collapsed repeats), and tracks exact index maps back to original display offsets for `AnnotatedString` highlighting.
  - `engine/LanguageDetector.kt`: Classifies English, Hindi (Devanagari), Hinglish (via `hinglish_markers`), Mixed, or Unsupported script.
  - `intent/IntentDetector.kt` & `manipulation/ManipulationDetector.kt`: Extract intents and social-engineering tactics with observed/inferred evidence spans.
  - `category/ScamCategoryClassifier.kt`: Matches 12 categories from `categories.json` (`required_any_groups` + boosters + multi-signal combo scoring).
  - `url/UrlExtractor.kt`, `url/BrandImpersonationDetector.kt`, `url/UrlAnalyzer.kt`: Parses URLs safely, computes registered domains with `public_suffixes.json`, compares against `brands.json` (mismatch, hyphen padding, homoglyphs, Levenshtein ≤ 2), flags IPs, Punycode, shorteners, risky TLDs, `@`, `.apk`, and `http://`.
  - `upi/UpiQrAnalyzer.kt`: Parses `upi://pay` payloads (`pa, pn, am, tn, cu`) without invoking payment intents.
  - `conversation/ConversationSplitter.kt`, `KillChainTracker.kt`, `NextMoveForecaster.kt`: Splits WhatsApp/SMS chats, maps `S1..S7` stages, computes cumulative escalating risk, and forecasts next moves from `stage_transitions.json`.
  - `risk/RiskFusionEngine.kt`, `CoercionPressureEstimator.kt`, `CrossSignalEngine.kt`: Computes `R = 0.35·M + 0.20·S + 0.20·U + 0.15·P + 0.10·C`, applies floor overrides and legitimate-message dampeners, assigns `uncertain` & `Confidence`, estimates pressure risk, and fuses cross-signals.
  - `explain/ExplanationEngine.kt` & `RecommendationEngine.kt`: Renders English/Hindi explanations and Elderly-Mode simplified wording strictly from detected evidence.
  - `engine/ScamEngine.kt`: Exposes `ScamDetector` interface + `RuleBasedScamEngine` implementation (keeping an explicit interface boundary for future pluggable detectors).
- **`privacy/`**
  - `encryption/KeystoreCrypto.kt`: Non-exportable Android Keystore `AES-256-GCM` key with per-record random 12-byte IVs.
  - `data_management/PrivacyAuditHelper.kt`: Runtime `PackageManager` permission inspector and local byte-size auditor.
- **`data/`**
  - Room database (`rakshakai.db`) storing **summary-only** `ScanHistoryEntity`, AES-GCM encrypted `VaultItemEntity`, `QuarantineEntity`, `TrustedContactEntity`, `IncidentEntity`, `FeedbackEntity`, and `TrustedAllowlistEntity`.
  - DataStore (`UserPreferencesRepository`) for settings (`elderlyMode`, `appLanguage`, `historyEnabled`, `smsAutoScanEnabled`, `biometricVaultLock`, `flagSecureEnabled`, `reducedMotion`).
- **`integrations/`**
  - `ocr/ScreenshotOcrProcessor.kt`: Bundled ML Kit Latin + Devanagari text recognition + ZXing/ML Kit QR decoding from Photo Picker URIs.
  - `qr/LiveQrScannerView.kt`: CameraX + bundled ML Kit live barcode scanning.
  - `appscan/RemoteAccessAppScanner.kt`: Explicit `<queries>` check for 6 known remote-access apps.
  - `sms/SmsScanReceiver.kt` & `notifications/NotificationsHelper.kt`: Optional (OFF by default) local SMS screening with `VISIBILITY_PRIVATE` lock-screen-safe alerts.
  - `tts/TtsHelper.kt`: Android `TextToSpeech` in English (`en-IN`) and Hindi (`hi-IN`).
  - `BiometricAuthHelper.kt` & `share/ShareIntentsHelper.kt`.
- **`presentation/`**
  - Jetpack Compose Material 3 UI (`Splash`, 4-step `Onboarding`, 5-tab `MainBottomTab`: `Home`, `Scan`, `Protect`, `Vault`, `More`, plus dedicated `Result`, `ConversationTimeline`, `PreActionCheck`, `IsolationBreaker`, `EmergencyAssistant`, `FamilyGuardian`, `SimulationLab`, `PrivacyCentre`, `SafetyCentre`, `Settings`, `AboutLimitations`).
