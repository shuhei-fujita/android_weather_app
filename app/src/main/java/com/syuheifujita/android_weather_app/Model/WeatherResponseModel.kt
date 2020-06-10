package com.syuheifujita.android_weather_app.Model

import java.io.Serializable

data class WeatherResponseModel (

    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visiblity: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val valsys: Sys,
    val id: Int,
    val name: String,
    val cod: Int

): Serializable