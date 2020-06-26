package com.syuheifujita.android_weather_app.network

import com.syuheifujita.android_weather_app.model.WeatherResponseModel
import retrofit.Call
import retrofit.http.GET
import retrofit.http.Query

interface WeatherService {

    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String?,
        @Query("appid") appid:String?
    ): Call<WeatherResponseModel>
}
