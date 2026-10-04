package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NepaliDateUtils {

    private val timeZoneAsiaKathmandu = TimeZone.getTimeZone("Asia/Kathmandu")

    val nepaliMonths = listOf(
        "वैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज",
        "कात्तिक", "मंसीर", "पुस", "माघ", "फागुन", "चैत"
    )

    val nepaliMonthsEn = listOf(
        "Baishakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin",
        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    )

    // Days in each month for BS years from 2075 to 2085
    private val bsYearData = mapOf(
        2075 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2076 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2077 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2078 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2079 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2080 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2081 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2082 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2083 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2084 to intArrayOf(31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30),
        2085 to intArrayOf(31, 32, 31, 32, 30, 31, 30, 30, 29, 30, 30, 30)
    )

    // Reference epoch: 2075-01-01 BS corresponds to 2018-04-14 AD in Asia/Kathmandu
    private val epochAdMillis: Long = run {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kathmandu"))
        cal.set(2018, Calendar.APRIL, 14, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    /**
     * Converts a Gregorian Date to Bikram Sambat (BS) date string: "YYYY-MM-DD"
     */
    fun adToBs(date: Date = Date()): String {
        val targetCal = Calendar.getInstance(timeZoneAsiaKathmandu).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val epochCal = Calendar.getInstance(timeZoneAsiaKathmandu).apply {
            timeInMillis = epochAdMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - epochCal.timeInMillis
        val diffDays = (diffMillis / (1000L * 60L * 60L * 24L)).toInt()
        var remainingDays = diffDays

        if (remainingDays < 0) {
            // Fallback for dates before reference epoch
            return "2075-01-01"
        }

        var bsYear = 2075
        var bsMonth = 1
        var bsDay = 1

        while (bsYear <= 2085) {
            val monthDays = bsYearData[bsYear] ?: intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30)
            val daysInYear = monthDays.sum()

            if (remainingDays >= daysInYear) {
                remainingDays -= daysInYear
                bsYear++
            } else {
                for (m in 0 until 12) {
                    val daysInMonth = monthDays[m]
                    if (remainingDays >= daysInMonth) {
                        remainingDays -= daysInMonth
                    } else {
                        bsMonth = m + 1
                        bsDay = remainingDays + 1
                        remainingDays = 0
                        break
                    }
                }
                break
            }
        }

        return "%04d-%02d-%02d".format(bsYear, bsMonth, bsDay)
    }

    /**
     * Converts a Bikram Sambat (BS) date string ("YYYY-MM-DD" or "YYYY/MM/DD") to Gregorian AD date string "YYYY-MM-DD".
     */
    fun bsToAd(bsDate: String): String {
        if (bsDate.isBlank()) return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = timeZoneAsiaKathmandu }.format(Date())
        val normalized = bsDate.replace('/', '-')
        val parts = normalized.split("-")
        if (parts.size != 3) return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = timeZoneAsiaKathmandu }.format(Date())

        val targetYear = parts[0].toIntOrNull() ?: 2081
        val targetMonth = parts[1].toIntOrNull() ?: 1
        val targetDay = parts[2].toIntOrNull() ?: 1

        var totalDays = 0
        for (y in 2075 until targetYear) {
            val monthDays = bsYearData[y] ?: intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30)
            totalDays += monthDays.sum()
        }
        val yearData = bsYearData[targetYear] ?: intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30)
        for (m in 0 until targetMonth - 1) {
            totalDays += yearData[m]
        }
        totalDays += (targetDay - 1)

        val cal = Calendar.getInstance(timeZoneAsiaKathmandu).apply {
            timeInMillis = epochAdMillis
            add(Calendar.DAY_OF_YEAR, totalDays)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = timeZoneAsiaKathmandu
        }
        return sdf.format(cal.time)
    }

    /**
     * Verifies conversion accuracy against known reference dates across months and year boundaries.
     */
    fun verifyKnownDates(): Boolean {
        val references = mapOf(
            "2018-04-14" to "2075-01-01",
            "2026-04-14" to "2083-01-01",
            "2026-09-02" to "2083-05-17",
            "2026-09-03" to "2083-05-18",
            "2027-04-14" to "2084-01-01"
        )
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = timeZoneAsiaKathmandu
        }
        for ((adStr, expectedBs) in references) {
            val date = sdf.parse(adStr) ?: continue
            val computedBs = adToBs(date)
            if (computedBs != expectedBs) {
                return false
            }
        }
        return true
    }

    fun todayBs(): String = adToBs(Date())

    fun todayTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun toNepaliDigits(input: String): String {
        val devanagariDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
        val sb = StringBuilder()
        for (c in input) {
            if (c in '0'..'9') {
                sb.append(devanagariDigits[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Formats "2081/05/18" or "2081-05-18" to Nepali readable: "२०८१ भदौ १८"
     */
    fun formatBsNepali(bsDate: String): String {
        val normalized = bsDate.replace('-', '/')
        val parts = normalized.split("/")
        if (parts.size != 3) return bsDate
        val year = parts[0]
        val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: 0
        val day = parts[2]

        val monthName = if (monthIndex in 0..11) nepaliMonths[monthIndex] else ""
        return "${toNepaliDigits(year)} $monthName ${toNepaliDigits(day.trimStart('0'))}"
    }

    fun getMonthDays(year: Int, monthIndex: Int): Int {
        val days = bsYearData[year] ?: intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30)
        return if (monthIndex in 0 until 12) days[monthIndex] else 30
    }

    /**
     * Determines default current fiscal year in Nepal (starts Shrawan / 4th month).
     * E.g. "2081/082" or "2083/084"
     */
    fun currentFiscalYear(bsDate: String = todayBs()): String {
        val normalized = bsDate.replace('-', '/')
        val parts = normalized.split("/")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2083
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 5
        return if (month >= 4) {
            val nextYearShort = "%03d".format((year + 1) % 1000)
            "$year/$nextYearShort"
        } else {
            val prevYear = year - 1
            val thisYearShort = "%03d".format(year % 1000)
            "$prevYear/$thisYearShort"
        }
    }

    val availableFiscalYears = listOf(
        "2080/081",
        "2081/082",
        "2082/083",
        "2083/084",
        "2084/085"
    )
}
