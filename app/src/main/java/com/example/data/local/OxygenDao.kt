package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OxygenDao {

    // --- Cylinders ---
    @Query("SELECT * FROM oxygen_cylinders WHERE orgName = :orgName ORDER BY cylinderNo ASC")
    fun getCylinders(orgName: String): Flow<List<OxygenCylinderEntity>>

    @Query("SELECT * FROM oxygen_cylinders WHERE id = :id LIMIT 1")
    suspend fun getCylinderById(id: String): OxygenCylinderEntity?

    @Query("SELECT * FROM oxygen_cylinders WHERE cylinderNo = :cylinderNo AND orgName = :orgName LIMIT 1")
    suspend fun getCylinderByNo(cylinderNo: String, orgName: String): OxygenCylinderEntity?

    @Query("SELECT * FROM oxygen_cylinders WHERE syncStatus = 'PENDING'")
    suspend fun getPendingCylinders(): List<OxygenCylinderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCylinder(entity: OxygenCylinderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCylinders(entities: List<OxygenCylinderEntity>)

    @Query("UPDATE oxygen_cylinders SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markCylinderSynced(id: String)

    @Query("DELETE FROM oxygen_cylinders WHERE id = :id")
    suspend fun deleteCylinderById(id: String)

    // --- Distribution Records ---
    @Query("SELECT * FROM oxygen_distribution_records WHERE orgName = :orgName ORDER BY createdAt DESC")
    fun getDistributions(orgName: String): Flow<List<OxygenDistributionEntity>>

    @Query("SELECT * FROM oxygen_distribution_records WHERE id = :id LIMIT 1")
    suspend fun getDistributionById(id: String): OxygenDistributionEntity?

    @Query("SELECT * FROM oxygen_distribution_records WHERE syncStatus = 'PENDING'")
    suspend fun getPendingDistributions(): List<OxygenDistributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDistribution(entity: OxygenDistributionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDistributions(entities: List<OxygenDistributionEntity>)

    @Query("UPDATE oxygen_distribution_records SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markDistributionSynced(id: String)

    @Query("DELETE FROM oxygen_distribution_records WHERE id = :id")
    suspend fun deleteDistributionById(id: String)
}
