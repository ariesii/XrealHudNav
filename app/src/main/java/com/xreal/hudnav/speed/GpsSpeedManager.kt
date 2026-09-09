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
 * 支援多 Provider 融合 (GPS + Network + Passive)
 * 當系統 hasSpeed() 為 false 時，自動利用前後經緯度位移與時間差動態換算即時車速
 */
class GpsSpeedManager(private val context: Context) {

    companion object {
        private const val TAG = "GpsSpeedManager"
        private const val MIN_TIME_MS = 1000L // 1 秒更新一次
        private const val MIN_DISTANCE_M = 0.5f // 0.5 公尺更新一次
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val speedCameraManager = SpeedCameraManager()
    private var isListening = false

    private var lastLocation: Location? = null
    private var lastLocationTimeMs: Long = 0L

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "定位 Provider 啟用: $provider")
        }
        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "定位 Provider 停用: $provider")
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val nowMs = System.currentTimeMillis()
        var calculatedSpeedKmH = 0f

        // 1. 優先取系統提供的 location.speed (單位 m/s)
        if (location.hasSpeed() && location.speed > 0f) {
            calculatedSpeedKmH = location.speed * 3.6f
        } else {
            // 2. 若系統未提供 speed (常見於某些 Android ROM 或網路定位)，由位移與時間差手動反推
            val prev = lastLocation
            val prevTime = lastLocationTimeMs
            if (prev != null && prevTime > 0L) {
                val timeDeltaSec = (nowMs - prevTime) / 1000.0
                if (timeDeltaSec in 0.5..10.0) {
                    val distanceMeters = location.distanceTo(prev)
                    // 過濾 GPS 靜態漂移 (< 1.5 米不計)
                    if (distanceMeters >= 1.5f) {
                        val speedMs = distanceMeters / timeDeltaSec
                        val speedKmh = (speedMs * 3.6).toFloat()
                        if (speedKmh <= 200f) { // 剔除離群跳點
                            calculatedSpeedKmH = speedKmh
                        }
                    }
                }
            }
        }

        lastLocation = location
        lastLocationTimeMs = nowMs

        // 低速抗抖：低於 3 km/h 視為停等或靜止 0 km/h
        val finalSpeedInt = if (calculatedSpeedKmH < 3.0f) 0 else calculatedSpeedKmH.roundToInt()

        Log.d(TAG, "定位更新 [${location.provider}] -> 原始速度: ${if (location.hasSpeed()) location.speed else "無"} m/s, 計算車速: $finalSpeedInt km/h, 座標: (${location.latitude}, ${location.longitude})")

        // 更新至全域狀態
        NavStateRepository.updateSpeed(finalSpeedInt)

        // 比對測速照相機距離
        val cameraAlert = speedCameraManager.checkNearestCamera(location.latitude, location.longitude)
        if (cameraAlert != null) {
            NavStateRepository.updateCameraAlert(
                warningTag = "📷 ${cameraAlert.speedLimit}",
                distanceMeters = cameraAlert.distanceMeters,
                speedLimit = cameraAlert.speedLimit
            )
        } else {
            NavStateRepository.updateCameraAlert(null, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isListening) return
        try {
            // 優先取得最後已知位置，立即初始化
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLast = when {
                lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }
            if (bestLast != null) {
                lastLocation = bestLast
                lastLocationTimeMs = System.currentTimeMillis()
            }

            // 註冊 GPS_PROVIDER
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener
                )
            }

            // 同時註冊 NETWORK_PROVIDER 作為輔助，確保室內或地下道/高架橋下能有連續定位
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener
                )
            }

            isListening = true
            Log.i(TAG, "多 Provider GPS 車速監聽已啟動")
        } catch (e: Exception) {
            Log.e(TAG, "啟動 GPS 監聽失敗", e)
        }
    }

    fun stopTracking() {
        if (!isListening) return
        try {
            locationManager.removeUpdates(locationListener)
            isListening = false
            lastLocation = null
            lastLocationTimeMs = 0L
            Log.i(TAG, "GPS 車速監聽已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止 GPS 監聽失敗", e)
        }
    }
}
