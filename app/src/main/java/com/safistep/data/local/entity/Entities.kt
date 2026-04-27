package com.safistep.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Platform entity (blacklist cache) ────────────────────────
@Entity(tableName = "platforms")
data class PlatformEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val category: String,
    val keywords: String,        // comma-separated list
    val versionHash: String,
    val updatedAt: String
) {
    fun keywordList(): List<String> = keywords.split(",").map { it.trim().lowercase() }
}

// ── Block history entity ─────────────────────────────────────
@Entity(tableName = "block_history")
data class BlockHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformId: Long?,
    val platformName: String?,
    val matchedKeyword: String?,
    val amountAttempted: Double?,
    val rawStkText: String?,
    val paybillDetected: String?,
    val actionTaken: String,       // "blocked" | "user_overrode"
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false    // synced to server?
)

// ── Sync metadata ────────────────────────────────────────────
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val value: String
) {
    companion object {
        const val KEY_BLACKLIST_VERSION = "blacklist_version"
        const val KEY_LAST_SYNC        = "last_sync_ts"
    }
}
