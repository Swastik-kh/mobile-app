package com.example.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ColdChainEquipment(
    val id: String = "",
    val name: String = "",                 // e.g. "ILR Fridge 1", "Deep Freezer 2"
    val type: String = "Other",             // one of: "ILR" | "Deep Freezer" | "Cold Box" | "Other"
    val serialNumber: String? = null,
    val model: String? = null,
    val location: String? = null,
    val isActive: Boolean = true,
    val remarks: String? = null,
    val _orgName: String? = null
) {
    val deviceName: String get() = name
    val deviceType: String get() = type
    val safeMinTemp: Double get() = 2.0
    val safeMaxTemp: Double get() = 8.0
    val organizationName: String get() = _orgName ?: ""
    val displayTypeNepali: String get() = type

    fun toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "type" to type,
            "serialNumber" to serialNumber,
            "model" to model,
            "location" to location,
            "isActive" to isActive,
            "remarks" to remarks,
            "_orgName" to _orgName
        ).filterValues { it != null }
    }
}
