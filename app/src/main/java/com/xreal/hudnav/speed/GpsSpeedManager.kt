package com.xreal.hudnav.speed

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.xreal.hudnav.model.NavStateRepository
import kotlin.math.roundToInt

/**
 * GPS 即時車速與位置監聽管理器
 */
class GpsSpeedManager(private val context: Context) {

    companion object {
        private const val TAG = "GpsSpeedManager"
        private const val MIN_TIME_MS = 1000L // 1 秒更新一次
        private const val MIN_DISTANCE_M = 1.0f // 1 公尺更新一次
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val speedCameraManager = SpeedCameraManager()
    private var isListening = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // 速度單位換算：m/s 轉換為 km/h
            val rawSpeed = if (location.hasSpeed()) location.speed * 3.6f else 0f
            val currentSpeedKmH = if (rawSpeed < 3.0f) 0 else rawSpeed.roundToInt()

            Log.d(TAG, "GPS 更新 -> 速度:  km/h, 經緯度: (, )")

            // 更新至全域狀態
            NavStateRepository.updateSpeed(currentSpeedKmH)

            // 比對測速照相機距離
            val cameraAlert = speedCameraManager.checkNearestCamera(location.latitude, location.longitude)
            if (cameraAlert != null) {
                NavStateRepository.updateCameraAlert(
                    warningTag = "📷 ",
                    distanceMeters = cameraAlert.distanceMeters,
                    speedLimit = cameraAlert.speedLimit
                )
            } else {
                NavStateRepository.updateCameraAlert(null, null)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isListening) return
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener
                )
                isListening = true
                Log.i(TAG, "GPS 車速監聽已啟動")
            }
        } catch (e: Exception) {
            Log.e(TAG, "啟動 GPS 監聽失敗", e)
        }
    }

    fun stopTracking() {
        if (!isListening) return
        try {
            locationManager.removeUpdates(locationListener)
            isListening = false
            Log.i(TAG, "GPS 車速監聽已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止 GPS 監聽失敗", e)
        }
    }
}
