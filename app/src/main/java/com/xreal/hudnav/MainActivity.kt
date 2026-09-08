package com.xreal.hudnav

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Display
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import com.xreal.hudnav.presentation.DisplayAssistant
import com.xreal.hudnav.service.NavDemoSimulator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * XREAL 導航 HUD 主控制器
 * 負責權限驗證、外接眼鏡生命週期管理、HUD 鏡像預覽與個人化偏好調整
 */
class MainActivity : AppCompatActivity(), DisplayAssistant.OnGlassesDisplayListener {

    private lateinit var displayAssistant: DisplayAssistant

    // UI 元件
    private lateinit var indicatorGlasses: View
    private lateinit var tvGlassesStatus: TextView
    private lateinit var tvPermissionStatus: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnToggleDemo: Button

    // 鏡像預覽元件
    private lateinit var previewHudContainer: LinearLayout
    private lateinit var previewIvManeuver: ImageView
    private lateinit var previewTvDistance: TextView
    private lateinit var previewTvRoadNumber: TextView
    private lateinit var previewTvRoadName: TextView
    private lateinit var previewLayoutNextAction: LinearLayout
    private lateinit var previewTvNextAction: TextView
    private lateinit var previewLayoutTripSummary: LinearLayout
    private lateinit var previewTvTripSummary: TextView
    private lateinit var previewTvStandbyHint: TextView

