package com.xreal.hudnav.speed

import kotlin.math.*

/**
 * 測速照相機警示資料結構
 */
data class CameraAlertResult(
    val speedLimit: Int,
    val distanceMeters: Int,
    val locationName: String
)

/**
 * 測速照相機基本資料
 */
data class SpeedCameraPoint(
    val lat: Double,
    val lng: Double,
    val speedLimit: Int,
    val name: String
)

/**
 * 測速照相與科技執法資料庫管理
 */
class SpeedCameraManager {

    companion object {
        private const val ALERT_DISTANCE_THRESHOLD_M = 450 // 進入 450 公尺內觸發警示
        private const val EARTH_RADIUS_M = 6371000.0
    }

    // 內建台灣重要幹道與市區示範測速點清單（包含新北板橋民族路周邊、市民大道、台64等）
    private val cameraDatabase = listOf(
        SpeedCameraPoint(25.0118, 121.4655, 50, "新北板橋民族路測速"),
        SpeedCameraPoint(25.0135, 121.4688, 50, "新北板橋縣民大道測速"),
        SpeedCameraPoint(25.0182, 121.4623, 50, "新北板橋文化路測速"),
        SpeedCameraPoint(25.0245, 121.4721, 60, "台64線板橋段測速"),
        SpeedCameraPoint(25.0456, 121.5342, 60, "台北市民大道高架測速"),
        SpeedCameraPoint(25.0489, 121.5178, 50, "台北忠孝西路測速"),
        SpeedCameraPoint(25.0612, 121.5234, 50, "台北民權西路測速"),
        SpeedCameraPoint(24.1567, 120.6589, 50, "台中台灣大道測速"),
        SpeedCameraPoint(22.6289, 120.3012, 50, "高雄中山二路測速")
    )

    /**
     * 比對當前經緯度前方最近的測速照相機
     *
     * @param currentLat 目前緯度
     * @param currentLng 目前經度
     * @return 符合預警距離的 CameraAlertResult，無則回傳 null
     */
    fun checkNearestCamera(currentLat: Double, currentLng: Double): CameraAlertResult? {
        var nearest: CameraAlertResult? = null
        var minDistance = Double.MAX_VALUE

        for (cam in cameraDatabase) {
            val dist = calculateDistance(currentLat, currentLng, cam.lat, cam.lng)
            if (dist < ALERT_DISTANCE_THRESHOLD_M && dist < minDistance) {
                minDistance = dist
                nearest = CameraAlertResult(
                    speedLimit = cam.speedLimit,
                    distanceMeters = dist.roundToInt(),
                    locationName = cam.name
                )
            }
        }
        return nearest
    }

    /**
     * 使用 Haversine 大圓航線公式計算兩點經緯度直線公尺距離
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }
}
