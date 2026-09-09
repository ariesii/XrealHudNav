package com.xreal.hudnav.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.xreal.hudnav.model.LaneItem
import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import com.xreal.hudnav.model.TrafficLightState
import com.xreal.hudnav.model.TrafficSegment
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections

/**
 * 高德地圖標準車載/手機雙模式導航廣播接收器
 * 參考 zd423/AMap- (Navi-Link) 完整協定與型別安全機制重構
 * 支援 5 大核心 KEY_TYPE:
 * - 10001: 導航/巡航主資訊 (NEW_ICON/ICON, 距離, 道路名, ETA, 限速, 測速相機)
 * - 13012: 官方車道線 (EXTRA_DRIVE_WAY JSON 解析)
 * - 60073: 官方紅綠燈狀態與倒數秒數 (trafficLightStatus, redLightCountDownSeconds)
 * - 13011: TMC 路況分段 (EXTRA_TMC_SEGMENT)
 * - 10019: 導航結束/晝夜模式 (EXTRA_STATE)
 */
class AmapBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AmapReceiver"
        const val ACTION_AMAP_SEND = "AUTONAVI_STANDARD_BROADCAST_SEND"
        const val ACTION_AMAP_RECV = "AUTONAVI_STANDARD_BROADCAST_RECV"

        // 高德目標套件名稱 (包含手機版 minimap 與車機版 amapauto)
        private val AMAP_PACKAGES = arrayOf(
            "com.autonavi.minimap",
            "com.autonavi.amapauto"
        )

        /**
         * 向高德地圖發送多重握手指令，主動請求啟動標準廣播推送與狀態回報
         */
        fun requestAmapBroadcast(context: Context) {
            try {
                // 1. 請求開啟廣播發送 (KEY_TYPE = 10013, EXTRA_TYPE = 1)
                val intentOpen = Intent(ACTION_AMAP_RECV).apply {
                    putExtra("KEY_TYPE", 10013)
                    putExtra("EXTRA_TYPE", 1)
                    putExtra("SOURCE_APP", context.packageName)
                }
                context.sendBroadcast(intentOpen)

                // 2. 請求回報當前運行狀態 (KEY_TYPE = 13030)
                val intentStatus = Intent(ACTION_AMAP_RECV).apply {
                    putExtra("KEY_TYPE", 13030)
                    putExtra("SOURCE_APP", context.packageName)
                }
                context.sendBroadcast(intentStatus)

                // 3. 針對高德手機版與車機版顯式發送定向廣播 (確保突破背景發送限制)
                for (pkg in AMAP_PACKAGES) {
                    try {
                        val explicitIntent = Intent(ACTION_AMAP_RECV).apply {
                            setPackage(pkg)
                            putExtra("KEY_TYPE", 10013)
                            putExtra("EXTRA_TYPE", 1)
                            putExtra("SOURCE_APP", context.packageName)
                        }
                        context.sendBroadcast(explicitIntent)
                    } catch (e: Exception) {
                        Log.w(TAG, "發送高德顯式廣播至 $pkg 略過: ${e.message}")
                    }
                }

                NavStateRepository.appendNotificationLog("[高德廣播] 已發送多重握手連線請求 (10013/13030)")
                Log.i(TAG, "已向高德發送標準廣播連線握手請求")
            } catch (e: Exception) {
                Log.e(TAG, "發送高德廣播握手失敗", e)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return
        val action = intent.action ?: return

        if (action == ACTION_AMAP_SEND) {
            val keyType = getIntSafe(intent, "KEY_TYPE", 0)
            dumpDebugExtras(intent, keyType)

            when (keyType) {
                10001 -> handleNaviInfo(intent)
                13012 -> handleDriveWay(intent)
                60073 -> handleTrafficLight(intent)
                13011 -> handleTmcSegment(intent)
                10019 -> handleStateUpdate(intent)
                10002 -> handleNaviFinish()
                else -> {
                    Log.d(TAG, "收到其他高德廣播 KEY_TYPE: $keyType")
                }
            }
        }
    }

    /**
     * 除錯印出高德原始廣播 Extra 欄位
     */
    private fun dumpDebugExtras(intent: Intent, keyType: Int) {
        val extras = intent.extras ?: return
        val summary = StringBuilder()
        var count = 0
        for (k in extras.keySet()) {
            if (count++ > 6) break // 截取前幾項避免日誌過長
            summary.append("$k=${extras.get(k)} ")
        }
        NavStateRepository.appendNotificationLog("[高德收到] TYPE=$keyType: $summary")
        Log.d(TAG, "高德原始廣播 TYPE=$keyType: $summary")
    }

    /**
     * 處理 10001 導航與巡航核心資訊 (完全參照 zd423/AMap- 欄位映射)
     */
    private fun handleNaviInfo(intent: Intent) {
        // 轉向圖標：優先使用 NEW_ICON，其次使用 ICON
        var icon = getIntSafe(intent, "NEW_ICON", 0)
        if (icon == 0) {
            icon = getIntSafe(intent, "ICON", 0)
        }

        // 距離處理 (智慧支援字串 "500米" / "1.2公里" 與數字 Int)
        var segRemainDis = getStringSafe(intent, "SEG_REMAIN_DIS_AUTO")
        if (segRemainDis.isBlank()) {
            val distInt = getIntSafe(intent, "SEG_REMAIN_DIS", -1)
            segRemainDis = if (distInt >= 1000) {
                String.format("%.1f公里", distInt / 1000.0)
            } else if (distInt >= 0) {
                "${distInt}米"
            } else {
                "--"
            }
        }

        val routeRemainDis = getStringSafe(intent, "ROUTE_REMAIN_DIS_AUTO").ifBlank {
            val distInt = getIntSafe(intent, "ROUTE_REMAIN_DIS", -1)
            if (distInt >= 1000) String.format("%.1f公里", distInt / 1000.0) else if (distInt >= 0) "${distInt}米" else "--"
        }

        val routeRemainTime = getStringSafe(intent, "ROUTE_REMAIN_TIME_AUTO").ifBlank {
            val timeInt = getIntSafe(intent, "ROUTE_REMAIN_TIME", -1)
            if (timeInt > 0) {
                val mins = timeInt / 60
                if (mins >= 60) "${mins / 60}小時${mins % 60}分" else "${mins}分鐘"
            } else "--"
        }

        val etaText = getStringSafe(intent, "ETA_TEXT")
        var nextRoadName = getStringSafe(intent, "NEXT_ROAD_NAME")
        val curRoadName = getStringSafe(intent, "CUR_ROAD_NAME")
        if (nextRoadName.isBlank()) nextRoadName = curRoadName
        if (nextRoadName.isBlank()) nextRoadName = "直行前進"

        // 車速與測速相機
        val curSpeed = getIntSafe(intent, "CUR_SPEED", -1)
        val limitedSpeed = getIntSafe(intent, "LIMITED_SPEED", 0)
        val cameraDist = getIntSafe(intent, "CAMERA_DIST", 0)
        val cameraSpeed = getIntSafe(intent, "CAMERA_SPEED", 0)
        val endPoiName = getStringSafe(intent, "endPOIName")

        // 轉向列舉解析
        val maneuver = ManeuverType.fromAmapIconCode(icon)

        // 距離拆分格式化
        val formattedDist = if (segRemainDis.endsWith("米")) {
            segRemainDis.replace("米", "公尺")
        } else {
            segRemainDis
        }

        val currentNav = NavStateRepository.navState.value

        // 若廣播帶有車道線直接解析
        val driveWayStr = getStringSafe(intent, "EXTRA_DRIVE_WAY").ifBlank {
            getStringSafe(intent, "DRIVE_WAY")
        }
        val lanes = if (driveWayStr.isNotBlank()) parseDriveWayJson(driveWayStr) else currentNav.lanes

        // 整理行程摘要
        val summaryText = if (routeRemainDis != "--" || routeRemainTime != "--") {
            "$routeRemainDis · $routeRemainTime"
        } else currentNav.remainingDistance

        val updatedNav = currentNav.copy(
            maneuver = maneuver,
            distance = formattedDist,
            distanceMeters = parseDistanceMeters(formattedDist),
            roadName = nextRoadName,
            roadNumber = if (curRoadName.isNotBlank()) curRoadName else null,
            nextAction = maneuver.description,
            eta = if (etaText.isNotBlank()) etaText else currentNav.eta,
            remainingTime = routeRemainTime,
            remainingDistance = summaryText,
            currentSpeed = if (curSpeed >= 0) curSpeed else currentNav.currentSpeed,
            speedLimit = if (limitedSpeed > 0) limitedSpeed else cameraSpeed,
            cameraWarning = if (cameraSpeed > 0 || limitedSpeed > 0) "限速 ${if (cameraSpeed > 0) cameraSpeed else limitedSpeed}" else null,
            cameraDistance = if (cameraDist > 0) cameraDist else null,
            mapSource = "高德地圖",
            isNavigating = (icon != 0),
            lanes = lanes
        )

        NavStateRepository.updateNavInfo(updatedNav)
        NavStateRepository.appendNotificationLog("[高德導航] 動作:${maneuver.description} | 距:$formattedDist | 路:$nextRoadName")
    }

    /**
     * 處理 13012 官方車道線廣播 (EXTRA_DRIVE_WAY)
     */
    private fun handleDriveWay(intent: Intent) {
        val driveWayJson = getStringSafe(intent, "EXTRA_DRIVE_WAY")
        if (driveWayJson.isBlank()) return

        val lanes = parseDriveWayJson(driveWayJson)
        if (lanes.isNotEmpty()) {
            val current = NavStateRepository.navState.value
            NavStateRepository.updateNavInfo(current.copy(lanes = lanes))
            NavStateRepository.appendNotificationLog("[高德車道線] 已更新 ${lanes.size} 條車道導引")
        }
    }

    /**
     * 解析車道線 JSON 字串
     */
    private fun parseDriveWayJson(jsonStr: String): List<LaneItem> {
        try {
            val root = JSONObject(jsonStr)
            val enabled = root.optBoolean("drive_way_enabled", true)
            if (!enabled) return emptyList()

            val infoArray = root.optJSONArray("drive_way_info") ?: return emptyList()
            val list = mutableListOf<JSONObject>()
            for (i in 0 until infoArray.length()) {
                list.add(infoArray.getJSONObject(i))
            }

            // 按 drive_way_number 排序
            list.sortWith(Comparator { a, b ->
                a.optInt("drive_way_number", 0) - b.optInt("drive_way_number", 0)
            })

            val laneItems = mutableListOf<LaneItem>()
            for (obj in list) {
                val iconStr = obj.optString("drive_way_lane_Back_icon", "")
                val advised = obj.optBoolean("trafficLaneAdvised", false)
                        || "true".equals(obj.optString("trafficLaneAdvised"), ignoreCase = true)
                        || "1" == obj.optString("trafficLaneAdvised")

                val direction = mapLaneIconToDirection(iconStr)
                laneItems.add(LaneItem(direction = direction, isOptimal = advised))
            }
            return laneItems
        } catch (e: Exception) {
            Log.e(TAG, "解析高德車道線 JSON 失敗", e)
            return emptyList()
        }
    }

    /**
     * 將高德車道線 icon 代碼映射為標準方向 (LEFT/RIGHT/STRAIGHT/UTURN)
     */
    private fun mapLaneIconToDirection(iconStr: String): String {
        return when (iconStr) {
            "1" -> "STRAIGHT"
            "2" -> "LEFT"
            "3" -> "RIGHT"
            "4" -> "STRAIGHT_LEFT"
            "5" -> "STRAIGHT_RIGHT"
            "6" -> "LEFT_UTURN"
            "8" -> "UTURN"
            else -> "STRAIGHT"
        }
    }

    /**
     * 處理 60073 官方紅綠燈廣播 (trafficLightStatus, redLightCountDownSeconds, dir)
     */
    private fun handleTrafficLight(intent: Intent) {
        val status = getIntSafe(intent, "trafficLightStatus", 0)
        val countdown = getIntSafe(intent, "redLightCountDownSeconds", 0)

        // status: 1=紅燈, 4=綠燈, 其他=黃燈/等待
        val state = when (status) {
            1 -> TrafficLightState.RED
            4 -> TrafficLightState.GREEN
            2, 3 -> TrafficLightState.YELLOW
            else -> if (countdown > 0) TrafficLightState.RED else TrafficLightState.NONE
        }

        val current = NavStateRepository.navState.value
        NavStateRepository.updateNavInfo(
            current.copy(
                trafficLightState = state,
                trafficLightSeconds = if (countdown > 0) countdown else null
            )
        )
        NavStateRepository.appendNotificationLog("[高德紅綠燈] 燈號:$state | 倒數:${countdown}秒")
    }

    /**
     * 處理 13011 TMC 路況分段資料
     */
    private fun handleTmcSegment(intent: Intent) {
        val tmcJson = getStringSafe(intent, "EXTRA_TMC_SEGMENT")
        if (tmcJson.isBlank()) return
        try {
            val array = JSONArray(tmcJson)
            val segments = mutableListOf<TrafficSegment>()
            for (i in 0 until array.length()) {
                val seg = array.getJSONObject(i)
                val status = seg.optInt("status", 1)
                val percent = seg.optDouble("percent", 0.2).toFloat()
                val color = when (status) {
                    1 -> "#34C759" // 暢通 (綠)
                    2 -> "#FFCC00" // 緩行 (黃)
                    3 -> "#FF9500" // 擁堵 (橙)
                    4 -> "#FF3B30" // 嚴重擁堵 (紅)
                    else -> "#34C759"
                }
                segments.add(TrafficSegment(color, percent))
            }
            if (segments.isNotEmpty()) {
                val current = NavStateRepository.navState.value
                NavStateRepository.updateNavInfo(current.copy(trafficSegments = segments))
                NavStateRepository.appendNotificationLog("[高德TMC] 成功載入 ${segments.size} 段路況彩色分段")
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析高德 TMC 分段失敗", e)
        }
    }

    /**
     * 處理 10019 導航狀態切換
     */
    private fun handleStateUpdate(intent: Intent) {
        val extraState = getIntSafe(intent, "EXTRA_STATE", -1)
        when (extraState) {
            9 -> handleNaviFinish()
            25 -> {
                NavStateRepository.appendNotificationLog("[高德廣播] 巡航已結束")
            }
        }
    }

    /**
     * 處理 10002 導航結束
     */
    private fun handleNaviFinish() {
        NavStateRepository.appendNotificationLog("[高德廣播] 導航已正常結束")
        val currentNav = NavStateRepository.navState.value
        NavStateRepository.updateNavInfo(
            currentNav.copy(
                isNavigating = false,
                roadName = "導航已結束",
                distance = "--",
                trafficLightState = TrafficLightState.NONE,
                trafficLightSeconds = null
            )
        )
    }

    /**
     * 安全 Int 提取器 (支援 Number, String, Float 相容)
     */
    private fun getIntSafe(intent: Intent, key: String, defaultValue: Int): Int {
        val extras = intent.extras ?: return defaultValue
        if (!extras.containsKey(key)) return defaultValue
        val value = extras.get(key)
        return when (value) {
            is Number -> value.toInt()
            is String -> {
                value.toIntOrNull() ?: value.toFloatOrNull()?.toInt() ?: defaultValue
            }
            else -> defaultValue
        }
    }

    /**
     * 安全 String 提取器 (防 null 與全型空白相容)
     */
    private fun getStringSafe(intent: Intent, key: String): String {
        val extras = intent.extras ?: return ""
        val value = extras.get(key) ?: return ""
        return value.toString().trim()
    }

    /**
     * 解析距離字串為米數
     */
    private fun parseDistanceMeters(distStr: String): Int {
        return try {
            if (distStr.contains("公里")) {
                val num = distStr.replace("公里", "").trim().toDouble()
                (num * 1000).toInt()
            } else {
                distStr.replace("公尺", "").replace("米", "").trim().toInt()
            }
        } catch (e: Exception) {
            500
        }
    }
}
