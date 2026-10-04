package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OxygenDistributionRecord
import com.example.model.User
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorderNeutral
import com.example.ui.theme.PolishHeroAction
import com.example.ui.theme.PolishHeroCardBg
import com.example.ui.theme.PolishSafeBadgeBg
import com.example.ui.theme.PolishSafeBadgeText
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.util.NepaliDateUtils
import com.example.util.ReportExportUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OxygenInvoiceScreen(
    record: OxygenDistributionRecord,
    currentUser: User?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val orgName = currentUser?.organizationName?.ifBlank { "स्वास्थ्य संस्था (Health Facility)" }
        ?: "स्वास्थ्य संस्था (Health Facility)"
    val fiscalYear = NepaliDateUtils.currentFiscalYear()

    val isReturned = record.status.contains("Returned", ignoreCase = true) || record.status.contains("फिर्ता", ignoreCase = true)

    Scaffold(
        containerColor = PolishBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "अक्सिजन सेवा बिल (Invoice)",
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_invoice")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "पछाडि"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            exportAndShareInvoicePdf(context, orgName, fiscalYear, record) { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("PDF शेयर गर्दा त्रुटि: $error")
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_share_invoice_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "PDF शेयर गर्नुहोस्"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PolishBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Invoice Paper Card Preview
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PolishCardBorderNeutral),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_invoice_preview")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Organization & Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(PolishHeroCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🫧", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = orgName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "अक्सिजन सिलिन्डर वितरण तथा सेवा रसिद",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PolishHeroAction,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Oxygen Service Invoice & Distribution Receipt",
                            fontSize = 11.sp,
                            color = PolishTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "आर्थिक वर्ष (Fiscal Year): $fiscalYear",
                            fontSize = 11.5.sp,
                            color = PolishTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = PolishCardBorderNeutral, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Invoice Metadata Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "रसिद नं. (Invoice No):",
                                fontSize = 11.sp,
                                color = PolishTextSecondary
                            )
                            Text(
                                text = record.invoiceNo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "मिति (Date BS):",
                                fontSize = 11.sp,
                                color = PolishTextSecondary
                            )
                            Text(
                                text = NepaliDateUtils.toNepaliDigits(record.issuedDateBs),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Patient Information Card
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PolishBackground),
                        border = BorderStroke(0.5.dp, PolishCardBorderNeutral),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "प्राप्तकर्ता / बिरामी विवरण (Patient Details)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(text = "नाम: ${record.patientName}", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                    if (!record.patientPhone.isNullOrBlank()) {
                                        Text(
                                            text = "फोन: ${NepaliDateUtils.toNepaliDigits(record.patientPhone)}",
                                            fontSize = 12.sp,
                                            color = PolishTextSecondary
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(text = "ठेगाना (Address):", fontSize = 11.sp, color = PolishTextSecondary)
                                    Text(text = record.wardOrDept, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Item Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PolishHeroCardBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "विवरण (Description)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishHeroAction
                        )
                        Text(
                            text = "रकम (Amount)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishHeroAction
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Line Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "अक्सिजन सिलिन्डर वितरण तथा सेवा शुल्क",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = "सिलिन्डर नं: ${record.cylinderNo}",
                                fontSize = 12.sp,
                                color = PolishTextSecondary
                            )
                        }

                        Text(
                            text = "रु. ${NepaliDateUtils.toNepaliDigits(String.format(java.util.Locale.US, "%,d", record.serviceFee))}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = PolishCardBorderNeutral, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Total Amount Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "जम्मा रकम (Total Amount):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                        Text(
                            text = "रु. ${NepaliDateUtils.toNepaliDigits(String.format(java.util.Locale.US, "%,d", record.serviceFee))}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishHeroAction
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status & Return Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "अवस्था: ${record.status}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishTextPrimary
                        )

                        if (isReturned && !record.returnDateBs.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PolishSafeBadgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "फिर्ता: ${NepaliDateUtils.toNepaliDigits(record.returnDateBs)} (${record.returnCondition ?: "Empty"})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishSafeBadgeText
                                )
                            }
                        }
                    }

                    if (!record.remarks.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "कैफियत: ${record.remarks}",
                            fontSize = 12.sp,
                            color = PolishTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Signatures
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(1.dp)
                                    .background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "प्राप्तकर्ताको दस्तखत",
                                fontSize = 11.sp,
                                color = PolishTextSecondary
                            )
                            Text(
                                text = "(Recipient Signature)",
                                fontSize = 9.sp,
                                color = PolishTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(1.dp)
                                    .background(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "फाँटवाला / वितरक",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextSecondary
                            )
                            Text(
                                text = record.issuedBy ?: "स्टाफ",
                                fontSize = 10.sp,
                                color = PolishTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("पछाडि (Back)")
                }

                Button(
                    onClick = {
                        exportAndShareInvoicePdf(context, orgName, fiscalYear, record) { err ->
                            scope.launch {
                                snackbarHostState.showSnackbar("PDF निर्माण असफल: $err")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("btn_print_invoice"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF शेयर / प्रिन्ट", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun exportAndShareInvoicePdf(
    context: Context,
    orgName: String,
    fiscalYear: String,
    record: OxygenDistributionRecord,
    onError: (String) -> Unit
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 15f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            color = AndroidColor.rgb(27, 110, 243)
            textSize = 12f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 10f
        }
        val boldPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val secondaryPaint = Paint().apply {
            color = AndroidColor.DKGRAY
            textSize = 9f
        }
        val linePaint = Paint().apply {
            color = AndroidColor.LTGRAY
            strokeWidth = 0.8f
        }
        val tableBgPaint = Paint().apply {
            color = AndroidColor.rgb(240, 244, 255)
        }

        val centerX = 297.5f
        var y = 50f

        // Header
        canvas.drawText(orgName, centerX, y, titlePaint)
        y += 18f
        canvas.drawText("अक्सिजन सिलिन्डर वितरण तथा सेवा रसिद (Oxygen Service Invoice)", centerX, y, subPaint)
        y += 14f
        canvas.drawText("आर्थिक वर्ष: $fiscalYear | तयार मिति: ${NepaliDateUtils.todayBs()}", centerX, y, secondaryPaint.apply { textAlign = Paint.Align.CENTER })
        secondaryPaint.textAlign = Paint.Align.LEFT

        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Invoice Meta
        canvas.drawText("रसिद नं. (Invoice No): ${record.invoiceNo}", 40f, y, boldPaint)
        canvas.drawText("वितरण मिति (BS): ${record.issuedDateBs}", 380f, y, boldPaint)
        y += 16f

        // Patient Box
        canvas.drawRect(40f, y, 555f, y + 42f, tableBgPaint)
        canvas.drawText("बिरामी / प्राप्तकर्ता: ${record.patientName}", 50f, y + 16f, boldPaint)
        if (!record.patientPhone.isNullOrBlank()) {
            canvas.drawText("सम्पर्क फोन: ${record.patientPhone}", 50f, y + 32f, textPaint)
        }
        canvas.drawText("ठेगाना: ${record.wardOrDept}", 360f, y + 16f, textPaint)
        canvas.drawText("वितरणकर्ता: ${record.issuedBy ?: "स्टाफ"}", 360f, y + 32f, textPaint)

        y += 62f

        // Table Header
        canvas.drawRect(40f, y, 555f, y + 20f, Paint().apply { color = AndroidColor.rgb(220, 230, 250) })
        canvas.drawText("क्र.सं.", 48f, y + 14f, boldPaint)
        canvas.drawText("सेवा विवरण (Description)", 85f, y + 14f, boldPaint)
        canvas.drawText("सिलिन्डर नं", 360f, y + 14f, boldPaint)
        canvas.drawText("रकम (रु.)", 480f, y + 14f, boldPaint)

        y += 22f
        canvas.drawText("१.", 48f, y + 16f, textPaint)
        canvas.drawText("अक्सिजन सिलिन्डर वितरण तथा सेवा शुल्क", 85f, y + 16f, textPaint)
        canvas.drawText(record.cylinderNo, 360f, y + 16f, textPaint)
        canvas.drawText("रु. ${record.serviceFee}", 480f, y + 16f, boldPaint)

        y += 30f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // Total
        canvas.drawText("कुल जम्मा रकम (Total NPR):", 340f, y, boldPaint)
        canvas.drawText("रु. ${record.serviceFee}", 480f, y, boldPaint)

        y += 30f
        // Return info
        canvas.drawText("स्थिति: ${record.status}", 40f, y, textPaint)
        if (record.status.contains("Returned", ignoreCase = true) || record.status.contains("फिर्ता")) {
            canvas.drawText("फिर्ता मिति: ${record.returnDateBs ?: "-"} | अवस्था: ${record.returnCondition ?: "Empty"}", 240f, y, textPaint)
        }

        if (!record.remarks.isNullOrBlank()) {
            y += 16f
            canvas.drawText("कैफियत: ${record.remarks}", 40f, y, secondaryPaint)
        }

        // Signatures
        y = 700f
        canvas.drawLine(60f, y, 180f, y, linePaint)
        canvas.drawText("प्राप्तकर्ताको दस्तखत", 80f, y + 14f, secondaryPaint)

        canvas.drawLine(415f, y, 535f, y, linePaint)
        canvas.drawText("अधिकृत वितरकको दस्तखत", 420f, y + 14f, secondaryPaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Oxygen_Invoice_${record.invoiceNo}.pdf")
        val out = FileOutputStream(file)
        pdfDocument.writeTo(out)
        out.close()
        pdfDocument.close()

        ReportExportUtils.shareFile(
            context = context,
            file = file,
            mimeType = "application/pdf",
            title = "अक्सिजन सेवा रसिद शेयर गर्नुहोस्"
        )
    } catch (e: Exception) {
        e.printStackTrace()
        onError(e.localizedMessage ?: "Unknown PDF error")
    }
}
