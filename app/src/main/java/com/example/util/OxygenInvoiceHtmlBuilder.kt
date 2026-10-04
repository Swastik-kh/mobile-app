package com.example.util

import com.example.model.OrgSettings
import com.example.model.OxygenDistributionRecord

object OxygenInvoiceHtmlBuilder {

    private fun escapeHtml(text: String?): String {
        if (text == null) return ""
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    fun buildInvoiceHtml(
        dist: OxygenDistributionRecord,
        settings: OrgSettings,
        activeOrgName: String,
        currentFiscalYear: String
    ): String {
        val orgTitle = when {
            settings.orgNameNepali.isNotBlank() -> settings.orgNameNepali
            settings.orgNameEnglish.isNotBlank() -> settings.orgNameEnglish
            activeOrgName.isNotBlank() -> activeOrgName
            else -> "स्वास्थ्य संस्था"
        }

        val subtitles = mutableListOf<String>()
        if (settings.subTitleNepali.isNotBlank()) subtitles.add(settings.subTitleNepali)
        if (settings.subTitleNepali2.isNotBlank()) subtitles.add(settings.subTitleNepali2)
        if (settings.subTitleNepali3.isNotBlank()) subtitles.add(settings.subTitleNepali3)
        if (settings.subTitleNepali4.isNotBlank()) subtitles.add(settings.subTitleNepali4)

        if (subtitles.isEmpty() && settings.address.isNotBlank()) {
            subtitles.add(settings.address)
        }

        val patientPhoneDisplay = if (!dist.patientPhone.isNullOrBlank()) {
            escapeHtml(dist.patientPhone)
        } else {
            "उपलब्ध छैन"
        }

        val returnDateDisplay = if (!dist.returnDateBs.isNullOrBlank()) {
            escapeHtml(dist.returnDateBs)
        } else {
            "हाल फिर्ता भएको छैन"
        }

        val issuedByDisplay = if (!dist.issuedBy.isNullOrBlank()) {
            escapeHtml(dist.issuedBy)
        } else {
            "Admin"
        }

        val logoHtml = if (settings.logoUrl.isNotBlank()) {
            """<img src="${escapeHtml(settings.logoUrl)}" alt="Logo" style="max-width: 75px; max-height: 75px; object-fit: contain;" />"""
        } else {
            """<div style="width: 75px; height: 75px;"></div>"""
        }

        val provinceLogoHtml = if (settings.provinceLogoUrl.isNotBlank()) {
            """<img src="${escapeHtml(settings.provinceLogoUrl)}" alt="Province Logo" style="max-width: 56px; max-height: 56px; object-fit: contain;" />"""
        } else {
            """<div style="width: 56px; height: 56px;"></div>"""
        }

        val subtitlesHtml = subtitles.joinToString("\n") {
            """<div style="font-size: 12.5px; color: #334155; margin-top: 2px;">${escapeHtml(it)}</div>"""
        }

        val remarksHtml = if (!dist.remarks.isNullOrBlank()) {
            """
            <div style="background-color: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; padding: 10px 14px; margin-bottom: 20px; font-size: 12px; color: #92400e;">
                <strong>विशेष टिप्पणी:</strong> ${escapeHtml(dist.remarks)}
            </div>
            """.trimIndent()
        } else {
            ""
        }

        return """
        <!DOCTYPE html>
        <html lang="ne">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Oxygen Invoice - ${escapeHtml(dist.invoiceNo)}</title>
            <style>
                @page {
                    size: A4;
                    margin: 12mm;
                }
                * {
                    box-sizing: border-box;
                    -webkit-print-color-adjust: exact;
                    print-color-adjust: exact;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif, "Noto Sans Devanagari";
                    margin: 0;
                    padding: 16px;
                    color: #1e293b;
                    background-color: #ffffff;
                    font-size: 13px;
                    line-height: 1.4;
                }
                .invoice-container {
                    max-width: 800px;
                    margin: 0 auto;
                }
                .header-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    border-bottom: 2px solid #1e293b;
                    padding-bottom: 12px;
                    margin-bottom: 8px;
                }
                .header-center {
                    text-align: center;
                    flex: 1;
                    padding: 0 12px;
                }
                .header-center h2 {
                    margin: 0;
                    font-size: 20px;
                    font-weight: bold;
                    color: #0f172a;
                }
                .invoice-tag {
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    color: #155e75;
                    font-size: 12.5px;
                    font-weight: bold;
                    margin: 6px 0 0 0;
                }
                .meta-row {
                    display: flex;
                    justify-content: space-between;
                    font-family: monospace, -apple-system, sans-serif;
                    font-size: 12px;
                    color: #475569;
                    border-bottom: 1px solid #e2e8f0;
                    padding-bottom: 6px;
                    margin-bottom: 14px;
                }
                .patient-box {
                    display: flex;
                    justify-content: space-between;
                    background-color: #f8fafc;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                    padding: 12px 16px;
                    margin-bottom: 16px;
                }
                .patient-col-left {
                    flex: 1.2;
                }
                .patient-col-right {
                    flex: 1;
                    text-align: right;
                }
                .section-label {
                    font-size: 11px;
                    font-weight: bold;
                    color: #64748b;
                    margin-bottom: 4px;
                    text-transform: uppercase;
                }
                .patient-name {
                    font-size: 15px;
                    font-weight: bold;
                    color: #0f172a;
                    margin-bottom: 3px;
                }
                .info-line {
                    font-size: 12px;
                    color: #334155;
                    margin-bottom: 2px;
                }
                table.items-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 16px;
                    font-size: 12.5px;
                }
                table.items-table th {
                    background-color: #f1f5f9;
                    color: #1e293b;
                    font-weight: bold;
                    border: 1px solid #cbd5e1;
                    padding: 8px 10px;
                    text-align: left;
                }
                table.items-table td {
                    border: 1px solid #cbd5e1;
                    padding: 8px 10px;
                    color: #334155;
                }
                .footer-row td {
                    border-top: 2px solid #1e293b;
                    background-color: #f8fafc;
                    font-weight: bold;
                    color: #0f172a;
                }
                .signature-row {
                    display: flex;
                    justify-content: space-between;
                    margin-top: 52px;
                    padding-top: 12px;
                }
                .signature-box {
                    width: 40%;
                    border-top: 1px solid #475569;
                    text-align: center;
                    font-size: 12px;
                    color: #334155;
                    padding-top: 6px;
                }
                .signature-sub {
                    font-size: 10px;
                    color: #64748b;
                    margin-top: 2px;
                }
            </style>
        </head>
        <body>
            <div class="invoice-container">
                <!-- 1. Header row -->
                <div class="header-row">
                    <div style="flex-shrink: 0;">
                        $logoHtml
                    </div>
                    <div class="header-center">
                        <h2>${escapeHtml(orgTitle)}</h2>
                        $subtitlesHtml
                        <p class="invoice-tag">अक्सिजन सिलिन्डर वितरण तथा सेवा शुल्क इनभ्वाइस</p>
                    </div>
                    <div style="flex-shrink: 0;">
                        $provinceLogoHtml
                    </div>
                </div>

                <!-- 2. Thin meta row -->
                <div class="meta-row">
                    <div><strong>आर्थिक वर्ष:</strong> ${escapeHtml(currentFiscalYear)}</div>
                    <div><strong>बिल नं:</strong> ${escapeHtml(dist.invoiceNo)}</div>
                </div>

                <!-- 3. Patient & distribution info box -->
                <div class="patient-box">
                    <div class="patient-col-left">
                        <div class="section-label">बिरामीको विवरण:</div>
                        <div class="patient-name">${escapeHtml(dist.patientName)}</div>
                        <div class="info-line">सम्पर्क नं: <strong>$patientPhoneDisplay</strong></div>
                        <div class="info-line">ठेगाना: <strong>${escapeHtml(dist.wardOrDept)}</strong></div>
                    </div>
                    <div class="patient-col-right">
                        <div class="section-label">वितरण विवरण:</div>
                        <div class="info-line">वितरण मिति (BS): <strong>${escapeHtml(dist.issuedDateBs)}</strong></div>
                        <div class="info-line">फिर्ता मिति (BS): <strong>$returnDateDisplay</strong></div>
                        <div class="info-line">वितरण गर्ने: <strong>$issuedByDisplay</strong></div>
                    </div>
                </div>

                <!-- 4. Items table -->
                <table class="items-table">
                    <thead>
                        <tr>
                            <th style="width: 45px; text-align: center;">क्र.सं.</th>
                            <th>विवरण (Description)</th>
                            <th style="width: 120px; text-align: center;">सिलिन्डर नम्बर</th>
                            <th style="width: 100px; text-align: center;">स्थिति</th>
                            <th style="width: 110px; text-align: right;">रकम (रु.)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td style="text-align: center;">1</td>
                            <td>
                                <strong>अक्सिजन सिलिन्डर वितरण तथा सेवा शुल्क</strong>
                                <div style="font-size: 11px; color: #64748b; margin-top: 2px;">
                                    Oxygen Cylinder Rental & Refill Service Fee
                                </div>
                            </td>
                            <td style="text-align: center; font-weight: bold;">${escapeHtml(dist.cylinderNo)}</td>
                            <td style="text-align: center;">${escapeHtml(dist.status)}</td>
                            <td style="text-align: right; font-weight: 500;">Rs. ${dist.serviceFee}.00</td>
                        </tr>
                        <tr class="footer-row">
                            <td colspan="4" style="text-align: right; padding: 10px;">
                                जम्मा सेवा शुल्क (Total Fee):
                            </td>
                            <td style="text-align: right; padding: 10px; font-size: 13.5px;">
                                Rs. ${dist.serviceFee}.00
                            </td>
                        </tr>
                    </tbody>
                </table>

                <!-- 5. Remarks box -->
                $remarksHtml

                <!-- 6. Signature row -->
                <div class="signature-row">
                    <div class="signature-box">
                        <div>बुझ्ने/बिरामीको सही</div>
                        <div class="signature-sub">Patient / Receiver Signature</div>
                    </div>
                    <div class="signature-box">
                        <div>अधिकृत कर्मचारीको सही</div>
                        <div class="signature-sub">Authorized Staff Signature ($issuedByDisplay)</div>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
