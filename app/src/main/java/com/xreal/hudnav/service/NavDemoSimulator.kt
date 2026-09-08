package com.xreal.hudnav.service

import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import com.xreal.hudnav.model.TrafficLightState
import kotlinx.coroutines.*

object NavDemoSimulator {
    private var simulationJob: Job? = null

    fun startSimulation(scope: CoroutineScope) {
        stopSimulation()
        simulationJob = scope.launch(Dispatchers.Default) {
            val demoSteps = listOf(
                // 情境 1：使用者高德截圖原貌 -> 48m 民族路，右轉，紅燈 24 秒等待！
                NavInfo(
                    maneuver = ManeuverType.TURN_RIGHT,
                    distance = "48m",
                    distanceMeters = 48,
                    roadNumber = null,
                    roadName = "民族路",
                    nextAction = "請注意觀察周邊環境，謹慎駕駛",
                    remainingTime = "28 分鐘",
                    remainingDistance = "10.6 公里",
                    eta = "下午 3:19",
                    currentSpeed = 35,
                    speedLimit = 50,
                    cameraWarning = null,
                    cameraDistance = null,
                    trafficLightState = TrafficLightState.RED,
                    trafficLightSeconds = 24,
                    mapSource = "高德地圖",
                    isNavigating = true
                ),
                // 情境 2：紅綠燈秒數倒數至 5 秒
                NavInfo(
                    maneuver = ManeuverType.TURN_RIGHT,
                    distance = "30m",
                    distanceMeters = 30,
                    roadNumber = null,
                    roadName = "民族路",
                    nextAction = "請注意觀察周邊環境，謹慎駕駛",
                    remainingTime = "28 分鐘",
                    remainingDistance = "10.6 公里",
                    eta = "下午 3:19",
                    currentSpeed = 20,
                    speedLimit = 50,
                    cameraWarning = null,
                    cameraDistance = null,
                    trafficLightState = TrafficLightState.RED,
                    trafficLightSeconds = 5,
                    mapSource = "高德地圖",
                    isNavigating = true
                ),
                // 情境 3：號誌轉為綠燈！綠燈剩餘 18s，開始右轉
                NavInfo(
                    maneuver = ManeuverType.TURN_RIGHT,
                    distance = "即將右轉",
                    distanceMeters = 10,
                    roadNumber = null,
                    roadName = "民族路",
                    nextAction = "右轉進入 民族路",
                    remainingTime = "27 分鐘",
                    remainingDistance = "10.5 公里",
                    eta = "下午 3:19",
                    currentSpeed = 28,
                    speedLimit = 50,
                    cameraWarning = null,
                    cameraDistance = null,
                    trafficLightState = TrafficLightState.GREEN,
                    trafficLightSeconds = 18,
                    mapSource = "高德地圖",
                    isNavigating = true
                ),
                // 情境 4：進入民族路順暢行駛，時速加速至 56 km/h（展示放大時速與超速警示黃色），前方測速相機 [📷 50] 280m 出現！
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "600 公尺",
                    distanceMeters = 600,
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "接下來 ↰ 106縣道",
                    remainingTime = "25 分鐘",
                    remainingDistance = "10.1 公里",
                    eta = "下午 3:19",
                    currentSpeed = 56, // 超速！
                    speedLimit = 50,
                    cameraWarning = "📷 50 超速!",
                    cameraDistance = 280,
                    trafficLightState = TrafficLightState.NONE,
                    trafficLightSeconds = null,
                    mapSource = "Google Maps",
                    isNavigating = true
                ),
                // 情境 5：長途直行 -> 觸發 Smart Glance 自動淡出極致淨空
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "1.2 公里",
                    distanceMeters = 1200,
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "直行通過 縣民大道",
                    remainingTime = "22 分鐘",
                    remainingDistance = "9.5 公里",
                    eta = "下午 3:19",
                    currentSpeed = 48,
                    speedLimit = 50,
                    cameraWarning = null,
                    cameraDistance = null,
                    trafficLightState = TrafficLightState.NONE,
                    trafficLightSeconds = null,
                    mapSource = "Google Maps",
                    isNavigating = true
                )
            )

            var index = 0
            while (isActive) {
                val step = demoSteps[index % demoSteps.size]
                NavStateRepository.updateNavInfo(step)
                delay(3800)
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
