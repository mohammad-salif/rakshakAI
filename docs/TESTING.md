# RakshakAI Testing & Privacy Verification

## 1. Automated Unit Test Suite (`gradle :app:testDebugUnitTest`)

All engine components run in pure JVM JUnit 4 tests against the real production rule files (`app/src/main/assets/rules/*.json`):

- **`PreprocessorAndEdgeCaseTest`**: Zero-width character stripping, Devanagari digit conversion (`०-९` → `0-9`), leet deobfuscation without corrupting valid OTP digits, 5,000-character truncation notice, empty/emoji/RTL safety, language detection (English, Hindi, Hinglish, Mixed, Unsupported script warning, manual hint), and `RuleLoadResult.Unavailable` error handling when rules are corrupt/missing.
- **`UrlBrandAndUpiTest`**: URL/UPI/phone/amount extraction, `.co.in`/`.gov.in` two-level public-suffix registered domain calculation, raw IP host, Punycode (`xn--`), URL shorteners (`Uncertain`), suspicious TLDs (`.top`, `.xyz`), `@` credential trick, deep subdomain brand mismatch, hyphen-padded/homoglyph/Levenshtein lookalikes, `.apk` downloads, non-standard ports, official domain ≤ 10 cap, HTTPS-never-proof check, malformed URL recovery, and `upi://pay` refund QR analysis.
- **`ConversationAndPredictiveTest`**: Bracketed WhatsApp and blank-line conversation splitting, Digital Arrest multi-turn escalation (`S2→S3→S4→S6`), next-move forecast (`S7`), cumulative risk escalation, Coercion Pressure Estimator, Cross-Signal Engine (with real installed remote-app signal & simulated screen-share toggles), and Pre-Action Safety Check (10-second pause vs safe payee).
- **`SeedAndFalsePositiveSuiteTest`**: All 15 §10 benchmark samples (English, Hindi, Hinglish, OTP theft, lottery, job, investment, **unseen electricity disconnection paraphrase**, AnyDesk remote-access, parcel `.top`, legit OTP template, legit delivery, legit English & Hindi bill reminders, `"Hi"` greeting), extra misspelling/Hindi/Hinglish cases, performance check (< 2s across 15 scans), and a **15-message legitimate false-positive audit verifying 0 High/Critical false alarms**.

## 2. Privacy & Manifest Audit

Verify that the **merged** Android manifest contains zero network permissions and zero broad package/storage permissions:

```bash
gradle :app:processDebugManifest
grep -E "INTERNET|ACCESS_NETWORK_STATE|QUERY_ALL_PACKAGES|READ_EXTERNAL_STORAGE|READ_MEDIA_IMAGES" \
  app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
```

Expected output: **no matches** (exit code 1 from `grep`).

Check dependencies to ensure no HTTP/network client SDKs (OkHttp, Retrofit, Ktor, Firebase, Coil, Glide) are on the runtime classpath:

```bash
gradle :app:dependencies --configuration debugRuntimeClasspath | grep -iE "okhttp|retrofit|ktor|firebase|coil|glide|analytics|crashlytics"
```

Expected output: **no matches**.

## 3. Manual Airplane-Mode Verification Checklist

1. Enable **Airplane Mode** on Android 10+ device.
2. Open **More → Privacy Centre** → Confirm `"Internet permission: NOT requested ✅"`.
3. Tap **Run Offline Self-Test** → Verify all 6 test scenarios complete in milliseconds offline.
4. Pick a screenshot via **Scan → Screenshot** (English or Hindi) → Verify offline ML Kit OCR extracts text cleanly.
5. Inspect `upi://pay?pa=refund.helpdesk@upi&pn=Refund%20Desk&am=5000&tn=Receive%20refund` via **Scan → QR/UPI** → Verify warning without launching any payment app.
6. Save a scan to **Vault** and export ZIP → Verify Vault record decrypts locally and requires confirmation before exporting.
