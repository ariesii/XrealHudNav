package com.xreal.hudnav.model

/**
 * 紅綠燈號誌狀態
 */
enum class TrafficLightState(val label: String, val colorHex: String) {
    NONE("無號誌", "#000000"),
    RED("紅燈", "#FF3B30"),
    GREEN("綠燈", "#34C759"),
    YELLOW("黃燈", "#FFCC00")
}

/**
 * 導航即時資訊資料結構
 */
data class NavInfo(
    val maneuver: ManeuverType = ManeuverType.STRAIGHT,
    val distance: String = "直行",
    val distanceMeters: Int = 1000,
    val roadNumber: String? = null,
    val roadName: String = "等待導航開始...",
    val nextAction: String? = null,
    val eta: String = "--:--",
    val remainingTime: String = "--",
    val remainingDistance: String = "--",
    val currentSpeed: Int = 0,
    val speedLimit: Int = 50,
    val cameraWarning: String? = null,
    val cameraDistance: Int? = null,
    val trafficLightState: TrafficLightState = TrafficLightState.NONE,
    val trafficLightSeconds: Int? = null,
    val mapSource: String = "Google Maps",
    val isNavigating: Boolean = false
) {
    /**
     * 是否處於超速狀態
     */
    fun isSpeeding(): Boolean = currentSpeed > speedLimit && speedLimit > 0

    /**
     * 是否有紅綠燈倒數資訊
     */
    fun hasTrafficLight(): Boolean = trafficLightState != TrafficLightState.NONE && trafficLightSeconds != null && trafficLightSeconds > 0

    /**
     * 組合道路標示顯示字串
     */
    fun getFullRoadDisplay(): String {
        return if (!roadNumber.isNullOrBlank()) {
            "[$roadNumber] $roadName"
        } else {
            roadName
        }
    }
}
