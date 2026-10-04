package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.StoreTemperatureLogEntry

@Entity(tableName = "store_temperature_logs")
data class StoreTemperatureEntity(
    @PrimaryKey
    val id: String,
    val fiscalYear: String,
    val dateBs: String,
    val storeName: String,
    val roomOrShelf: String,
    val temperatureCelsius: Double,
    val humidityPercent: Double?,
    val isWithinSafeRange: Boolean,
    val alarmTriggered: Boolean,
    val correctiveAction: String,
    val recordedBy: String,
    val organizationName: String,
    val remarks: String,
    val createdAt: Long,
    val updatedAt: Long?,
    val updatedBy: String?,
    val syncStatus: String = "SYNCED"
) {
    fun toModel(): StoreTemperatureLogEntry = StoreTemperatureLogEntry(
        id = id,
        fiscalYear = fiscalYear,
        dateBs = dateBs,
        storeName = storeName,
        roomOrShelf = roomOrShelf,
        temperatureCelsius = temperatureCelsius,
        humidityPercent = humidityPercent,
        isWithinSafeRange = isWithinSafeRange,
        alarmTriggered = alarmTriggered,
        correctiveAction = correctiveAction,
        recordedBy = recordedBy,
        organizationName = organizationName,
        remarks = remarks,
        createdAt = createdAt,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        syncStatus = syncStatus
    )

    companion object {
        fun fromModel(model: StoreTemperatureLogEntry): StoreTemperatureEntity = StoreTemperatureEntity(
            id = model.id,
            fiscalYear = model.fiscalYear,
            dateBs = model.dateBs,
            storeName = model.storeName,
            roomOrShelf = model.roomOrShelf,
            temperatureCelsius = model.temperatureCelsius,
            humidityPercent = model.humidityPercent,
            isWithinSafeRange = model.isWithinSafeRange,
            alarmTriggered = model.alarmTriggered,
            correctiveAction = model.correctiveAction ?: "",
            recordedBy = model.recordedBy,
            organizationName = model.organizationName,
            remarks = model.remarks ?: "",
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            updatedBy = model.updatedBy,
            syncStatus = model.syncStatus
        )
    }
}
