package com.xreal.hudnav.service

import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import kotlinx.coroutines.*

/**
 * 升級版路況模擬器
 * 模擬時速變換、測速相機警示、Smart Glance 自動喚醒與高德地圖情境
 */
object NavDemoSimulator {
    private var simulationJob: Job? = null

    fun startSimulation(scope: CoroutineScope) {
        stopSimulation()
        simulationJob = scope.launch(Dispatchers.Default) {
            val demoSteps = listOf(
                // 情境 1：長途直行 (500m) -> 測試 Smart Glance 自動淡出極致淨空
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "500 公尺",
                    distanceMeters = 500,
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "接下來 ↰ 106縣道",
                    remainingTime = "23 分鐘",
                    remainingDistance = "11 公里",
                    eta = "上午 10:12",
                    currentSpeed = 45,
                    speedLimit = 50,
                    cameraWarning = null,
                    cameraDistance = null,
                    mapSource = "Google Maps",
                    isNavigating = true
                ),
                // 情境 2：前方測速照相機警示出現！[📷 50] 350m
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "400 公尺",
                    distanceMeters = 400,
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "接下來 ↰ 106縣道",
                    remainingTime = "22 分鐘",
                    remainingDistance = "10.9 公里",
                    eta = "上午 10:12",
                    currentSpeed = 48,
                    speedLimit = 50,
                    cameraWarning = "📷 50",
                    cameraDistance = 350,
                    mapSource = "Google Maps",
                    isNavigating = true
                ),
                // 情境 3：超速警示！車速拉高至 56 km/h，時速數字變黃/紅，相機逼近 120m
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "320 公尺",
                    distanceMeters = 320,
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "接下來 ↰ 106縣道",
                    remainingTime = "22 分鐘",
                    remainingDistance = "10.8 公里",
                    eta = "上午 10:12",
                    currentSpeed = 56, // 超速！
                    speedLimit = 50,
                    cameraWarning = "📷 50 超速!",
                    cameraDistance = 120,
                    mapSource = "Google Maps",
                    isNavigating = true
                ),
                // 情境 4：逼近路口 (<300m) -> Smart Glance 自動喚醒，左轉流光動態啟動！
                NavInfo(
                    maneuver = ManeuverType.TURN_LEFT,
                    distance = "150 公尺後左轉",
                    distanceMeters = 150,
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "接下來 ↰ 106縣道",
                    remainingTime = "21 分鐘",
                    remainingDistance = "10.7 公里",
                    eta = "上午 10:12",
                    currentSpeed = 38,
                    speedLimit = 50,
                    cameraWarning = null, // 已通過相機
                    cameraDistance = null,
                    mapSource = "Google Maps",
                    isNavigating = true
                ),
                // 情境 5：切換為高德地圖導航風格！
                NavInfo(
                    maneuver = ManeuverType.SLIGHT_RIGHT,
                    distance = "80米後 向右前方行駛",
                    distanceMeters = 80,
                    roadNumber = null,
                    roadName = "新府路",
                    nextAction = "進入 文化路",
                    remainingTime = "18 分鐘",
                    remainingDistance = "9.2 公里",
                    eta = "上午 10:15",
                    currentSpeed = 32,
                    speedLimit = 50,
                    cameraWarning = null,
                    cameraDistance = null,
                    mapSource = "高德地圖",
                    isNavigating = true
                ),
                // 情境 6：終點抵達
                NavInfo(
                    maneuver = ManeuverType.DESTINATION,
                    distance = "抵達目的地",
                    distanceMeters = 0,
                    roadNumber = null,
                    roadName = "板橋車站 (已在右側)",
                    nextAction = null,
                    remainingTime = "1 分鐘",
                    remainingDistance = "20 公尺",
                    eta = "上午 10:16",
                    currentSpeed = 0,
                    speedLimit = 50,
                    cameraWarning = null,
                    cameraDistance = null,
                    mapSource = "Google Maps",
                    isNavigating = true
                )
            )

            var index = 0
            while (isActive) {
                val step = demoSteps[index % demoSteps.size]
                NavStateRepository.updateNavInfo(step)
                delay(3800) // 每 3.8 秒推進一個情境
                index++
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        NavStateRepository.updateNavInfo(
            NavInfo(
                isNavigating = false,
                roadName = "已停止模擬展示"
            )
        )
    }

    fun isSimulating(): Boolean = simulationJob?.isActive == true
}
