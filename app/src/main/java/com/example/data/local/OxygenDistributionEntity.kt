package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.OxygenDistributionRecord

@Entity(tableName = "oxygen_distribution_records")
data class OxygenDistributionEntity(
    @PrimaryKey val id: String,
    val cylinderId: String,
    val cylinderNo: String,
    val patientName: String,
    val patientPhone: String?,
    val wardOrDept: String,
    val issuedDateBs: String,
    val returnDateBs: String?,
    val status: String,
    val issuedBy: String?,
    val invoiceNo: String,
    val serviceFee: Int,
    val returnCondition: String?,
    val remarks: String?,
    val orgName: String,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncStatus: String = "SYNCED" // "SYNCED" or "PENDING"
) {
    fun toModel(): OxygenDistributionRecord {
        return OxygenDistributionRecord(
            id = id,
            cylinderId = cylinderId,
            cylinderNo = cylinderNo,
            patientName = patientName,
            patientPhone = patientPhone,
            wardOrDept = wardOrDept,
            issuedDateBs = issuedDateBs,
            returnDateBs = returnDateBs,
            status = status,
            issuedBy = issuedBy,
            invoiceNo = invoiceNo,
            serviceFee = serviceFee,
            returnCondition = returnCondition,
            remarks = remarks,
            _orgName = orgName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncStatus = syncStatus
        )
    }

    companion object {
        fun fromModel(model: OxygenDistributionRecord, orgName: String, syncStatus: String = "SYNCED"): OxygenDistributionEntity {
            return OxygenDistributionEntity(
                id = model.id,
                cylinderId = model.cylinderId,
                cylinderNo = model.cylinderNo,
                patientName = model.patientName,
                patientPhone = model.patientPhone,
                wardOrDept = model.wardOrDept,
                issuedDateBs = model.issuedDateBs,
                returnDateBs = model.returnDateBs,
                status = model.status,
                issuedBy = model.issuedBy,
                invoiceNo = model.invoiceNo,
                serviceFee = model.serviceFee,
                returnCondition = model.returnCondition,
                remarks = model.remarks,
                orgName = orgName,
                createdAt = if (model.createdAt != 0L) model.createdAt else System.currentTimeMillis(),
                updatedAt = model.updatedAt ?: System.currentTimeMillis(),
                syncStatus = syncStatus
            )
        }
    }
}
