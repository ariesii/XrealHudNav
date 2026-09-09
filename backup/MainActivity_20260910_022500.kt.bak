package com.xreal.hudnav

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Display
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.xreal.hudnav.model.LaneItem
import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import com.xreal.hudnav.model.PowerMode
import com.xreal.hudnav.model.TrafficLightState
import com.xreal.hudnav.model.TrafficSegment
import com.xreal.hudnav.presentation.DisplayAssistant
import com.xreal.hudnav.service.NavDemoSimulator
import com.xreal.hudnav.speed.GpsSpeedManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 手機端主控制介面
 * 採用手機直向專屬 HUD 儀表 (Phone HUD Dashboard)，排版層次分明、絕不重疊！
 * 同時保持眼鏡端 (XREAL Air 2 Pro) 1920x1080 原版橫向 HUD 輸出，兩端各自最優呈現。
 */
class MainActivity : AppCompatActivity(), DisplayAssistant.OnGlassesDisplayListener {

    private lateinit var displayAssistant: DisplayAssistant
    private lateinit var gpsSpeedManager: GpsSpeedManager

    // 狀態與權限元件
    private lateinit var indicatorGlasses: View
    private lateinit var tvGlassesStatus: TextView
    private lateinit var tvBatteryStatus: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnGrantGps: Button
    private lateinit var btnGrantOverlay: Button
    private lateinit var btnToggleDemo: Button
    private lateinit var btnEnterBlackout: Button
    private lateinit var btnPingAmap: Button
    private lateinit var layoutBlackoutOverlay: FrameLayout
    private lateinit var tvNotificationLog: TextView

    // Inspector 即時數據看板元件
    private lateinit var tvInspectorStatusBadge: TextView
    private lateinit var tvInspectSource: TextView
    private lateinit var tvInspectSpeed: TextView
    private lateinit var tvInspectManeuver: TextView
    private lateinit var tvInspectDistance: TextView
    private lateinit var tvInspectRoad: TextView
    private lateinit var tvInspectTrafficLight: TextView
    private lateinit var tvInspectLanes: TextView

    // 一鍵快速測試注入按鈕
    private lateinit var btnInjectAmapRight: Button
    private lateinit var btnInjectGoogleLeft: Button
    private lateinit var btnInjectTrafficLight: Button

    // 全螢幕 HUD 元件
    private lateinit var btnFullscreenPreview: Button
    private lateinit var layoutFullscreenHud: FrameLayout
    private lateinit var btnCloseFullscreen: Button

    // 省電模式
    private lateinit var rgPowerMode: RadioGroup
    private lateinit var rbNormal: RadioButton
    private lateinit var rbEco: RadioButton
    private lateinit var rbUltraEco: RadioButton

    // 手機直向專屬 HUD 儀表 ViewHolder (分別用於主介面預覽與全螢幕模式)
    private lateinit var dashboardHolder: PhoneHudViewHolder
    private lateinit var fullscreenHolder: PhoneHudViewHolder

