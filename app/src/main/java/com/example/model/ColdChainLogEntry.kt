package com.example.model

import com.example.util.NepaliDateUtils
import com.example.model.LogSession
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ColdChainLogEntry(
    val id: String = "",
    val equipmentId: String = "",
    val equipmentName: String = "",
    val dateBs: String = "",                // "YYYY-MM-DD" Bikram Sambat
    val dateAd: String = "",                // "YYYY-MM-DD" Gregorian, derived from dateBs
    val session: String = LogSession.MORNING.value,        // "Morning" | "Evening"
    val tempCelsius: Double = 0.0,
    val recordedBy: String = "",            // currentUser.fullName ?: currentUser.username
    val recordedByUid: String = "",         // currentUser.id
    val isOutOfRange: Boolean = false,
    val remarks: String? = null,
    val correctiveAction: String? = null,
    val _orgName: String? = null,
    // Compatibility fields for Room entities and legacy code
    val temperatureCelsius: Double = 0.0,
    val fiscalYear: String = "",
    val deviceId: String = "",
    val deviceType: String = "ILR",
    val minTempOfDay: Double? = null,
    val maxTempOfDay: Double? = null,
    val isWithinSafeRange: Boolean = true,
    val alarmTriggered: Boolean = false,
    val organizationName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val updatedBy: String? = null,
    val syncStatus: String = "SYNCED"
) {
    val displayTemp: Double get() = if (tempCelsius != 0.0) tempCelsius else temperatureCelsius
    val displayName: String get() = equipmentName.ifBlank { deviceId.ifBlank { "कोल्ड चेन उपकरण" } }
    val displaySafe: Boolean get() = if (isOutOfRange) false else isWithinSafeRange

    fun toFirebaseMap(): Map<String, Any?> {
        val actualTemp = if (tempCelsius != 0.0) tempCelsius else temperatureCelsius
        val actualOrg = _orgName ?: organizationName
        val actualEquipmentId = equipmentId.ifBlank { deviceId }
        return mapOf(
            "id" to id,
            "equipmentId" to actualEquipmentId,
            "deviceId" to actualEquipmentId,
            "equipmentName" to equipmentName,
            "dateBs" to dateBs,
            "dateAd" to dateAd,
            "session" to session,
            "tempCelsius" to actualTemp,
            "temperatureCelsius" to actualTemp,
            "recordedBy" to recordedBy,
            "recordedByUid" to recordedByUid,
            "isOutOfRange" to isOutOfRange,
            "isWithinSafeRange" to !isOutOfRange,
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
