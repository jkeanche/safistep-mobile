package com.safistep.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safistep.data.local.dao.BlockHistoryDao
import com.safistep.data.local.dao.PlatformDao
import com.safistep.data.local.dao.SyncMetaDao
import com.safistep.data.local.entity.BlockHistoryEntity
import com.safistep.data.local.entity.PlatformEntity
import com.safistep.data.local.entity.SyncMetaEntity

@Database(
    entities = [
        PlatformEntity::class,
        BlockHistoryEntity::class,
        SyncMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SafiStepDatabase : RoomDatabase() {
    abstract fun platformDao(): PlatformDao
    abstract fun blockHistoryDao(): BlockHistoryDao
    abstract fun syncMetaDao(): SyncMetaDao
}
