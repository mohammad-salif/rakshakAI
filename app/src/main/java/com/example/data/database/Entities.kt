package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Summary-only scan history: never stores raw message/SMS/WhatsApp text.
 */
@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey val scanId: String,
    val timestamp: Long,
    val source: String,
    val riskScore: Int,
    val riskLevel: String,
    val uncertain: Boolean,
    val categoryName: String,
    val shortExplanation: String,
    val feedbackOption: String? = null
)

/**
 * Consent-based Encrypted Evidence Vault item (AES-256-GCM).
 */
@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val itemType: String, // MESSAGE, CONVERSATION, URL, QR, SCREENSHOT
    val encryptedBlob: String,
    val ivBase64: String,
    val mediaFileName: String? = null,
    val mediaIvBase64: String? = null,
    val createdAt: Long,
    val riskScore: Int,
    val riskLevel: String,
    val categoryName: String,
    val stagesCsv: String = "",
    val encryptedNote: String = "",
    val noteIvBase64: String = "",
    val incidentId: String? = null
)

/**
 * App-level encrypted quarantine copy (original SMS/WhatsApp is never touched).
 */
@Entity(tableName = "quarantine_items")
data class QuarantineEntity(
    @PrimaryKey val id: String,
    val encryptedBlob: String,
    val ivBase64: String,
    val createdAt: Long,
    val riskScore: Int,
    val riskLevel: String,
    val categoryName: String,
    val previewSummary: String
)

/**
 * Trusted family contacts with encrypted phone number.
 */
@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val relationship: String,
    val encryptedPhone: String,
    val ivBase64: String,
    val createdAt: Long
)

/**
 * Incident grouping for Scam Replay Timeline.
 */
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val highestRiskScore: Int
)

/**
 * Local-only user feedback (never uploads or silently mutates weights).
 */
@Entity(tableName = "feedback_entries")
data class FeedbackEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val option: String,
    val createdAt: Long
)

/**
 * User-controlled local trusted domain allowlist.
 */
@Entity(tableName = "trusted_allowlist")
data class TrustedAllowlistEntity(
    @PrimaryKey val domainOrSender: String,
    val note: String,
    val createdAt: Long
)
