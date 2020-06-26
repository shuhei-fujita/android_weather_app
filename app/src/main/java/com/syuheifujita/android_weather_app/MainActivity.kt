package com.syuheifujita.android_weather_app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.syuheifujita.android_weather_app.model.WeatherResponseModel
import com.syuheifujita.android_weather_app.network.WeatherService
import kotlinx.android.synthetic.main.activity_main.*
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

    private lateinit var mSharePreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        mSharePreferences = getSharedPreferences(Constant.PREFERNCE_NAME, Context.MODE_PRIVATE)

        setupUI()

        if (!isLocationEnabled()) {
            //端末自体のGPSがoffの場合の処理

            Toast.makeText(
                this, "Location provider is turned off. Plaese turn it on.", Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            requestPermission()
        }
    }

    // menu を表示させる役割
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return super.onCreateOptionsMenu(menu)
    }

    // menu　のリスナーを定義する役割
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when(item.itemId) {
            R.id.action_refresh -> {
                requestLocationData()
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // アプリ自体のGPSのpermissionをrequest
    private fun requestPermission() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        requestLocationData()
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            this@MainActivity, "You have denied location", Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissons: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    // TODO - implement showRationalDialogForPermissions()
                    showRationalDialogForPermissions()
                }

            }).onSameThread()
            .check()
    }

    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("このアプリではpermissionが必要です")
            .setPositiveButton(
                "Got to Setting"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") {dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    // callbackで位置情報を取得，ここは非同期の処理なので
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation : Location = locationResult.lastLocation

            val latitude = mLastLocation.latitude
            Log.i("latitude", "$latitude")
            val longitude = mLastLocation.longitude
            Log.i("longitude", "$longitude")

            getLocationWeatherDetail(latitude, longitude)
        }
    }

    //
    private fun getLocationWeatherDetail(latitude: Double, longitude: Double) {
        if(Constant.isNetworkAvailable(this@MainActivity)) {
            // スマホがインターネットに接続されている場合

            // apiのBaseUrlを作成
            val retrofit: Retrofit = Retrofit
                .Builder()
                .baseUrl(Constant.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // interfaceを初期化
            val service: WeatherService = retrofit
                .create<WeatherService>(WeatherService::class.java)

            // クエリを作成
            val listCall: Call<WeatherResponseModel> = service.getWeather(
                latitude, longitude, Constant.METRIC_UNIT, Constant.APP_ID
            )

//            showCustomDialog()

            listCall.enqueue(object : Callback<WeatherResponseModel> {
                @SuppressLint("")
                override fun onResponse(
                    response: Response<WeatherResponseModel>,
                    retrofit: Retrofit
                ) {
                    if (response.isSuccess) {
                        val weatherList: WeatherResponseModel = response.body()
                        Log.i("Response result", "$weatherList")

                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editer = mSharePreferences.edit()
                        editer.putString(Constant.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editer.apply()

                        setupUI()
                    } else {
                        val rc = response.code()
                        when(rc) {
                            400 -> {
                                Log.e("Error 400", "Bad connection")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    Log.e("Errorrrrrr", t.message.toString())
                }
            })

        } else {
            // スマホがインターネットに接続されていない場合
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCustomDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if(mProgressDialog != null){
            mProgressDialog!!.dismiss()
        }
    }

    private fun setupUI() {

        val weatherResponseJsonString =
            mSharePreferences.getString(Constant.WEATHER_RESPONSE_DATA, "")

        if(!weatherResponseJsonString.isNullOrEmpty()) {

            val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponseModel::class.java)

            for(i in weatherList.weather.indices) {
                Log.i("Weather Name", weatherList.weather[i].main)

                tv_weather.text = weatherList.weather[i].main
                tv_weather_description.text = weatherList.weather[i].description
                tv_degree.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

                tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
                tv_min.text = weatherList.main.temp_min.toString() + " min"
                tv_max.text = weatherList.main.temp_max.toString() + " max"
                tv_speed.text = weatherList.wind.speed.toString()
                tv_weather.text = weatherList.name
                tv_country.text = weatherList.sys.country
                tv_sunrise.text = unixTime(weatherList.sys.sunrise)
                tv_sunset.text = unixTime(weatherList.sys.sunset)

                when(weatherList.weather[i].icon) {
                    "01d" -> iv_weather.setImageResource(R.drawable.sunny)
                    "02d" -> iv_weather.setImageResource(R.drawable.cloud)
                    "03d" -> iv_weather.setImageResource(R.drawable.cloud)
                    "04d" -> iv_weather.setImageResource(R.drawable.cloud)
                    "04n" -> iv_weather.setImageResource(R.drawable.cloud)
                    "10d" -> iv_weather.setImageResource(R.drawable.rain)
                    "11d" -> iv_weather.setImageResource(R.drawable.storm)
                    "13d" -> iv_weather.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_weather.setImageResource(R.drawable.cloud)
                    "02d" -> iv_weather.setImageResource(R.drawable.cloud)
                    "03d" -> iv_weather.setImageResource(R.drawable.cloud)
                    "10n" -> iv_weather.setImageResource(R.drawable.cloud)
                    "11d" -> iv_weather.setImageResource(R.drawable.rain)
                    "13d" -> iv_weather.setImageResource(R.drawable.snowflake)
                }
            }
        }
    }

    private fun getUnit(value: String): String? {
        Log.i(" ", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(time: Long) : String? {
        val date = Date(time * 1000L)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(date)
    }
}
