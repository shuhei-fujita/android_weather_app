package com.syuheifujita.android_weather_app.Model

import java.io.Serializable

data class Sys (

    val message: Double,
    val country: String,
    val sunrise: Double,
    val sunset: Double

): Serializable
