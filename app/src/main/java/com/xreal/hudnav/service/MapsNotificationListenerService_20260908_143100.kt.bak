package com.xreal.hudnav.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import java.util.regex.Pattern

/**
 * Google Maps 即時導航通知監聽服務
 * 透過 Android 系統 NotificationListenerService 即時攔截並解析導航指示
 */
class MapsNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MapsNotification"
        private const val MAPS_PACKAGE = "com.google.android.apps.maps"
        
        // 正則表達式：擷取距離（例如 "100 公尺"、"1.5 公里"、"200 m"）
        private val DISTANCE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?\\s*(公尺|公里|m|km))", Pattern.CASE_INSENSITIVE)
        
        // 正則表達式：擷取道路編號（例如 106縣道、台64線、國道一號）
        private val ROAD_NUM_PATTERN = Pattern.compile("(\\b\\d{1,3}\\b|台\\d+|國道\\d+|市道\\d+)")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName != MAPS_PACKAGE) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 提取主要與次要文字
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        Log.d(TAG, "攔截到 Google Maps 通知 -> Title: [], Text: [], SubText: []")

        if (title.isBlank() && text.isBlank()) return

        // 解析導航內容
        parseAndPostNavData(title, text, subText)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == MAPS_PACKAGE) {
            Log.d(TAG, "Google Maps 導航通知已結束")
            NavStateRepository.updateNavInfo(
                NavInfo(
                    isNavigating = false,
                    roadName = "導航已結束"
                )
            )
        }
    }

    /**
     * 解析 Google Maps 標題與內文字串，抽取 HUD 核心資訊
     */
    private fun parseAndPostNavData(title: String, text: String, subText: String) {
        // 1. 推斷轉向動作
        val combinedActionText = " "
        val maneuver = ManeuverType.fromText(combinedActionText)

        // 2. 擷取距離
        var distanceStr = ""
        val distMatcher = DISTANCE_PATTERN.matcher(title)
        if (distMatcher.find()) {
            distanceStr = distMatcher.group(1).orEmpty()
        }

        // 3. 擷取道路編號與主要道路名稱
        var roadNum: String? = null
        val numMatcher = ROAD_NUM_PATTERN.matcher(title)
        if (numMatcher.find()) {
            roadNum = numMatcher.group(1)
        }

        // 清理標題取得道路名稱（去除 "前往"、"直行"、距離等字眼）
        var cleanRoadName = title
            .replace("前往", "")
            .replace("直行", "")
            .replace("左轉", "")
            .replace("右轉", "")
            .replace(distanceStr, "")
            .trim()
            
        if (roadNum != null) {
            cleanRoadName = cleanRoadName.replace(roadNum, "").trim()
        }
        if (cleanRoadName.isBlank()) {
            cleanRoadName = if (roadNum != null) " 道路" else title
        }

        // 4. 解析行車時間與總剩餘里程（通常位於 text 中，例如 "23 分鐘 · 11 公里 · 上午10:12"）
        var remainingTime = "--"
        var remainingDistance = "--"
        var eta = "--:--"

        if (text.isNotBlank()) {
            val parts = text.split("·", "-", "•").map { it.trim() }
            if (parts.isNotEmpty()) remainingTime = parts[0]
            if (parts.size > 1) remainingDistance = parts[1]
            if (parts.size > 2) eta = parts[2]
        }

        val navInfo = NavInfo(
            maneuver = maneuver,
            distance = if (distanceStr.isNotBlank()) distanceStr else "直行",
            roadNumber = roadNum,
            roadName = cleanRoadName,
            nextAction = if (subText.isNotBlank()) subText else null,
            eta = eta,
            remainingTime = remainingTime,
            remainingDistance = remainingDistance,
            isNavigating = true
        )

        NavStateRepository.updateNavInfo(navInfo)
    }
}
