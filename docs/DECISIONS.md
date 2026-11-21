# Architectural & Implementation Decisions (`DECISIONS.md`)

1. **Preserved `namespace = "com.example"` & `applicationId = "com.aistudio.kavach.rx7k2m"`**
   - **Decision:** Retained the platform-assigned `applicationId` (`com.aistudio.kavach.rx7k2m`) and root `com.example` directory namespace per AI Studio Android build guidelines (to avoid breaking resource R-class resolution and APK signing continuity), while organizing all packages cleanly under `com.example.{presentation, domain, data, detection, privacy, integrations}`.
   - **Impact:** Zero impact on user-facing branding (`RakshakAI` in `strings.xml` and `settings.gradle.kts`) and clean modular separation matching §2.

2. **Pure-Kotlin Rule Loader (`RuleSource` Abstraction) + `kotlinx.serialization`**
   - **Decision:** Parsed `app/src/main/assets/rules/*.json` with `kotlinx.serialization.json` rather than `org.json.JSONObject` and injected rules through a `RuleSource` interface.
   - **Rationale:** `org.json` is an Android SDK stub that throws or returns defaults on pure JVM unit tests unless mocked. `kotlinx.serialization` + `RuleSource` lets the exact production detection code and real JSON assets run natively in fast JVM JUnit tests (`FileSystemRuleSource` in tests, `AssetRuleSource` on Android).

3. **Explicit `tools:node="remove"` on Network Permissions**
   - **Decision:** Added `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />` (and `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`) in `AndroidManifest.xml`.
   - **Rationale:** Bundled ML Kit artifacts (`com.google.mlkit:barcode-scanning`, `text-recognition`, `text-recognition-devanagari`) transitively include `mlkit:common` which may declare `INTERNET`/`ACCESS_NETWORK_STATE` during manifest merging for optional telemetry. Stripping them guarantees the final merged APK manifest has zero network permissions while offline bundled OCR & barcode scanning continue to run locally.

4. **Built-In Kotlin Support with AGP 9.1.1 + KSP**
   - **Decision:** Used AGP 9.1.1 built-in Kotlin support (`2.2.10`), KSP `2.2.10-2.0.2`, and `android.disallowKotlinSourceSets=false` in `gradle.properties`.
   - **Rationale:** Matches Android Studio container toolchain without adding deprecated `org.jetbrains.kotlin.android` plugins.

5. **Runtime Locale Switching via `Configuration.setLocale` + `CompositionLocalProvider`**
   - **Decision:** Implemented instant English/Hindi UI switching via `LocalizedContextWrapper` bound to DataStore's `appLanguage` preference (`res/values/strings.xml` + `res/values-hi/strings.xml`), plus passing `appLanguage` directly into `RuleBasedScamEngine` for `explanations_en.json` / `explanations_hi.json` and `TtsHelper`.
   - **Rationale:** Instant live toggling without requiring `AppCompatActivity` theme overrides or restarting the Activity.

6. **`FragmentActivity` Base Class for `MainActivity`**
   - **Decision:** Extended `androidx.fragment.app.FragmentActivity` (instead of bare `ComponentActivity`) in `MainActivity`.
   - **Rationale:** `androidx.biometric.BiometricPrompt` requires `FragmentActivity` while supporting Jetpack Compose `setContent` identically.
