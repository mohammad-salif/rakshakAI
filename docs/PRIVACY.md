# RakshakAI Privacy & Security Architecture

## 1. Zero Network Capability (Structural Guarantee)

- `app/src/main/AndroidManifest.xml` does **not** declare `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, or `android.permission.ACCESS_WIFI_STATE`.
- `tools:node="remove"` is explicitly applied to `INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_WIFI_STATE` so transitive AAR manifest merging cannot re-introduce network permissions.
- All OCR (`com.google.mlkit:text-recognition`, `text-recognition-devanagari`) and QR (`com.google.mlkit:barcode-scanning`, `com.google.zxing:core`) dependencies use **bundled** on-device artifacts. No cloud AI, Firebase, ads, crash reporting, or analytics SDKs are included.
- `android:allowBackup="false"` prevents cloud backup of app databases or encrypted files.
- **Privacy Centre** (`PrivacyAuditHelper`) queries `PackageManager` at runtime to verify and display that `INTERNET` is not declared.

## 2. Separation of Local Processing vs. Local Storage vs. External Transmission

| Boundary | Behavior in RakshakAI |
|---|---|
| **Local Processing** | Message, URL, QR/UPI, conversation, call transcript, and screenshot OCR analysis execute strictly in memory on the device CPU. |
| **Local Storage (Minimum by Default)** | **Scan History** (toggleable OFF in Settings) stores *only* a non-sensitive summary (`timestamp`, `source`, `riskScore`, `riskLevel`, `categoryName`, `shortExplanation`). **Raw message text is NOT stored in history.** |
| **Consent-Only Encrypted Storage** | Saving to **Evidence Vault**, **Quarantine**, or **Trusted Contacts** requires explicit user action and encrypts content using **AES-256-GCM** with a non-exportable key in **Android Keystore** (`rakshakai_vault_aes256_key`) and a fresh random 12-byte IV per record. Vault screenshots are encrypted as `.enc` files inside app-private `filesDir/vault_media/`. |
| **External Transmission** | Structurally impossible in the background. Any alert or export (Trusted Contact SMS alert, Emergency complaint draft, Vault ZIP export) uses explicit user confirmation and launches the user's own SMS/Share UI (`ACTION_SENDTO` / `ACTION_SEND` via `FileProvider`) where the user taps Send themselves. Trusted Contact alerts include **only risk level + timestamp + "please call me"** — never message text. |

## 3. Permissions Model (Least Privilege, Just-in-Time)

- **Zero storage permissions:** Screenshot and QR image picking uses Android's system Photo Picker (`ActivityResultContracts.PickVisualMedia`).
- **Camera (`CAMERA`):** Requested only when the user opens Live Camera QR scanning.
- **Notifications (`POST_NOTIFICATIONS`):** Optional; used for SMS auto-scan alerts with `Notification.VISIBILITY_PRIVATE` so lock-screen previews never leak message contents.
- **SMS (`RECEIVE_SMS`):** **OFF by default.** Requested only when the user manually enables SMS Auto-Scan after reading the plain-language rationale.
- **Remote-Access Check:** Uses a narrow `<queries>` block for 6 specific remote-access package names — **never** `QUERY_ALL_PACKAGES`.

## 4. How to Delete Data

1. **Single items:** Delete individual history entries, Vault items (and their encrypted `.enc` screenshot file), Quarantine copies, or Trusted Contacts from their screens after confirmation.
2. **Everything at once:** Open **More → Privacy Centre → Delete Everything** (or Settings) to wipe all Room tables, delete encrypted media & cached export files, clear DataStore preferences, and delete the `AndroidKeyStore` AES key alias.
