package com.syuheifujita.android_weather_app.model

import java.io.Serializable

data class Main (

    val temp: Int,
    val pressure: Int,
    val humidity: Int,
    val temp_min: Int,
    val temp_max: Int,
    val sea_level: Int,
    val gmd_level: Int

): Serializable
