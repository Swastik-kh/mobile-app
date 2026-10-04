package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CryptoUtils
import com.example.util.NepaliDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Log", appName)
    }

    @Test
    fun `test crypto sha256 hash and verify`() {
        val password = "NepalHealth@2024"
        val hash = CryptoUtils.hashPassword(password)
        assertTrue(CryptoUtils.verifyPassword(password, hash))
        assertFalse(CryptoUtils.verifyPassword("WrongPassword", hash))
    }

    @Test
    fun `test nepali date conversion and formatting`() {
        val nepaliDigits = NepaliDateUtils.toNepaliDigits("2081/05/18")
        assertEquals("२०८१/०५/१८", nepaliDigits)

        val formatted = NepaliDateUtils.formatBsNepali("2081/05/18")
        assertTrue(formatted.contains("२०८१"))
        assertTrue(formatted.contains("भदौ"))

        assertTrue("Known date conversions must match authoritative reference dates", NepaliDateUtils.verifyKnownDates())
    }
}
