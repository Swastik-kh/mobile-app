package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ColdChainDao {

    @Query("SELECT * FROM cold_chain_logs WHERE organizationName = :orgName ORDER BY createdAt DESC")
    fun getLogsByOrg(orgName: String): Flow<List<ColdChainEntity>>

    @Query("SELECT * FROM cold_chain_logs WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncLogs(): List<ColdChainEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ColdChainEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ColdChainEntity>)

    @Query("UPDATE cold_chain_logs SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM cold_chain_logs WHERE id = :id")
    suspend fun deleteById(id: String)
}
