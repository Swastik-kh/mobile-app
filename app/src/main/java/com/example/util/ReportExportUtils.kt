package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.ColdChainLogEntry
import com.example.model.StoreTemperatureLogEntry
import java.io.File
import java.io.FileOutputStream

object ReportExportUtils {

    /**
     * Generates a PDF report for Cold Chain temperature logs.
     */
    fun exportColdChainPdf(
        context: Context,
        orgName: String,
        logs: List<ColdChainLogEntry>,
        dateRangeText: String = ""
    ): Result<File> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isFakeBoldText = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(30, 60, 114)
                textSize = 10f
                isFakeBoldText = true
            }
            val headerBgPaint = Paint().apply {
                color = Color.rgb(230, 238, 250)
            }
            val rowTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
            }
            val alertRowBgPaint = Paint().apply {
                color = Color.rgb(255, 235, 235)
            }
            val normalRowBgPaint = Paint().apply {
                color = Color.rgb(248, 249, 250)
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.5f
            }

            var yPos = 40f
            val startX = 36f
            val tableWidth = 523f

            // Title & Header
            canvas.drawText("कोल्ड चेन तापक्रम अनुगमन प्रतिवेदन", startX, yPos, titlePaint)
            yPos += 18f
            canvas.drawText("संस्था: $orgName | $dateRangeText | तयार मिति: ${NepaliDateUtils.todayBs()}", startX, yPos, subtitlePaint)
            yPos += 28f

            // Table headers
            val colDate = startX + 5f
            val colTime = startX + 75f
            val colDevice = startX + 125f
            val colTemp = startX + 225f
            val colStatus = startX + 285f
            val colAction = startX + 375f
            val colRecorder = startX + 465f

            canvas.drawRect(startX, yPos - 12f, startX + tableWidth, yPos + 6f, headerBgPaint)
            canvas.drawText("मिति (BS)", colDate, yPos, headerPaint)
            canvas.drawText("सत्र", colTime, yPos, headerPaint)
            canvas.drawText("उपकरण", colDevice, yPos, headerPaint)
            canvas.drawText("तापक्रम", colTemp, yPos, headerPaint)
            canvas.drawText("अवस्था", colStatus, yPos, headerPaint)
            canvas.drawText("सुधारात्मक कदम", colAction, yPos, headerPaint)
            canvas.drawText("रेकर्डकर्ता", colRecorder, yPos, headerPaint)

            // Rows
            val maxRows = 22
            var rowIndex = 0
            for (item in logs.take(maxRows)) {
                yPos += 18f
                val rowBg = if (!item.isWithinSafeRange) alertRowBgPaint else if (rowIndex % 2 == 0) normalRowBgPaint else Paint().apply { color = Color.WHITE }
                canvas.drawRect(startX, yPos - 12f, startX + tableWidth, yPos + 6f, rowBg)

                canvas.drawText(item.dateBs, colDate, yPos, rowTextPaint)
                canvas.drawText(item.session, colTime, yPos, rowTextPaint)
                canvas.drawText(item.equipmentName.take(18).ifBlank { item.deviceType.take(18) }, colDevice, yPos, rowTextPaint)
                canvas.drawText("${item.tempCelsius}°C", colTemp, yPos, rowTextPaint)

                val statusText = if (item.isWithinSafeRange) "सुरक्षित (Safe)" else "दायरा बाहिर (Alarm)"
                canvas.drawText(statusText, colStatus, yPos, rowTextPaint)

                val actionText = if (item.correctiveAction?.isNotBlank() == true) item.correctiveAction!!.take(30) else "-"
                canvas.drawText(actionText, colAction, yPos, rowTextPaint)

                canvas.drawText(item.recordedBy.take(18), colRecorder, yPos, rowTextPaint)

                canvas.drawLine(startX, yPos + 6f, startX + tableWidth, yPos + 6f, linePaint)
                rowIndex++
            }

            // Footer note
            yPos += 26f
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 8.5f
            }
            canvas.drawText("Smart Log प्रणाली • Unique Healthh & Smart Health Nepal", startX, yPos, footerPaint)

            pdfDocument.finishPage(page)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "ColdChain_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates CSV for Cold Chain temperature logs.
     */
    fun exportColdChainCsv(
        context: Context,
        orgName: String,
        logs: List<ColdChainLogEntry>
    ): Result<File> {
        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "ColdChain_Logs_${System.currentTimeMillis()}.csv")

            val outputStream = FileOutputStream(file)
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val sb = StringBuilder()
            sb.append("संस्था:,").append("\"").append(orgName).append("\"\n")
            sb.append("प्रतिवेदन:,कोल्ड चेन तापक्रम अनुगमन लग (Cold Chain Temperature Log)\n")
            sb.append("तयार मिति:,${NepaliDateUtils.todayBs()}\n\n")

            sb.append("क्र.सं.,मिति (BS),सत्र,उपकरण ID,उपकरण नाम,तापक्रम (°C),न्यूनतम,अधिकतम,अवस्था,अलार्म,सुधारात्मक कदम,कैफियत,रेकर्डकर्ता\n")

            logs.forEachIndexed { index, item ->
                sb.append("${index + 1},")
                sb.append("\"${item.dateBs}\",")
                sb.append("\"${item.session}\",")
                sb.append("\"${item.equipmentId}\",")
                sb.append("\"${item.equipmentName}\",")
                sb.append("${item.tempCelsius},")
                sb.append("${item.minTempOfDay ?: ""},")
                sb.append("${item.maxTempOfDay ?: ""},")
                sb.append(if (item.isWithinSafeRange) "\"सुरक्षित (Safe)\"," else "\"दायरा बाहिर (Out of Range)\",")
                sb.append(if (item.alarmTriggered) "\"हो (Yes)\"," else "\"होइन (No)\",")
                sb.append("\"${(item.correctiveAction ?: "").replace("\"", "\"\"")}\",")
                sb.append("\"${(item.remarks ?: "").replace("\"", "\"\"")}\",")
                sb.append("\"${item.recordedBy.replace("\"", "\"\"")}\"\n")
            }

            outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
            outputStream.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates CSV for Store Room temperature logs.
     */
    fun exportStoreTempCsv(
        context: Context,
        orgName: String,
        logs: List<StoreTemperatureLogEntry>
    ): Result<File> {
        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "StoreTemp_Logs_${System.currentTimeMillis()}.csv")

            val outputStream = FileOutputStream(file)
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val sb = StringBuilder()
            sb.append("संस्था:,").append("\"").append(orgName).append("\"\n")
            sb.append("प्रतिवेदन:,स्टोर तापक्रम तथा आर्द्रता लग (Store Temperature & Humidity)\n")
            sb.append("तयार मिति:,${NepaliDateUtils.todayBs()}\n\n")

            sb.append("क्र.सं.,मिति (BS),सत्र,स्टोर कोठा ID,कोठा नाम,तापक्रम (°C),आर्द्रता (%),अवस्था,सुधारात्मक कदम,कैफियत,रेकर्डकर्ता\n")

            logs.forEachIndexed { index, item ->
                sb.append("${index + 1},")
                sb.append("\"${item.dateBs}\",")
                sb.append("\"${item.session}\",")
                sb.append("\"${item.roomId}\",")
                sb.append("\"${item.roomName}\",")
                sb.append("${item.tempCelsius},")
                sb.append("${item.humidityPercent ?: ""},")
                sb.append(if (item.isWithinSafeRange) "\"सुरक्षित\"," else "\"दायरा बाहिर\",")
                sb.append("\"${(item.correctiveAction ?: "").replace("\"", "\"\"")}\",")
                sb.append("\"${(item.remarks ?: "").replace("\"", "\"\"")}\",")
                sb.append("\"${item.recordedBy.replace("\"", "\"\"")}\"\n")
            }

            outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
            outputStream.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
