package com.example.model

import com.example.util.NepaliDateUtils
import com.example.model.LogSession
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class StoreTemperatureLogEntry(
    val id: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val dateBs: String = "",
    val dateAd: String = "",
    val session: String = LogSession.MORNING.value,        // "Morning" | "Evening"
    val tempCelsius: Double = 0.0,
    val humidityPercent: Double? = null,
    val isOutOfRange: Boolean = false,
    val recordedBy: String = "",
    val recordedByUid: String = "",
    val remarks: String? = null,
    val correctiveAction: String? = null,
    val _orgName: String? = null,
    // Compatibility fields for Room entities and legacy code
    val temperatureCelsius: Double = 0.0,
    val storeName: String = "",
    val roomOrShelf: String = "",
    val fiscalYear: String = "",
    val isWithinSafeRange: Boolean = true,
    val alarmTriggered: Boolean = false,
    val organizationName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val updatedBy: String? = null,
    val syncStatus: String = "SYNCED"
) {
    val displayTemp: Double get() = if (tempCelsius != 0.0) tempCelsius else temperatureCelsius
    val displayName: String get() = roomName.ifBlank { storeName.ifBlank { "औषधि स्टोर" } }
    val displaySafe: Boolean get() = if (isOutOfRange) false else isWithinSafeRange

    fun toFirebaseMap(): Map<String, Any?> {
        val actualTemp = if (tempCelsius != 0.0) tempCelsius else temperatureCelsius
        val actualOrg = _orgName ?: organizationName
        val actualRoom = roomName.ifBlank { storeName }
        return mapOf(
            "id" to id,
            "roomId" to roomId.ifBlank { storeName },
            "roomName" to actualRoom,
            "storeName" to actualRoom,
            "roomOrShelf" to roomOrShelf.ifBlank { actualRoom },
            "dateBs" to dateBs,
            "dateAd" to dateAd,
            "session" to session,
            "tempCelsius" to actualTemp,
            "temperatureCelsius" to actualTemp,
            "humidityPercent" to humidityPercent,
            "isOutOfRange" to isOutOfRange,
            "isWithinSafeRange" to !isOutOfRange,
            "recordedBy" to recordedBy,
            "recordedByUid" to recordedByUid,
            "remarks" to remarks,
            "correctiveAction" to correctiveAction,
            "_orgName" to actualOrg,
            "organizationName" to actualOrg,
            "createdAt" to if (createdAt != 0L) createdAt else System.currentTimeMillis(),
            "updatedAt" to (updatedAt ?: System.currentTimeMillis()),
            "updatedBy" to updatedBy
        ).filterValues { it != null }
    }
}
