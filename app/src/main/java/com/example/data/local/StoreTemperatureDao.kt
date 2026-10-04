package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreTemperatureDao {

    @Query("SELECT * FROM store_temperature_logs WHERE organizationName = :orgName ORDER BY createdAt DESC")
    fun getLogsByOrg(orgName: String): Flow<List<StoreTemperatureEntity>>

    @Query("SELECT * FROM store_temperature_logs WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncLogs(): List<StoreTemperatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: StoreTemperatureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StoreTemperatureEntity>)

    @Query("UPDATE store_temperature_logs SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM store_temperature_logs WHERE id = :id")
    suspend fun deleteById(id: String)
}
