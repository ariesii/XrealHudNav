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
 * 雙地圖即時導航通知監聽服務
 * 同時支援 Google Maps 與高德地圖（AutoNavi / Amap）
 */
class MapsNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MultiMapNotification"
        
        // 支援的地圖應用套件名稱
        private const val PKG_GOOGLE_MAPS = "com.google.android.apps.maps"
        private const val PKG_AMAP = "com.autonavi.minimap"
        private const val PKG_AMAP_AUTO = "com.autonavi.amapauto"

        // 正則表達式：擷取距離（相容繁體「公尺/公里」、簡體「米/公里」、英文「m/km」）
        private val DISTANCE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?\\s*(公尺|公里|米|m|km))", Pattern.CASE_INSENSITIVE)
        
        // 正則表達式：擷取道路編號（例如 106、台64、G15、S20）
        private val ROAD_NUM_PATTERN = Pattern.compile("(\\b[A-Za-z]?\\d{1,4}\\b|台\\d+|國道\\d+|市道\\d+|省道\\d+)")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg != PKG_GOOGLE_MAPS && pkg != PKG_AMAP && pkg != PKG_AMAP_AUTO) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val mapSourceName = when (pkg) {
            PKG_GOOGLE_MAPS -> "Google Maps"
            PKG_AMAP -> "高德地圖"
            PKG_AMAP_AUTO -> "高德車機"
            else -> "導航"
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()

        Log.d(TAG, "[] 攔截通知 -> Title: [], Text: [], SubText: []")

        if (title.isBlank() && text.isBlank()) return

        parseAndPostNavData(mapSourceName, title, text, subText)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName
        if (pkg == PKG_GOOGLE_MAPS || pkg == PKG_AMAP || pkg == PKG_AMAP_AUTO) {
            Log.d(TAG, "導航通知已結束: ")
            NavStateRepository.updateNavInfo(
                NavInfo(
                    isNavigating = false,
                    roadName = "導航已結束"
                )
            )
        }
    }

    private fun parseAndPostNavData(source: String, title: String, text: String, subText: String) {
        val combinedActionText = "  "
        val maneuver = ManeuverType.fromText(combinedActionText)

        // 擷取距離字串與公尺數值
        var distanceStr = ""
        var distanceMeters = 1000
        val distMatcher = DISTANCE_PATTERN.matcher(title.ifBlank { text })
        if (distMatcher.find()) {
            distanceStr = distMatcher.group(1).orEmpty()
            distanceMeters = parseDistanceToMeters(distanceStr)
        }

        // 擷取道路編號
        var roadNum: String? = null
        val numMatcher = ROAD_NUM_PATTERN.matcher(title)
        if (numMatcher.find()) {
            roadNum = numMatcher.group(1)
        }

        // 清理標題取得純道路名稱
        var cleanRoadName = title
            .replace("前往", "")
            .replace("進入", "")
            .replace("直行", "")
            .replace("左轉", "")
            .replace("右轉", "")
            .replace("沿", "")
            .replace("行駛", "")
            .replace(distanceStr, "")
            .trim()

        if (roadNum != null) {
            cleanRoadName = cleanRoadName.replace(roadNum, "").trim()
        }
        if (cleanRoadName.isBlank()) {
            cleanRoadName = if (roadNum != null) " 道路" else title
        }

        // 解析剩餘行程
        var remainingTime = "--"
        var remainingDistance = "--"
        var eta = "--:--"

        if (text.isNotBlank()) {
            val parts = text.split("·", "-", "•", ",").map { it.trim() }
            if (parts.isNotEmpty()) remainingTime = parts[0]
            if (parts.size > 1) remainingDistance = parts[1]
            if (parts.size > 2) eta = parts[2]
        }

        // 保持現有 GPS 車速
        val currentSpeed = NavStateRepository.navState.value.currentSpeed
        val cameraWarning = NavStateRepository.navState.value.cameraWarning
        val cameraDistance = NavStateRepository.navState.value.cameraDistance

        val navInfo = NavInfo(
            maneuver = maneuver,
            distance = if (distanceStr.isNotBlank()) distanceStr else "直行",
            distanceMeters = distanceMeters,
            roadNumber = roadNum,
            roadName = cleanRoadName,
            nextAction = if (subText.isNotBlank()) subText else null,
            eta = eta,
            remainingTime = remainingTime,
            remainingDistance = remainingDistance,
            currentSpeed = currentSpeed,
            cameraWarning = cameraWarning,
            cameraDistance = cameraDistance,
            mapSource = source,
            isNavigating = true
        )

        NavStateRepository.updateNavInfo(navInfo)
    }

    /**
     * 將字串距離換算為數值公尺
     */
    private fun parseDistanceToMeters(distStr: String): Int {
        return try {
            val clean = distStr.lowercase().replace(" ", "")
            when {
                clean.contains("公里") || clean.contains("km") -> {
                    val num = clean.replace("公里", "").replace("km", "").toDouble()
                    (num * 1000).toInt()
                }
                clean.contains("公尺") || clean.contains("米") || clean.contains("m") -> {
                    clean.replace("公尺", "").replace("米", "").replace("m", "").toInt()
                }
                else -> 500
            }
        } catch (e: Exception) {
            500
        }
    }
}
