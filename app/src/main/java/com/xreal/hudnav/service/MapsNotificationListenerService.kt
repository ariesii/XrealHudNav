package com.xreal.hudnav.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import com.xreal.hudnav.model.TrafficLightState
import java.util.regex.Pattern

/**
 * 雙地圖即時導航通知監聽服務
 * 整合 Google Maps 通知、高德地圖通知、高德車載標準廣播與 LargeIcon 視覺分析
 */
class MapsNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MultiMapNotification"
        private const val PKG_GOOGLE_MAPS = "com.google.android.apps.maps"
        private const val PKG_AMAP = "com.autonavi.minimap"
        private const val PKG_AMAP_AUTO = "com.autonavi.amapauto"

        // 高德車載標準導航廣播 Action
        private const val ACTION_AMAP_BROADCAST = "AUTONAVI_STANDARD_BROADCAST_SEND"

        private val DISTANCE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?\\s*(公尺|公里|米|m|km))", Pattern.CASE_INSENSITIVE)
        private val ROAD_NUM_PATTERN = Pattern.compile("(\\b[A-Za-z]?\\d{1,4}\\b|台\\d+|國道\\d+|市道\\d+|省道\\d+)")
        
        // 紅綠燈即時秒數正則 (如 "紅燈 24秒"、"綠燈等待 15s")
        private val TRAFFIC_LIGHT_PATTERN = Pattern.compile("(紅燈|綠燈|黃燈|等待)\\D*?(\\d{1,3})\\s*(秒|s)?", Pattern.CASE_INSENSITIVE)
    }

    private var amapReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        registerAmapBroadcastReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterAmapBroadcastReceiver()
    }

    /**
     * 註冊高德地圖標準導航廣播監聽器
     */
    private fun registerAmapBroadcastReceiver() {
        amapReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_AMAP_BROADCAST) {
                    val keyType = intent.getIntExtra("KEY_TYPE", 0)
                    if (keyType == 10001) { // 導航資訊更新
                        val iconCode = intent.getIntExtra("EXTRA_MNAEUVER_ID", intent.getIntExtra("EXTRA_ICON", 0))
                        val nextRoad = intent.getStringExtra("EXTRA_NEXT_ROAD_NAME").orEmpty()
                        val remainDis = intent.getIntExtra("EXTRA_SEG_REMAIN_DIS", 0)

                        Log.i(TAG, "收到高德車載廣播 -> 動作代碼: $iconCode, 下一路口: $nextRoad, 剩餘公尺: $remainDis")

                        if (iconCode > 0 || nextRoad.isNotBlank()) {
                            val maneuver = ManeuverType.fromAmapIconCode(iconCode)
                            val current = NavStateRepository.navState.value
                            NavStateRepository.updateNavInfo(
                                current.copy(
                                    maneuver = maneuver,
                                    distance = if (remainDis > 0) "${remainDis}m" else current.distance,
                                    distanceMeters = if (remainDis > 0) remainDis else current.distanceMeters,
                                    roadName = if (nextRoad.isNotBlank()) nextRoad else current.roadName,
                                    mapSource = "高德地圖",
                                    isNavigating = true
                                )
                            )
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(ACTION_AMAP_BROADCAST)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(amapReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(amapReceiver, filter)
        }
    }

    private fun unregisterAmapBroadcastReceiver() {
        amapReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
            amapReceiver = null
        }
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
        val ticker = notification.tickerText?.toString().orEmpty()

        Log.d(TAG, "[$mapSourceName] 通知內容 -> Title: [$title], Text: [$text], SubText: [$subText], Ticker: [$ticker]")

        if (title.isBlank() && text.isBlank()) return

        parseAndPostNavData(mapSourceName, title, text, subText, ticker, notification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName
        if (pkg == PKG_GOOGLE_MAPS || pkg == PKG_AMAP || pkg == PKG_AMAP_AUTO) {
            NavStateRepository.updateNavInfo(
                NavStateRepository.navState.value.copy(
                    isNavigating = false,
                    roadName = "導航已結束"
                )
            )
        }
    }

    private fun parseAndPostNavData(
        source: String,
        title: String,
        text: String,
        subText: String,
        ticker: String,
        notification: Notification
    ) {
        val fullContent = "$title $subText $text $ticker"

        // 1. 推斷轉向動作（結合文字分析與 LargeIcon 特徵分析）
        var maneuver = ManeuverType.fromText(fullContent)

        // 如果文字判斷為直行或未知，但這是高德地圖，嘗試透過 Notification 圖標視覺分析左右轉
        if (maneuver == ManeuverType.STRAIGHT || maneuver == ManeuverType.UNKNOWN) {
            val iconManeuver = analyzeNotificationIcon(notification)
            if (iconManeuver != null) {
                maneuver = iconManeuver
                Log.i(TAG, "透過 Notification 圖標成功識別轉向: ${maneuver.description}")
            }
        }

        // 2. 擷取距離字串與公尺
        var distanceStr = ""
        var distanceMeters = 1000
        val distMatcher = DISTANCE_PATTERN.matcher(title.ifBlank { text })
        if (distMatcher.find()) {
            distanceStr = distMatcher.group(1).orEmpty()
            distanceMeters = parseDistanceToMeters(distanceStr)
        }

        // 3. 擷取道路編號與名稱
        var roadNum: String? = null
        val numMatcher = ROAD_NUM_PATTERN.matcher(title)
        if (numMatcher.find()) {
            roadNum = numMatcher.group(1)
        }

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
            cleanRoadName = if (roadNum != null) "$roadNum 道路" else title
        }

        // 4. 解析紅綠燈秒數資訊 (例如 "紅燈 24秒" / "綠燈 12s")
        var trafficLightState = TrafficLightState.NONE
        var trafficLightSeconds: Int? = null

        val tlMatcher = TRAFFIC_LIGHT_PATTERN.matcher(fullContent)
        if (tlMatcher.find()) {
            val stateStr = tlMatcher.group(1).orEmpty()
            val secStr = tlMatcher.group(2).orEmpty()
            val secs = secStr.toIntOrNull()
            if (secs != null && secs > 0) {
                trafficLightSeconds = secs
                trafficLightState = when {
                    stateStr.contains("紅") || stateStr.contains("等待") -> TrafficLightState.RED
                    stateStr.contains("綠") -> TrafficLightState.GREEN
                    stateStr.contains("黃") -> TrafficLightState.YELLOW
                    else -> TrafficLightState.RED
                }
                Log.i(TAG, "偵測到路口智慧紅綠燈 -> 狀態: ${trafficLightState.label}, 秒數: ${secs}s")
            }
        }

        // 5. 解析剩餘行程資訊
        var remainingTime = "--"
        var remainingDistance = "--"
        var eta = "--:--"

        if (text.isNotBlank()) {
            val parts = text.split("·", "-", "•", ",").map { it.trim() }
            if (parts.isNotEmpty()) remainingTime = parts[0]
            if (parts.size > 1) remainingDistance = parts[1]
            if (parts.size > 2) eta = parts[2]
        }

        val currentRepo = NavStateRepository.navState.value

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
            currentSpeed = currentRepo.currentSpeed,
            speedLimit = currentRepo.speedLimit,
            cameraWarning = currentRepo.cameraWarning,
            cameraDistance = currentRepo.cameraDistance,
            trafficLightState = trafficLightState,
            trafficLightSeconds = trafficLightSeconds,
            mapSource = source,
            isNavigating = true
        )

        NavStateRepository.updateNavInfo(navInfo)
    }

    /**
     * 從 Notification 圖標提取像素重心以判定左轉/右轉
     */
    private fun analyzeNotificationIcon(notification: Notification): ManeuverType? {
        return try {
            val icon = notification.getLargeIcon() ?: return null
            val drawable = icon.loadDrawable(this) ?: return null
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, 32, 32)
                drawable.draw(canvas)
                bmp
            }

            // 分析 32x32 左右兩半部的亮色像素密度
            val width = bitmap.width
            val height = bitmap.height
            var leftLum = 0L
            var rightLum = 0L

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xFF
                    val red = (pixel shr 16) and 0xFF
                    val green = (pixel shr 8) and 0xFF
                    val blue = pixel and 0xFF
                    val lum = (red + green + blue) * alpha / 255

                    if (x < width / 2) {
                        leftLum += lum
                    } else if (x > width / 2) {
                        rightLum += lum
                    }
                }
            }

            // 若右側像素明顯多於左側 (1.4 倍以上) 判定為右轉，反之為左轉
            when {
                rightLum > leftLum * 1.4 -> ManeuverType.TURN_RIGHT
                leftLum > rightLum * 1.4 -> ManeuverType.TURN_LEFT
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

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
