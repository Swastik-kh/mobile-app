package com.example.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class OrgSettings(
    val coldChainMinTempC: Double = 2.0,
    val coldChainMaxTempC: Double = 8.0,
    val coldChainAlertPhone: String? = null,
    val orgNameNepali: String = "",
    val orgNameEnglish: String = "",
    val subTitleNepali: String = "",
    val subTitleNepali2: String = "",
    val subTitleNepali3: String = "",
    val subTitleNepali4: String = "",
    val address: String = "",
    val logoUrl: String = "",
    val provinceLogoUrl: String = "",
    val activeFiscalYear: String = "",
    val fiscalYear: String = ""
) {
    val effectiveFiscalYear: String
        get() = activeFiscalYear.ifBlank { fiscalYear }

    fun toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "coldChainMinTempC" to coldChainMinTempC,
            "coldChainMaxTempC" to coldChainMaxTempC,
            "coldChainAlertPhone" to coldChainAlertPhone,
            "orgNameNepali" to orgNameNepali,
            "orgNameEnglish" to orgNameEnglish,
            "subTitleNepali" to subTitleNepali,
            "subTitleNepali2" to subTitleNepali2,
            "subTitleNepali3" to subTitleNepali3,
            "subTitleNepali4" to subTitleNepali4,
            "address" to address,
            "logoUrl" to logoUrl,
            "provinceLogoUrl" to provinceLogoUrl,
            "activeFiscalYear" to activeFiscalYear,
            "fiscalYear" to fiscalYear
        ).filterValues { it != null }
    }
}
