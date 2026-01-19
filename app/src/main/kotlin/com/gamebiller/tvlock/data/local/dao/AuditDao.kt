package com.gamebiller.tvlock.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gamebiller.tvlock.data.local.entity.AuditLogEntity

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp ASC")
    suspend fun getAllLogs(): List<AuditLogEntity>

    @Delete
    suspend fun delete(log: AuditLogEntity)
    
    @Query("DELETE FROM audit_logs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
