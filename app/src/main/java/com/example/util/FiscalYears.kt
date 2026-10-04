package com.example.util

data class FiscalYearOption(val label: String, val value: String)

val FISCAL_YEARS = listOf(
    FiscalYearOption("२०८०/०८१", "2080/081"),
    FiscalYearOption("२०८१/०८२", "2081/082"),
    FiscalYearOption("२०८२/०८३", "2082/083"),
    FiscalYearOption("२०८३/०८४", "2083/084"),
    FiscalYearOption("२०८४/०८५", "2084/085"),
    FiscalYearOption("२०८५/०८६", "2085/086"),
    FiscalYearOption("२०८६/०८७", "2086/087"),
    FiscalYearOption("२०८७/०८८", "2087/088"),
    FiscalYearOption("२०८८/०८९", "2088/089"),
    FiscalYearOption("२०८९/०९०", "2089/090"),
    FiscalYearOption("२०९०/०९१", "2090/091")
)

const val DEFAULT_FISCAL_YEAR = "2083/084"

fun getFiscalYearLabel(value: String): String {
    return FISCAL_YEARS.find { it.value == value }?.label ?: value
}

fun getFiscalYearValue(label: String): String {
    return FISCAL_YEARS.find { it.label == label }?.value ?: label
}