    private var batteryReceiver: BroadcastReceiver? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            checkGpsPermission()
            gpsSpeedManager.startTracking()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 啟用鎖屏穿透設定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_main)

        gpsSpeedManager = GpsSpeedManager(this)

        initViews()
        setupListeners()
        setupDisplayAssistant()
        setupBatteryMonitoring()
        observeNavState()
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
        checkGpsPermission()
        checkOverlayPermission()
        // 主動向高德地圖發送車載標準廣播握手請求 (KEY_TYPE=10013)
        com.xreal.hudnav.receiver.AmapBroadcastReceiver.requestAmapBroadcast(this)

        if (!isNotificationServiceEnabled()) {
            NavStateRepository.appendNotificationLog("⚠️ 提示：通知監聽服務未啟動，請點擊【通知授權】開啟權限")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        displayAssistant.stopListening()
        gpsSpeedManager.stopTracking()
        NavDemoSimulator.stopSimulation()
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
    }

    private fun initViews() {
        indicatorGlasses = findViewById(R.id.indicatorGlasses)
        tvGlassesStatus = findViewById(R.id.tvGlassesStatus)
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnGrantGps = findViewById(R.id.btnGrantGps)
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay)
        btnToggleDemo = findViewById(R.id.btnToggleDemo)
        btnEnterBlackout = findViewById(R.id.btnEnterBlackout)
        btnPingAmap = findViewById(R.id.btnPingAmap)
        layoutBlackoutOverlay = findViewById(R.id.layoutBlackoutOverlay)
        tvNotificationLog = findViewById(R.id.tvNotificationLog)

        // Inspector
        tvInspectorStatusBadge = findViewById(R.id.tvInspectorStatusBadge)
        tvInspectSource = findViewById(R.id.tvInspectSource)
        tvInspectSpeed = findViewById(R.id.tvInspectSpeed)
        tvInspectManeuver = findViewById(R.id.tvInspectManeuver)
        tvInspectDistance = findViewById(R.id.tvInspectDistance)
        tvInspectRoad = findViewById(R.id.tvInspectRoad)
        tvInspectTrafficLight = findViewById(R.id.tvInspectTrafficLight)
        tvInspectLanes = findViewById(R.id.tvInspectLanes)

        // 注入測試按鈕
        btnInjectAmapRight = findViewById(R.id.btnInjectAmapRight)
        btnInjectGoogleLeft = findViewById(R.id.btnInjectGoogleLeft)
        btnInjectTrafficLight = findViewById(R.id.btnInjectTrafficLight)

        // 全螢幕
        btnFullscreenPreview = findViewById(R.id.btnFullscreenPreview)
        layoutFullscreenHud = findViewById(R.id.layoutFullscreenHud)
        btnCloseFullscreen = findViewById(R.id.btnCloseFullscreen)

        rgPowerMode = findViewById(R.id.rgPowerMode)
        rbNormal = findViewById(R.id.rbNormal)
        rbEco = findViewById(R.id.rbEco)
        rbUltraEco = findViewById(R.id.rbUltraEco)

        // 綁定手機專屬直立 HUD 儀表 (卡片小窗與全螢幕滿版)
        dashboardHolder = PhoneHudViewHolder(findViewById(R.id.phoneDashboardContent))
        fullscreenHolder = PhoneHudViewHolder(findViewById(R.id.phoneFullscreenContent))
    }

    private fun setupListeners() {
        btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnGrantGps.setOnClickListener {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // 測試發送高德車載標準廣播握手請求
        btnPingAmap.setOnClickListener {
            com.xreal.hudnav.receiver.AmapBroadcastReceiver.requestAmapBroadcast(this)
            Toast.makeText(this, "已發送高德車載廣播連線請求", Toast.LENGTH_SHORT).show()
        }

        // 全螢幕 HUD 預覽切換 (直向專屬滿版駕駛模式，絕對不重疊)
        btnFullscreenPreview.setOnClickListener {
            layoutFullscreenHud.visibility = View.VISIBLE
            renderPhoneDashboard(fullscreenHolder, NavStateRepository.navState.value)
        }
        btnCloseFullscreen.setOnClickListener {
            layoutFullscreenHud.visibility = View.GONE
        }

        // 快速測試注入：高德右轉 48m
        btnInjectAmapRight.setOnClickListener {
            val testNav = NavInfo(
                maneuver = ManeuverType.TURN_RIGHT,
                distance = "48公尺",
                distanceMeters = 48,
                roadName = "民族路",
                nextAction = "右轉進入民族路",
                currentSpeed = 42,
                speedLimit = 50,
                trafficLightState = TrafficLightState.GREEN,
                trafficLightSeconds = 15,
                mapSource = "高德地圖",
                isNavigating = true,
                lanes = listOf(
                    LaneItem("STRAIGHT", false),
                    LaneItem("STRAIGHT", false),
                    LaneItem("RIGHT", true)
                ),
                trafficSegments = listOf(
                    TrafficSegment("#34C759", 0.7f),
                    TrafficSegment("#FFCC00", 0.3f)
                ),
                routeProgress = 0.4f
            )
            NavStateRepository.updateNavInfo(testNav)
            NavStateRepository.appendNotificationLog("[測試注入] 成功灌入: 高德地圖 48米 右轉 民族路")
            Toast.makeText(this, "已注入高德右轉 48m 測試數據", Toast.LENGTH_SHORT).show()
        }

        // 快速測試注入：Google 左轉 150m
        btnInjectGoogleLeft.setOnClickListener {
            val testNav = NavInfo(
                maneuver = ManeuverType.TURN_LEFT,
                distance = "150公尺",
                distanceMeters = 150,
                roadName = "忠孝東路四段",
                nextAction = "向左微轉",
                currentSpeed = 55,
                speedLimit = 50,
                trafficLightState = TrafficLightState.RED,
                trafficLightSeconds = 28,
                mapSource = "Google Maps",
                isNavigating = true,
                lanes = listOf(
                    LaneItem("LEFT", true),
                    LaneItem("STRAIGHT", false),
                    LaneItem("STRAIGHT", false)
                ),
                trafficSegments = listOf(
                    TrafficSegment("#34C759", 0.5f),
                    TrafficSegment("#FF3B30", 0.5f)
                ),
                routeProgress = 0.65f
            )
            NavStateRepository.updateNavInfo(testNav)
            NavStateRepository.appendNotificationLog("[測試注入] 成功灌入: Google Maps 150米 左轉 忠孝東路")
            Toast.makeText(this, "已注入 Google 左轉 150m 測試數據", Toast.LENGTH_SHORT).show()
        }

        // 快速測試注入：紅綠燈切換
        btnInjectTrafficLight.setOnClickListener {
            val current = NavStateRepository.navState.value
            val nextState = if (current.trafficLightState == TrafficLightState.RED) TrafficLightState.GREEN else TrafficLightState.RED
            val nextSec = if (nextState == TrafficLightState.RED) 28 else 12
            NavStateRepository.updateNavInfo(
                current.copy(
                    trafficLightState = nextState,
                    trafficLightSeconds = nextSec
                )
            )
            NavStateRepository.appendNotificationLog("[測試注入] 切換號誌: $nextState $nextSec 秒")
        }

        // 黑屏純省電防誤觸模式 (降低螢幕亮度至 0.01f，雙擊螢幕喚醒)
        btnEnterBlackout.setOnClickListener {
            layoutBlackoutOverlay.visibility = View.VISIBLE
            val lp = window.attributes
            lp.screenBrightness = 0.01f
            window.attributes = lp
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                layoutBlackoutOverlay.visibility = View.GONE
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
                return true
            }
        })
        layoutBlackoutOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // 省電模型切換
        rgPowerMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbEco -> PowerMode.ECO
                R.id.rbUltraEco -> PowerMode.ULTRA_ECO
                else -> PowerMode.NORMAL
            }
            NavStateRepository.setPowerMode(mode)
        }

        btnToggleDemo.setOnClickListener {
            if (NavDemoSimulator.isSimulating()) {
                NavDemoSimulator.stopSimulation()
                btnToggleDemo.setText(R.string.start_demo)
                btnToggleDemo.setBackgroundColor(ContextCompat.getColor(this, R.color.hud_accent_cyan))
            } else {
                NavDemoSimulator.startSimulation(lifecycleScope)
                btnToggleDemo.setText(R.string.stop_demo)
                btnToggleDemo.setBackgroundColor(ContextCompat.getColor(this, R.color.hud_accent_amber))
            }
        }
    }

    private fun setupBatteryMonitoring() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL

                    if (level >= 0 && scale > 0) {
                        val pct = (level * 100 / scale.toFloat()).toInt()
                        NavStateRepository.updateBattery(pct, isCharging)
                        val icon = if (isCharging) "⚡" else "🔋"
                        tvBatteryStatus.text = "$icon $pct%"
                    }
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun setupDisplayAssistant() {
        displayAssistant = DisplayAssistant(this, this)
        displayAssistant.startListening()
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                btnGrantOverlay.visibility = View.GONE
            } else {
                btnGrantOverlay.visibility = View.VISIBLE
            }
        }
    }

    private fun checkNotificationPermission() {
        if (isNotificationServiceEnabled()) {
            btnGrantPermission.visibility = View.GONE
        } else {
            btnGrantPermission.visibility = View.VISIBLE
        }
    }

    private fun checkGpsPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            btnGrantGps.visibility = View.GONE
            gpsSpeedManager.startTracking()
        } else {
            btnGrantGps.visibility = View.VISIBLE
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun observeNavState() {
        lifecycleScope.launch {
            NavStateRepository.navState.collectLatest { info ->
                renderPhoneDashboard(dashboardHolder, info)
                if (layoutFullscreenHud.visibility == View.VISIBLE) {
                    renderPhoneDashboard(fullscreenHolder, info)
                }
                renderInspector(info)
            }
        }
        lifecycleScope.launch {
            NavStateRepository.notificationLogs.collectLatest { logs ->
                if (logs.isNotEmpty()) {
                    tvNotificationLog.text = logs.takeLast(20).joinToString("\n")
                }
            }
        }
    }

    /**
     * 渲染手機端專屬直立 HUD 儀表 (絕不重疊)
     */
    private fun renderPhoneDashboard(holder: PhoneHudViewHolder, info: NavInfo) {
        // 1. 頂部狀態標籤
        holder.tvMapSource.text = if (info.isNavigating) "[${info.mapSource}] 導航中" else "[HUD 就緒] 待機中"
        holder.tvPowerMode.text = info.powerMode.label
        holder.tvBattery.text = "${if (info.isCharging) "⚡" else "🔋"} ${info.batteryLevel}%"

        // 2. 轉向箭頭 (優先使用 Google Maps / 高德地圖 原版截取點陣圖！)
        if (info.isNavigating) {
            holder.ivManeuver.alpha = 1.0f
            if (info.customIconBitmap != null) {
                holder.ivManeuver.setImageBitmap(info.customIconBitmap)
            } else {
                holder.ivManeuver.setImageResource(info.maneuver.iconResId)
            }
        } else {
            holder.ivManeuver.setImageResource(R.drawable.ic_arrow_straight)
            holder.ivManeuver.alpha = 0.35f
        }

        // 3. 距離與下一動作
        holder.tvDistance.text = if (info.isNavigating) info.distance else "--"
        holder.tvNextAction.text = if (info.isNavigating) {
            info.nextAction ?: info.maneuver.description
        } else {
            "等待導航指令"
        }

        // 4. 道路名稱
        if (info.isNavigating && !info.roadNumber.isNullOrBlank()) {
            holder.tvRoadNumber.visibility = View.VISIBLE
            holder.tvRoadNumber.text = info.roadNumber
        } else {
            holder.tvRoadNumber.visibility = View.GONE
        }
        holder.tvRoadName.text = if (info.isNavigating) info.roadName else "等待導航開始..."

        // 5. GPS 車速
        holder.tvSpeedValue.text = info.currentSpeed.toString()
        if (info.isSpeeding()) {
            holder.tvSpeedValue.setTextColor(ContextCompat.getColor(this, R.color.hud_accent_amber))
        } else {
            holder.tvSpeedValue.setTextColor(Color.parseColor("#69F0AE"))
        }

        // 6. 測速照相
        if (!info.cameraWarning.isNullOrBlank()) {
            holder.layoutCameraAlert.visibility = View.VISIBLE
            holder.tvCameraTag.text = info.cameraWarning
            holder.tvCameraDist.text = "${info.cameraDistance ?: 0}m"
        } else {
            holder.layoutCameraAlert.visibility = View.GONE
        }

        // 7. 車道線 (動態徽章，推薦車道青色高亮)
        if (info.hasLanes()) {
            holder.layoutLanes.visibility = View.VISIBLE
            holder.layoutLanes.removeAllViews()
            val density = resources.displayMetrics.density
            val sizePx = (30 * density).toInt()
            val marginPx = (5 * density).toInt()

            for (lane in info.lanes) {
                val tv = TextView(this).apply {
                    val symbol = when (lane.iconType) {
                        "LEFT" -> "↰"
                        "RIGHT" -> "↱"
                        "UTURN" -> "↶"
                        "STRAIGHT_RIGHT" -> "↑↱"
                        "STRAIGHT_LEFT" -> "↰↑"
                        else -> "↑"
                    }
                    text = symbol
                    textSize = 15f
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                        marginEnd = marginPx
                    }
                    setBackgroundResource(R.drawable.bg_hud_badge)
                    if (lane.isRecommended) {
                        background?.mutate()?.setTint(Color.parseColor("#00E5FF"))
                        setTextColor(Color.parseColor("#000000"))
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    } else {
                        background?.mutate()?.setTint(Color.parseColor("#22FFFFFF"))
                        setTextColor(Color.parseColor("#88FFFFFF"))
                    }
                }
                holder.layoutLanes.addView(tv)
            }
        } else {
            holder.layoutLanes.visibility = View.GONE
        }

        // 8. 紅綠燈倒數
        if (info.hasTrafficLight()) {
            holder.layoutTrafficLight.visibility = View.VISIBLE
            holder.tvTrafficLightSeconds.text = "${info.trafficLightSeconds}s"
            val color = Color.parseColor(info.trafficLightState.colorHex)
            holder.tvTrafficLightSeconds.setTextColor(color)
            holder.viewTrafficLightDot.background.setTint(color)
            holder.tvTrafficLightLabel.text = when (info.trafficLightState) {
                TrafficLightState.RED -> "紅燈等待"
                TrafficLightState.GREEN -> "綠燈通行"
                TrafficLightState.YELLOW -> "注意減速"
                else -> ""
            }
        } else {
            holder.layoutTrafficLight.visibility = View.GONE
        }

        // 9. 行程摘要
        if (info.remainingTime != "--" || info.remainingDistance != "--") {
            holder.tvTripSummary.visibility = View.VISIBLE
            holder.tvTripSummary.text = "剩餘 ${info.remainingTime} · ${info.remainingDistance}"
        } else {
            holder.tvTripSummary.visibility = View.GONE
        }
    }

    /**
     * 更新 Inspector 即時數據看板
     */
    private fun renderInspector(info: NavInfo) {
        if (info.isNavigating) {
            tvInspectorStatusBadge.text = "導航中"
            tvInspectorStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00C853"))
        } else {
            tvInspectorStatusBadge.text = "待機中"
            tvInspectorStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#455A64"))
        }

        tvInspectSource.text = if (info.isNavigating) info.mapSource else "尚未連線"
        tvInspectSpeed.text = "${info.currentSpeed} km/h"
        tvInspectManeuver.text = "${info.maneuver.description} (${info.maneuver.name})"
        tvInspectDistance.text = if (info.isNavigating) "${info.distance} (${info.distanceMeters}m)" else "--"
        tvInspectRoad.text = info.getFullRoadDisplay()

        if (info.hasTrafficLight()) {
            val stateText = when (info.trafficLightState) {
                TrafficLightState.RED -> "🔴 紅燈"
                TrafficLightState.GREEN -> "🟢 綠燈"
                TrafficLightState.YELLOW -> "🟡 黃燈"
                else -> "無"
            }
            tvInspectTrafficLight.text = "$stateText ${info.trafficLightSeconds ?: 0}s"
            tvInspectTrafficLight.setTextColor(Color.parseColor(info.trafficLightState.colorHex))
        } else {
            tvInspectTrafficLight.text = "無號誌"
            tvInspectTrafficLight.setTextColor(Color.parseColor("#90A4AE"))
        }

        if (info.hasLanes()) {
            val laneDesc = info.lanes.joinToString(" | ") { lane ->
                val dir = when (lane.iconType) {
                    "LEFT" -> "左"
                    "RIGHT" -> "右"
                    "UTURN" -> "迴"
                    else -> "直"
                }
                if (lane.isRecommended) "[$dir★]" else dir
            }
            tvInspectLanes.text = "${info.lanes.size}車道: $laneDesc"
            tvInspectLanes.setTextColor(Color.parseColor("#00E5FF"))
        } else {
            tvInspectLanes.text = "未提供"
            tvInspectLanes.setTextColor(Color.parseColor("#90A4AE"))
        }
    }

    override fun onBackPressed() {
        if (layoutFullscreenHud.visibility == View.VISIBLE) {
            layoutFullscreenHud.visibility = View.GONE
            return
        }
        super.onBackPressed()
    }

    override fun onGlassesConnected(display: Display) {
        runOnUiThread {
            indicatorGlasses.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#00E676"))
            tvGlassesStatus.setText(R.string.glasses_connected)
        }
    }

    override fun onGlassesDisconnected() {
        runOnUiThread {
            indicatorGlasses.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5252"))
            tvGlassesStatus.setText(R.string.glasses_disconnected)
        }
    }

    /**
     * 手機直向 HUD 儀表 ViewHolder
     */
    class PhoneHudViewHolder(val root: View) {
        val tvMapSource: TextView = root.findViewById(R.id.phoneTvMapSource)
        val tvPowerMode: TextView = root.findViewById(R.id.phoneTvPowerMode)
        val tvBattery: TextView = root.findViewById(R.id.phoneTvBattery)
        val ivManeuver: ImageView = root.findViewById(R.id.phoneIvManeuver)
        val tvDistance: TextView = root.findViewById(R.id.phoneTvDistance)
        val tvNextAction: TextView = root.findViewById(R.id.phoneTvNextAction)
        val tvRoadNumber: TextView = root.findViewById(R.id.phoneTvRoadNumber)
        val tvRoadName: TextView = root.findViewById(R.id.phoneTvRoadName)
        val tvSpeedValue: TextView = root.findViewById(R.id.phoneTvSpeedValue)
        val tvSpeedUnit: TextView = root.findViewById(R.id.phoneTvSpeedUnit)
        val layoutCameraAlert: View = root.findViewById(R.id.phoneLayoutCameraAlert)
        val tvCameraTag: TextView = root.findViewById(R.id.phoneTvCameraTag)
        val tvCameraDist: TextView = root.findViewById(R.id.phoneTvCameraDist)
        val layoutLanes: LinearLayout = root.findViewById(R.id.phoneLayoutLanes)
        val layoutTrafficLight: View = root.findViewById(R.id.phoneLayoutTrafficLight)
        val viewTrafficLightDot: View = root.findViewById(R.id.phoneViewTrafficLightDot)
        val tvTrafficLightSeconds: TextView = root.findViewById(R.id.phoneTvTrafficLightSeconds)
        val tvTrafficLightLabel: TextView = root.findViewById(R.id.phoneTvTrafficLightLabel)
        val tvTripSummary: TextView = root.findViewById(R.id.phoneTvTripSummary)
    }
}
