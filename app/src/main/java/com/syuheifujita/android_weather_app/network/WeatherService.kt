package com.syuheifujita.android_weather_app.network

import com.syuheifujita.android_weather_app.model.WeatherResponseModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {

    @GET("2.5/weather")
    fun getWeather(

        @Query("lat") lat: Double,
        @Query("long") long: Double,
        @Query("units") units: String,
        @Query("appid") appid:String?

    ): Call<WeatherResponseModel>

}
