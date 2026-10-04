package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.OrgSettings
import com.example.model.OxygenDistributionRecord
import com.example.model.User
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishCardBorderNeutral
import com.example.ui.theme.PolishHeroAction
import com.example.ui.theme.PolishTextPrimary
import com.example.ui.theme.PolishTextSecondary
import com.example.util.NepaliDateUtils
import com.example.util.OxygenInvoiceHtmlBuilder
import com.example.util.ReportExportUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OxygenInvoicePreviewScreen(
    record: OxygenDistributionRecord,
    currentUser: User?,
    orgSettings: OrgSettings = OrgSettings(),
    activeOrgName: String = "",
    currentFiscalYear: String = NepaliDateUtils.currentFiscalYear(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

    val effectiveOrgName = when {
        orgSettings.orgNameNepali.isNotBlank() -> orgSettings.orgNameNepali
        orgSettings.orgNameEnglish.isNotBlank() -> orgSettings.orgNameEnglish
        activeOrgName.isNotBlank() -> activeOrgName
        !currentUser?.organizationName.isNullOrBlank() -> currentUser!!.organizationName
        else -> "स्वास्थ्य संस्था"
    }

    val initialFiscalYear = remember(currentFiscalYear, orgSettings, record) {
        currentFiscalYear.ifBlank {
            orgSettings.effectiveFiscalYear.ifBlank {
                if (!record.issuedDateBs.isNullOrBlank()) {
                    NepaliDateUtils.currentFiscalYear(record.issuedDateBs)
                } else {
                    NepaliDateUtils.currentFiscalYear()
                }
            }
        }
    }

    var selectedFiscalYear by remember(initialFiscalYear) { mutableStateOf(initialFiscalYear) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val fiscalYearList = remember(initialFiscalYear) {
        val standardList = com.example.util.FISCAL_YEARS.map { it.value }
        val combined = mutableListOf<String>()
        if (initialFiscalYear.isNotBlank()) {
            combined.add(initialFiscalYear)
        }
        standardList.forEach { if (!combined.contains(it)) combined.add(it) }
        combined
    }

    val htmlContent = remember(record, orgSettings, effectiveOrgName, selectedFiscalYear) {
        OxygenInvoiceHtmlBuilder.buildInvoiceHtml(
            dist = record,
            settings = orgSettings,
            activeOrgName = effectiveOrgName,
            currentFiscalYear = selectedFiscalYear
        )
    }

    fun triggerPrint() {
        val webView = webViewInstance
        if (webView == null) {
            Toast.makeText(context, "कृपया पृष्ठ लोड हुन दिनुहोस्...", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val jobName = "Oxygen_Invoice_${record.invoiceNo}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
                )
            } else {
                Toast.makeText(context, "प्रिन्ट सेवा उपलब्ध छैन", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch {
                snackbarHostState.showSnackbar("प्रिन्ट गर्दा त्रुटि: ${e.localizedMessage}")
            }
        }
    }

    fun triggerPdfShare() {
        exportAndShareInvoicePdf(
            context = context,
            orgName = effectiveOrgName,
            fiscalYear = selectedFiscalYear,
            record = record,
            settings = orgSettings
        ) { error ->
            scope.launch {
                snackbarHostState.showSnackbar("PDF शेयर गर्दा त्रुटि: $error")
            }
        }
    }

    Scaffold(
        containerColor = PolishBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "अक्सिजन बिल (Invoice #${record.invoiceNo})",
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary,
                        fontSize = 17.sp
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
                        onClick = { triggerPdfShare() },
                        modifier = Modifier.testTag("btn_share_invoice_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "PDF शेयर गर्नुहोस्"
                        )
                    }
                    IconButton(
                        onClick = { triggerPrint() },
                        modifier = Modifier.testTag("btn_top_print")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "प्रिन्ट गर्नुहोस्",
                            tint = PolishHeroAction
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
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Fiscal Year Selector Dropdown Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PolishCardBorderNeutral),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = PolishHeroAction,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "आर्थिक वर्ष (Fiscal Year)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextSecondary
                            )
                            Text(
                                text = "आ.व. $selectedFiscalYear",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishHeroAction
                            )
                        }
                    }

                    Box {
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedFiscalYear,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .width(140.dp)
                                    .testTag("dropdown_fiscal_year"),
                                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                fiscalYearList.forEach { fy ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "आ.व. $fy",
                                                fontWeight = if (fy == selectedFiscalYear) FontWeight.Bold else FontWeight.Normal,
                                                color = if (fy == selectedFiscalYear) PolishHeroAction else PolishTextPrimary
                                            )
                                        },
                                        onClick = {
                                            selectedFiscalYear = fy
                                            isDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("item_fiscal_year_$fy")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { triggerPrint() },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                        .testTag("btn_print_invoice"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishHeroAction)
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "प्रिन्ट गर्नुहोस् (Print)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { triggerPdfShare() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_share_pdf"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishHeroAction)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = PolishHeroAction,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PDF सेभ / Share",
                        fontWeight = FontWeight.SemiBold,
                        color = PolishHeroAction,
                        fontSize = 12.5.sp
                    )
                }
            }

            // WebView Invoice Card Container
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PolishCardBorderNeutral),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("card_invoice_webview")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isPageLoaded = true
                                    }
                                }

                                loadDataWithBaseURL(
                                    null,
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            view.loadDataWithBaseURL(
                                null,
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (!isPageLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PolishHeroAction,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun exportAndShareInvoicePdf(
    context: Context,
    orgName: String,
    fiscalYear: String,
    record: OxygenDistributionRecord,
    settings: OrgSettings = OrgSettings(),
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
            color = AndroidColor.rgb(21, 94, 117)
            textSize = 11.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 9.5f
        }
        val boldPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 9.5f
            isFakeBoldText = true
        }
        val secondaryPaint = Paint().apply {
            color = AndroidColor.DKGRAY
            textSize = 8.5f
        }
        val linePaint = Paint().apply {
            color = AndroidColor.LTGRAY
            strokeWidth = 0.8f
        }
        val tableBgPaint = Paint().apply {
            color = AndroidColor.rgb(248, 250, 252)
        }

        val centerX = 297.5f
        var y = 45f

        // Header
        canvas.drawText(orgName, centerX, y, titlePaint)
        y += 15f
        if (settings.address.isNotBlank()) {
            canvas.drawText(settings.address, centerX, y, secondaryPaint.apply { textAlign = Paint.Align.CENTER })
            y += 12f
        }
        canvas.drawText("अक्सिजन सिलिन्डर वितरण तथा सेवा शुल्क इनभ्वाइस", centerX, y, subPaint)
        y += 14f
        canvas.drawText("आर्थिक वर्ष: $fiscalYear | बिल नं: ${record.invoiceNo}", centerX, y, secondaryPaint.apply { textAlign = Paint.Align.CENTER })
        secondaryPaint.textAlign = Paint.Align.LEFT

        y += 16f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        // Patient Box
        canvas.drawRect(40f, y, 555f, y + 46f, tableBgPaint)
        canvas.drawText("बिरामी / प्राप्तकर्ता: ${record.patientName}", 50f, y + 16f, boldPaint)
        val phoneText = if (!record.patientPhone.isNullOrBlank()) record.patientPhone else "उपलब्ध छैन"
        canvas.drawText("सम्पर्क फोन: $phoneText", 50f, y + 32f, textPaint)

        canvas.drawText("ठेगाना: ${record.wardOrDept}", 360f, y + 16f, textPaint)
        canvas.drawText("वितरण मिति (BS): ${record.issuedDateBs}", 360f, y + 32f, textPaint)

        y += 62f

        // Table Header
        canvas.drawRect(40f, y, 555f, y + 20f, Paint().apply { color = AndroidColor.rgb(241, 245, 249) })
        canvas.drawText("क्र.सं.", 48f, y + 14f, boldPaint)
        canvas.drawText("विवरण (Description)", 85f, y + 14f, boldPaint)
        canvas.drawText("सिलिन्डर नं", 360f, y + 14f, boldPaint)
        canvas.drawText("स्थिति", 430f, y + 14f, boldPaint)
        canvas.drawText("रकम (रु.)", 495f, y + 14f, boldPaint)

        y += 22f
        canvas.drawText("१.", 48f, y + 16f, textPaint)
        canvas.drawText("अक्सिजन सिलिन्डर वितरण तथा सेवा शुल्क", 85f, y + 16f, textPaint)
        canvas.drawText(record.cylinderNo, 360f, y + 16f, textPaint)
        canvas.drawText(record.status, 430f, y + 16f, textPaint)
        canvas.drawText("Rs. ${record.serviceFee}.00", 495f, y + 16f, boldPaint)

        y += 30f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // Total
        canvas.drawText("जम्मा सेवा शुल्क (Total Fee):", 340f, y, boldPaint)
        canvas.drawText("Rs. ${record.serviceFee}.00", 495f, y, boldPaint)

        if (!record.remarks.isNullOrBlank()) {
            y += 26f
            canvas.drawRect(40f, y, 555f, y + 24f, Paint().apply { color = AndroidColor.rgb(255, 251, 235) })
            canvas.drawText("विशेष टिप्पणी: ${record.remarks}", 50f, y + 16f, Paint().apply {
                color = AndroidColor.rgb(146, 64, 14)
                textSize = 9f
            })
            y += 24f
        }

        // Signatures
        y = 720f
        canvas.drawLine(60f, y, 200f, y, linePaint)
        canvas.drawText("बुझ्ने/बिरामीको सही", 85f, y + 14f, secondaryPaint)

        canvas.drawLine(395f, y, 535f, y, linePaint)
        val issuer = if (!record.issuedBy.isNullOrBlank()) record.issuedBy else "Admin"
        canvas.drawText("अधिकृत कर्मचारीको सही ($issuer)", 400f, y + 14f, secondaryPaint)

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
