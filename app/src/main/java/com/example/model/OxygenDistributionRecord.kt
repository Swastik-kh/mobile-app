package com.example.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class OxygenDistributionRecord(
    val id: String = "",
    val cylinderId: String = "",
    val cylinderNo: String = "",
    val patientName: String = "",
    val patientPhone: String? = null,
    val wardOrDept: String = "",
    val issuedDateBs: String = "",
    val returnDateBs: String? = null,
    val status: String = "Issued (वितरण गरिएको)",  // "Issued (वितरण गरिएको)" | "Returned (फिर्ता आएको)"
    val issuedBy: String? = null,
    val invoiceNo: String = "",              // format: "OXY-INV-${last 6 digits of timestamp}"
    val serviceFee: Int = 1000,
    val returnCondition: String? = null,
    val remarks: String? = null,
    val _orgName: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val syncStatus: String = "SYNCED"
) {
    fun toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "cylinderId" to cylinderId,
            "cylinderNo" to cylinderNo,
            "patientName" to patientName,
            "patientPhone" to patientPhone,
            "wardOrDept" to wardOrDept,
            "issuedDateBs" to issuedDateBs,
            "returnDateBs" to returnDateBs,
            "status" to status,
            "issuedBy" to issuedBy,
            "invoiceNo" to invoiceNo,
            "serviceFee" to serviceFee,
            "returnCondition" to returnCondition,
            "remarks" to remarks,
            "_orgName" to _orgName,
            "createdAt" to if (createdAt != 0L) createdAt else System.currentTimeMillis(),
            "updatedAt" to (updatedAt ?: System.currentTimeMillis())
        ).filterValues { it != null }
    }
}
