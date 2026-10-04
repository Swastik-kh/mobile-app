package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ColdChainLogEntry

@Entity(tableName = "cold_chain_logs")
data class ColdChainEntity(
    @PrimaryKey
    val id: String,
    val fiscalYear: String,
    val dateBs: String,
    val deviceId: String,
    val deviceType: String,
    val temperatureCelsius: Double,
    val minTempOfDay: Double?,
    val maxTempOfDay: Double?,
    val isWithinSafeRange: Boolean,
    val alarmTriggered: Boolean,
    val correctiveAction: String,
    val recordedBy: String,
    val organizationName: String,
    val remarks: String,
    val photoUrl: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val updatedBy: String?,
    val syncStatus: String = "SYNCED" // "PENDING" or "SYNCED"
) {
    fun toModel(): ColdChainLogEntry = ColdChainLogEntry(
        id = id,
        fiscalYear = fiscalYear,
        dateBs = dateBs,
        deviceId = deviceId,
        deviceType = deviceType,
        temperatureCelsius = temperatureCelsius,
        minTempOfDay = minTempOfDay,
        maxTempOfDay = maxTempOfDay,
        isWithinSafeRange = isWithinSafeRange,
        alarmTriggered = alarmTriggered,
        correctiveAction = correctiveAction,
        recordedBy = recordedBy,
        organizationName = organizationName,
        remarks = remarks,
        photoUrl = photoUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        syncStatus = syncStatus
    )

    companion object {
        fun fromModel(model: ColdChainLogEntry): ColdChainEntity = ColdChainEntity(
            id = model.id,
            fiscalYear = model.fiscalYear,
            dateBs = model.dateBs,
            deviceId = model.deviceId,
            deviceType = model.deviceType,
            temperatureCelsius = model.temperatureCelsius,
            minTempOfDay = model.minTempOfDay,
            maxTempOfDay = model.maxTempOfDay,
            isWithinSafeRange = model.isWithinSafeRange,
            alarmTriggered = model.alarmTriggered,
            correctiveAction = model.correctiveAction ?: "",
            recordedBy = model.recordedBy,
            organizationName = model.organizationName,
            remarks = model.remarks ?: "",
            photoUrl = model.photoUrl,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            updatedBy = model.updatedBy,
            syncStatus = model.syncStatus
        )
    }
}
