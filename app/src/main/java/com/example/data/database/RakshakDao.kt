package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RakshakDao {
    // Scan History (summary-only)
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun observeHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: ScanHistoryEntity)

    @Query("UPDATE scan_history SET feedbackOption = :option WHERE scanId = :scanId")
    suspend fun updateHistoryFeedback(scanId: String, option: String)

    @Query("DELETE FROM scan_history WHERE scanId = :scanId")
    suspend fun deleteHistoryItem(scanId: String)

    @Query("DELETE FROM scan_history")
    suspend fun clearHistory()

    // Vault Items
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun observeVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items ORDER BY createdAt ASC")
    suspend fun getAllVaultItemsOnce(): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: String)

    @Query("DELETE FROM vault_items")
    suspend fun clearVault()

    // Quarantine
    @Query("SELECT * FROM quarantine_items ORDER BY createdAt DESC")
    fun observeQuarantineItems(): Flow<List<QuarantineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuarantine(item: QuarantineEntity)

    @Query("DELETE FROM quarantine_items WHERE id = :id")
    suspend fun deleteQuarantineItem(id: String)

    @Query("DELETE FROM quarantine_items")
    suspend fun clearQuarantine()

    // Trusted Contacts
    @Query("SELECT * FROM trusted_contacts ORDER BY createdAt DESC")
    fun observeTrustedContacts(): Flow<List<TrustedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustedContact(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteTrustedContact(id: String)

    @Query("DELETE FROM trusted_contacts")
    suspend fun clearTrustedContacts()

    // Incidents (Replay Timeline)
    @Query("SELECT * FROM incidents ORDER BY updatedAt DESC")
    fun observeIncidents(): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncident(incident: IncidentEntity)

    @Query("DELETE FROM incidents")
    suspend fun clearIncidents()

    // Feedback
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Query("DELETE FROM feedback_entries")
    suspend fun clearFeedback()

    // Allowlist
    @Query("SELECT * FROM trusted_allowlist ORDER BY createdAt DESC")
    fun observeAllowlist(): Flow<List<TrustedAllowlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllowlist(entry: TrustedAllowlistEntity)

    @Query("DELETE FROM trusted_allowlist WHERE domainOrSender = :key")
    suspend fun deleteAllowlist(key: String)

    @Query("DELETE FROM trusted_allowlist")
    suspend fun clearAllowlist()
}
