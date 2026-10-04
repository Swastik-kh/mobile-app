package com.example

import com.example.model.ColdChainEquipment
import com.example.model.ColdChainLogEntry
import com.example.model.StoreTemperatureLogEntry
import com.example.model.TemperatureThresholdSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColdChainValidationTest {

    @Test
    fun testVaccineFridgeSafeTemperatureRange() {
        val device = ColdChainEquipment(
            id = "dev_ilr_1",
            name = "मुख्य खोप रेफ्रिजरेटर ILR-1",
            type = "ILR",
            isActive = true
        )

        // 4.5°C is within safe range (2 to 8°C)
        val reading1 = 4.5
        val isSafe1 = reading1 in device.safeMinTemp..device.safeMaxTemp
        assertTrue(isSafe1)

        // 1.2°C is below safe range -> freeze risk!
        val reading2 = 1.2
        val isSafe2 = reading2 in device.safeMinTemp..device.safeMaxTemp
        assertFalse(isSafe2)

        // 9.5°C is above safe range -> heat risk!
        val reading3 = 9.5
        val isSafe3 = reading3 in device.safeMinTemp..device.safeMaxTemp
        assertFalse(isSafe3)
    }

    @Test
    fun testAdditionalEquipmentTemperatureRange() {
        val device = ColdChainEquipment(
            id = "dev_ilr_2",
            name = "ILR-2",
            type = "ILR",
            isActive = true
        )

        val reading1 = 5.0
        assertTrue(reading1 in device.safeMinTemp..device.safeMaxTemp)
    }

    @Test
    fun testStoreTemperatureSafeRange() {
        val settings = TemperatureThresholdSettings(
            storeTempMin = 15.0,
            storeTempMax = 25.0
        )

        val readingNormal = 21.0
        assertTrue(readingNormal in settings.storeTempMin..settings.storeTempMax)

        val readingHot = 28.5
        assertFalse(readingHot in settings.storeTempMin..settings.storeTempMax)
    }
}
