package com.example.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class TemperatureThresholdSettings(
    var fridgeMin: Double = 2.0,
    var fridgeMax: Double = 8.0,
    var freezerMin: Double = -25.0,
    var freezerMax: Double = -15.0,
    var carrierMin: Double = 2.0,
    var carrierMax: Double = 8.0,
    var storeTempMin: Double = 15.0,
    var storeTempMax: Double = 25.0,
    var storeHumidityMin: Double = 30.0,
    var storeHumidityMax: Double = 65.0,
    var fiscalYear: String = "2081/082",
    var note: String = "WHO/EPI मापदण्ड: फ्रिज २–८°C, फ्रिजर -१५ देखि -२५°C, औषधि स्टोर १५–२५°C।"
)
