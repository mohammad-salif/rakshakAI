# RakshakAI — Your Intelligent Digital Guardian

RakshakAI is a **privacy-first, predictive scam-defence Android app** built with Kotlin and Jetpack Compose (Material 3) for India. Instead of merely asking *"Is this a scam?"*, RakshakAI asks:

> **What does the scammer want the user to do next, what might they try after that, and how do we stop harm before it happens?**

Everything runs **100% on-device**. RakshakAI does **not** declare Android's `INTERNET` or `ACCESS_NETWORK_STATE` permission, making it structurally incapable of transmitting user content off the phone.

---

## Core Capabilities (4 Pillars, 20 Features)

1. **DETECT (Scam Intelligence)**
   - **Message Investigator:** Rule-based offline analyzer supporting **English, Hindi (Devanagari), and Hinglish**, with `AnnotatedString` phrase highlighting, intent extraction (`OTP`, `MONEY`, `PERSONAL_INFO`, `APP_INSTALL`, `LINK_CLICK`, `CALL_BACK`), pressure-tactic breakdown, and Share-Target (`ACTION_SEND`) support for text and images.
   - **Conversation Trap Detector & Timeline:** Auto-splits pasted WhatsApp/SMS chats (`[10:32] Name:`, `Name:`, blank-line blocks) plus a manual turn composer, calculating cumulative escalating risk across turns.
   - **Screenshot & Image Investigator:** Zero-permission Android Photo Picker + bundled **ML Kit Latin & Devanagari OCR** + **ZXing / ML Kit QR decoding**.
   - **Call Transcript Shield:** Paste call transcripts or run the **Scripted Demo Call** *(SIMULATED)* to watch the kill-chain update live.
2. **PREDICT (Kill-Chain & Next-Move Forecaster)**
   - Interactive `S1 Hook → S2 Authority → S3 Fear/Urgency → S4 Isolation → S5 Action Request → S6 Payment → S7 Post-Payment Escalation` stepper.
   - Transparent `stage_transitions.json` forecaster showing possible next moves (*Likely / Possible*) and protective advice.
   - **Coercion & Pressure Estimator:** 0–100 pressure-risk estimate with explicit *"Pressure-risk estimate — not a diagnosis."* disclaimer.
3. **PREVENT (Interventions & Guardrails)**
   - **Link & Brand Impersonation Investigator:** Inspects IP hosts, Punycode (`xn--`), shorteners, risky TLDs, `@` credentials, subdomain mismatches against `brands.json` (18 Indian banks/services/gov portals), Levenshtein/homoglyph lookalikes, `.apk` downloads, and plain `http://`. **Never auto-opens links** and never treats HTTPS as proof of safety.
   - **QR Code & UPI Scam Investigator:** Live CameraX + bundled ML Kit scanner and gallery picker; parses `upi://pay` (`pa, pn, am, tn, cu`) and warns against refund/collect traps without ever launching payment intents.
   - **Pre-Action Safety Check:** Full-screen pause-and-verify flow with context questions and a **10-second forced countdown** before allowing risky actions.
   - **Cross-Signal Intervention Engine & Remote-Access Guard:** Narrow `<queries>` check (no `QUERY_ALL_PACKAGES`) for AnyDesk, TeamViewer, QuickSupport, RustDesk, AirDroid, and Chrome Remote Desktop.
   - **Isolation Breaker:** Large-type calm screen for high-pressure/secrecy situations + consent-based, **metadata-only** SMS/Share alert to trusted contacts.
   - **Scam Emergency Assistant:** Static offline decision tree, evidence checklist, 1930 helpline dialer, and editable complaint draft.
