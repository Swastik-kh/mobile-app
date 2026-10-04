package com.example.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class OxygenCylinderRecord(
    val id: String = "",
    val cylinderNo: String = "",
    val size: String = "Jumbo",              // "Jumbo" | "Medium" | "Small" | "D-Type" | "B-Type"
    val status: String = "Full (भरिएको)",     // "Full (भरिएको)" | "Empty (खाली)" | "In Use (प्रयोगमा)" | "Maintenance (मर्मतमा)"
    val location: String = "मुख्य स्टोर",
    val lastRefilledDateBs: String = "",
    val pressurePsi: Int = 1500,
    val remarks: String? = null,
    val _orgName: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val syncStatus: String = "SYNCED"
) {
    fun toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "cylinderNo" to cylinderNo,
            "size" to size,
            "status" to status,
            "location" to location,
            "lastRefilledDateBs" to lastRefilledDateBs,
            "pressurePsi" to pressurePsi,
            "remarks" to remarks,
            "_orgName" to _orgName,
            "createdAt" to if (createdAt != 0L) createdAt else System.currentTimeMillis(),
            "updatedAt" to (updatedAt ?: System.currentTimeMillis())
        ).filterValues { it != null }
    }
}
