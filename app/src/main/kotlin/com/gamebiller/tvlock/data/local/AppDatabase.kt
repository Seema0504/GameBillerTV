package com.gamebiller.tvlock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gamebiller.tvlock.data.local.dao.AuditDao
import com.gamebiller.tvlock.data.local.entity.AuditLogEntity

@Database(entities = [AuditLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
}
