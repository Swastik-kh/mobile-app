package com.example.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class StoreRoom(
    val id: String = "",
    val name: String = "",
    val roomCode: String? = null,
    val location: String? = null,
    val minTempC: Double = 15.0,
    val maxTempC: Double = 25.0,
    val maxHumidityPercent: Double? = 65.0,
    val isActive: Boolean = true,
    val remarks: String? = null,
    val _orgName: String? = null
) {
    fun toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "roomCode" to roomCode,
            "location" to location,
            "minTempC" to minTempC,
            "maxTempC" to maxTempC,
            "maxHumidityPercent" to maxHumidityPercent,
            "isActive" to isActive,
            "remarks" to remarks,
            "_orgName" to _orgName
        ).filterValues { it != null }
    }
}
