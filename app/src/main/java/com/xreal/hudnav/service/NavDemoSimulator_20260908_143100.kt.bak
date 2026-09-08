package com.xreal.hudnav.service

import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import kotlinx.coroutines.*

/**
 * 離線導航模擬資料產生器
 * 專為室內開發與展示設計，模擬真實路況情境
 */
object NavDemoSimulator {
    private var simulationJob: Job? = null

    /**
     * 開始模擬路程
     */
    fun startSimulation(scope: CoroutineScope) {
        stopSimulation()
        simulationJob = scope.launch(Dispatchers.Default) {
            val demoSteps = listOf(
                // 情境 1：截圖初始狀態 - 前往 106 民族路
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "500 公尺",
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "接下來 ↰ 106縣道",
                    remainingTime = "23 分鐘",
                    remainingDistance = "11 公里",
                    eta = "上午 10:12",
                    isNavigating = true
                ),
                // 情境 2：逼近路口
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "150 公尺",
                    roadNumber = "106",
                    roadName = "民族路",
                    nextAction = "接下來 ↰ 106縣道",
                    remainingTime = "22 分鐘",
                    remainingDistance = "10.8 公里",
                    eta = "上午 10:12",
                    isNavigating = true
                ),
                // 情境 3：即將轉彎
                NavInfo(
                    maneuver = ManeuverType.TURN_LEFT,
                    distance = "50 公尺後左轉",
                    roadNumber = "106",
                    roadName = "106縣道",
                    nextAction = "接續直行 新府路",
                    remainingTime = "21 分鐘",
                    remainingDistance = "10.5 公里",
                    eta = "上午 10:12",
                    isNavigating = true
                ),
                // 情境 4：轉入新路段
                NavInfo(
                    maneuver = ManeuverType.STRAIGHT,
                    distance = "800 公尺",
                    roadNumber = null,
                    roadName = "新府路",
                    nextAction = "接下來 ↱ 文化路",
                    remainingTime = "19 分鐘",
                    remainingDistance = "9.8 公里",
                    eta = "上午 10:12",
                    isNavigating = true
                ),
                // 情境 5：準備右轉
                NavInfo(
                    maneuver = ManeuverType.TURN_RIGHT,
                    distance = "100 公尺後右轉",
                    roadNumber = null,
                    roadName = "文化路",
                    nextAction = "抵達目的地",
                    remainingTime = "5 分鐘",
                    remainingDistance = "1.2 公里",
                    eta = "上午 10:12",
                    isNavigating = true
                ),
                // 情境 6：即將抵達
                NavInfo(
                    maneuver = ManeuverType.DESTINATION,
                    distance = "即將抵達",
                    roadNumber = null,
                    roadName = "目的地已在您的右手邊",
                    nextAction = null,
                    remainingTime = "1 分鐘",
                    remainingDistance = "50 公尺",
                    eta = "上午 10:12",
                    isNavigating = true
                )
            )

            var index = 0
            while (isActive) {
                val step = demoSteps[index % demoSteps.size]
                NavStateRepository.updateNavInfo(step)
                delay(4000) // 每 4 秒切換一個導航情境
                index++
            }
        }
    }

    /**
     * 停止模擬
     */
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

    /**
     * 是否正在模擬中
     */
    fun isSimulating(): Boolean = simulationJob?.isActive == true
}