    // 微調控制元件
    private lateinit var sliderOffsetX: Slider
    private lateinit var sliderOffsetY: Slider
    private lateinit var sliderScale: Slider
    private lateinit var tvOffsetXLabel: TextView
    private lateinit var tvOffsetYLabel: TextView
    private lateinit var tvScaleLabel: TextView
    private lateinit var btnResetOffset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupDisplayAssistant()
        observeNavState()
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        displayAssistant.stopListening()
        NavDemoSimulator.stopSimulation()
    }

    private fun initViews() {
        indicatorGlasses = findViewById(R.id.indicatorGlasses)
        tvGlassesStatus = findViewById(R.id.tvGlassesStatus)
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnToggleDemo = findViewById(R.id.btnToggleDemo)

        // 預覽視窗
        val previewView = findViewById<View>(R.id.hudPreviewContent)
        previewHudContainer = previewView.findViewById(R.id.hudContainer)
        previewIvManeuver = previewView.findViewById(R.id.ivManeuver)
        previewTvDistance = previewView.findViewById(R.id.tvDistance)
        previewTvRoadNumber = previewView.findViewById(R.id.tvRoadNumber)
        previewTvRoadName = previewView.findViewById(R.id.tvRoadName)
        previewLayoutNextAction = previewView.findViewById(R.id.layoutNextAction)
        previewTvNextAction = previewView.findViewById(R.id.tvNextAction)
        previewLayoutTripSummary = previewView.findViewById(R.id.layoutTripSummary)
        previewTvTripSummary = previewView.findViewById(R.id.tvTripSummary)
        previewTvStandbyHint = previewView.findViewById(R.id.tvStandbyHint)

        // 微調
        sliderOffsetX = findViewById(R.id.sliderOffsetX)
        sliderOffsetY = findViewById(R.id.sliderOffsetY)
        sliderScale = findViewById(R.id.sliderScale)
        tvOffsetXLabel = findViewById(R.id.tvOffsetXLabel)
        tvOffsetYLabel = findViewById(R.id.tvOffsetYLabel)
        tvScaleLabel = findViewById(R.id.tvScaleLabel)
        btnResetOffset = findViewById(R.id.btnResetOffset)
    }

    private fun setupListeners() {
        btnGrantPermission.setOnClickListener {
            // 跳轉至系統通知存取權限設定頁面
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
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

        // 微調監聽
        val updateOffset = {
            val x = sliderOffsetX.value
            val y = sliderOffsetY.value
            val scale = sliderScale.value
            tvOffsetXLabel.text = "水平位置微調 (X Offset):  px"
            tvOffsetYLabel.text = "垂直高度微調 (Y Offset):  px"
            tvScaleLabel.text = "縮放比例 (Scale): x"
            NavStateRepository.updateHudOffset(x, y, scale)
        }

        sliderOffsetX.addOnChangeListener { _, _, _ -> updateOffset() }
        sliderOffsetY.addOnChangeListener { _, _, _ -> updateOffset() }
        sliderScale.addOnChangeListener { _, _, _ -> updateOffset() }

        btnResetOffset.setOnClickListener {
            sliderOffsetX.value = 0f
            sliderOffsetY.value = 0f
            sliderScale.value = 1.0f
            updateOffset()
        }
    }

    private fun setupDisplayAssistant() {
        displayAssistant = DisplayAssistant(this, this)
        displayAssistant.startListening()
    }

    private fun checkNotificationPermission() {
        val enabled = isNotificationServiceEnabled()
        if (enabled) {
            tvPermissionStatus.setText(R.string.permission_granted)
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.hud_accent_green))
            btnGrantPermission.visibility = View.GONE
        } else {
            tvPermissionStatus.setText(R.string.permission_required)
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.hud_accent_amber))
            btnGrantPermission.visibility = View.VISIBLE
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
        // 觀察導航狀態以更新鏡像預覽視窗
        lifecycleScope.launch {
            NavStateRepository.navState.collectLatest { info ->
                renderPreview(info)
            }
        }

        // 觀察位移微調以同步預覽視窗
        lifecycleScope.launch {
            NavStateRepository.hudOffsetX.collectLatest { x ->
                previewHudContainer.translationX = x * 0.4f // 縮放預覽微調比例
            }
        }
        lifecycleScope.launch {
            NavStateRepository.hudOffsetY.collectLatest { y ->
                previewHudContainer.translationY = y * 0.4f
            }
        }
        lifecycleScope.launch {
            NavStateRepository.hudScale.collectLatest { scale ->
                previewHudContainer.scaleX = scale
                previewHudContainer.scaleY = scale
            }
        }
    }

    private fun renderPreview(info: NavInfo) {
        if (!info.isNavigating) {
            previewHudContainer.visibility = View.GONE
            previewTvStandbyHint.visibility = View.VISIBLE
            return
        }

        previewTvStandbyHint.visibility = View.GONE
        previewHudContainer.visibility = View.VISIBLE

        previewIvManeuver.setImageResource(info.maneuver.iconResId)
        previewTvDistance.text = info.distance

        if (!info.roadNumber.isNullOrBlank()) {
            previewTvRoadNumber.text = info.roadNumber
            previewTvRoadNumber.visibility = View.VISIBLE
        } else {
            previewTvRoadNumber.visibility = View.GONE
        }

        previewTvRoadName.text = info.roadName

        if (!info.nextAction.isNullOrBlank()) {
            previewTvNextAction.text = info.nextAction
            previewLayoutNextAction.visibility = View.VISIBLE
        } else {
            previewLayoutNextAction.visibility = View.GONE
        }

        val summaryText = buildString {
            if (info.remainingTime != "--") append(info.remainingTime)
            if (info.remainingDistance != "--") append(" · ").append(info.remainingDistance)
            if (info.eta != "--:--") append(" · ").append(info.eta)
        }
        if (summaryText.isNotBlank()) {
            previewTvTripSummary.text = summaryText
            previewLayoutTripSummary.visibility = View.VISIBLE
        } else {
            previewLayoutTripSummary.visibility = View.GONE
        }
    }

    // --- OnGlassesDisplayListener ---
    override fun onGlassesConnected(display: Display) {
        runOnUiThread {
            indicatorGlasses.backgroundTintList = ContextCompat.getColorStateList(this, R.color.hud_accent_green)
            tvGlassesStatus.text = " ()"
        }
    }

    override fun onGlassesDisconnected() {
        runOnUiThread {
            indicatorGlasses.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_light)
            tvGlassesStatus.setText(R.string.glasses_disconnected)
        }
    }
}