4. **PROTECT (Encrypted Storage, Privacy & Accessibility)**
   - **AES-256-GCM Encrypted Evidence Vault** (Android Keystore, per-record random IV), optional Biometric/device-credential lock, ZIP export, and **Scam Replay Timeline** grouped by Incident.
   - **App-Level Quarantine:** Encrypted local copies (never modifies external apps' SMS/WhatsApp messages).
   - **Summary-Only Scan History:** Toggleable OFF; stores only score, category, explanation, and timestamp (never raw message text).
   - **Privacy Centre:** Live runtime `PackageManager` verification proving `INTERNET` is NOT requested, storage footprint view, offline self-test runner, and **Delete Everything** wipe.
   - **Elderly Mode & Bilingual UI:** Switch between **English** and **हिन्दी**, ≥18sp body typography, ≥56dp touch targets, simplified wording, and Android `TextToSpeech` read-aloud.

---

## Honest Capabilities & Limitations

- **Cannot read WhatsApp directly:** Only inspects text/images explicitly pasted or sent via the Android Share sheet (`ACTION_SEND`).
- **Cannot delete messages in other apps:** Quarantine moves an encrypted copy inside RakshakAI's own storage; original SMS or WhatsApp messages remain untouched.
- **Cannot block or lock payments in banking/UPI apps:** Pre-Action Safety Check is a reminder and pause-and-verify guardrail, not a payment lock.
- **Call Shield is transcript/simulated only:** RakshakAI does not record, intercept, or listen to live phone calls.
- **Family Guardian alerts & Dashboard:** Alerts open your own SMS/Share composer containing only risk level + time (no message text). The second-device Guardian Dashboard is a **SIMULATED / DEMO** preview.
- **Rule-based detection limits:** Deterministic rules can miss novel scam phrasing or raise false alarms on unusual legitimate messages. Risk scores are guidance, not proof of fraud; predictions are possibilities, not certainties.

---

## Building & Testing

### Prerequisites
- Android Studio Ladybug / Meerkat or newer with JDK 17+ and Android SDK 36 (Min SDK 29 / Android 10+).
- Note: Use system `gradle` or Android Studio's Gradle sync (no internet required once dependencies are cached).

### Build Debug APK
```bash
gradle :app:assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

Install onto an Android 10+ device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit Tests & Privacy Audit
```bash
gradle :app:testDebugUnitTest
gradle :app:processDebugManifest
```

---

## 2-Minute Demo Script (Judge / Reviewer Walkthrough)

1. **Privacy Proof (20s):** Turn on **Airplane Mode**. Open **More → Privacy Centre** and verify `Internet permission: NOT requested ✅` (computed live via `PackageManager`). Tap **Run Offline Self-Test**.
2. **Multilingual & Unseen Paraphrase Detection (30s):** Open **Protect → Simulation Lab**. Run Sample #1 (Fake SBI KYC lookalike), Sample #3 (Hindi Devanagari OTP trap), Sample #8 (unseen electricity disconnection paraphrase), and Sample #11 (legitimate HDFC OTP notice staying Low). Open **Result** to show highlighted evidence (`Observed` vs `Inferred`), expandable score breakdown, and **Read Aloud**.
3. **Predictive Kill-Chain & Isolation Breaker (35s):** In Simulation Lab, tap **Digital Arrest Chat** to open the **Conversation Timeline**. Show stages `S2 → S3 → S4 → S6`, cumulative Critical risk, Next-Move forecast (`S7 Post-payment escalation`), Coercion score, and tap **Open Isolation Breaker** to preview the privacy-safe metadata-only trusted contact alert.
4. **UPI QR & Pre-Action 10-Second Pause (20s):** In Simulation Lab, tap **UPI Refund QR** to inspect `upi://pay?...am=5000&tn=Receive%20refund` without launching any payment app. Tap **Pre-Action Check** to trigger the **10-second forced pause countdown** and verification checklist.
5. **Encrypted Vault & Replay Timeline (15s):** Save the result to the **Evidence Vault**, switch to the **Vault** tab to inspect the AES-256-GCM record and **Replay Timeline**, and show **Settings → Elderly Mode / हिन्दी** instant switch.
