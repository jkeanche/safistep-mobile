package com.safistep.data.local.dao

import androidx.room.*
import com.safistep.data.local.entity.BlockHistoryEntity
import com.safistep.data.local.entity.PlatformEntity
import com.safistep.data.local.entity.SyncMetaEntity
import kotlinx.coroutines.flow.Flow

// ── Platform DAO ─────────────────────────────────────────────
@Dao
interface PlatformDao {

    @Query("SELECT * FROM platforms WHERE id = :id")
    suspend fun getById(id: Long): PlatformEntity?

    @Query("SELECT * FROM platforms ORDER BY name ASC")
    fun getAllFlow(): Flow<List<PlatformEntity>>

    @Query("SELECT * FROM platforms")
    suspend fun getAll(): List<PlatformEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(platforms: List<PlatformEntity>)

    @Query("DELETE FROM platforms")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(platforms: List<PlatformEntity>) {
        deleteAll()
        upsertAll(platforms)
    }

    /**
     * Core matching query — checks if any platform's keywords contain the search text.
     * The actual keyword matching happens in-memory after this pull for flexibility.
     */
    @Query("SELECT * FROM platforms")
    suspend fun getAllForMatching(): List<PlatformEntity>
}

// ── Block History DAO ────────────────────────────────────────
@Dao
interface BlockHistoryDao {

    @Insert
    suspend fun insert(event: BlockHistoryEntity): Long

    @Query("SELECT * FROM block_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 50): Flow<List<BlockHistoryEntity>>

    @Query("SELECT * FROM block_history WHERE synced = 0")
    suspend fun getUnsynced(): List<BlockHistoryEntity>

    @Query("UPDATE block_history SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM block_history WHERE actionTaken = 'blocked'")
    fun getBlockedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_history WHERE actionTaken = 'blocked' AND timestamp >= :since")
    suspend fun getBlockedSince(since: Long): Int

    @Query("SELECT SUM(amountAttempted) FROM block_history WHERE actionTaken = 'blocked'")
    fun getTotalSavedFlow(): Flow<Double?>
}

// ── Sync Meta DAO ────────────────────────────────────────────
@Dao
interface SyncMetaDao {

    @Query("SELECT value FROM sync_meta WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(meta: SyncMetaEntity)

    suspend fun set(key: String, value: String) = setValue(SyncMetaEntity(key, value))
    suspend fun get(key: String): String? = getValue(key)
}
