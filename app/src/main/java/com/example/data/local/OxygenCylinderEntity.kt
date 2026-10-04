package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.OxygenCylinderRecord

@Entity(tableName = "oxygen_cylinders")
data class OxygenCylinderEntity(
    @PrimaryKey val id: String,
    val cylinderNo: String,
    val size: String,
    val status: String,
    val location: String,
    val lastRefilledDateBs: String,
    val pressurePsi: Int,
    val remarks: String?,
    val orgName: String,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncStatus: String = "SYNCED" // "SYNCED" or "PENDING"
) {
    fun toModel(): OxygenCylinderRecord {
        return OxygenCylinderRecord(
            id = id,
            cylinderNo = cylinderNo,
            size = size,
            status = status,
            location = location,
            lastRefilledDateBs = lastRefilledDateBs,
            pressurePsi = pressurePsi,
            remarks = remarks,
            _orgName = orgName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncStatus = syncStatus
        )
    }

    companion object {
        fun fromModel(model: OxygenCylinderRecord, orgName: String, syncStatus: String = "SYNCED"): OxygenCylinderEntity {
            return OxygenCylinderEntity(
                id = model.id,
                cylinderNo = model.cylinderNo,
                size = model.size,
                status = model.status,
                location = model.location,
                lastRefilledDateBs = model.lastRefilledDateBs,
                pressurePsi = model.pressurePsi,
                remarks = model.remarks,
                orgName = orgName,
                createdAt = if (model.createdAt != 0L) model.createdAt else System.currentTimeMillis(),
                updatedAt = model.updatedAt ?: System.currentTimeMillis(),
                syncStatus = syncStatus
            )
        }
    }
}
