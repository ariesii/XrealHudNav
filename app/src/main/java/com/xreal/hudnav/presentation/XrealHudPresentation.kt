package com.xreal.hudnav.presentation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.xreal.hudnav.R
import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max
import kotlin.math.min

/**
 * XREAL Air 2 Pro 眼鏡端專用 Presentation 渲染視窗
 * 具備 Smart Glance 自動淡出喚醒、轉向流光呼吸動態與時速測速預警
 */
class XrealHudPresentation(
    outerContext: Context,
    display: Display
) : Presentation(outerContext, display) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // UI 元件
    private lateinit var hudContainer: LinearLayout
    private lateinit var ivManeuver: ImageView
    private lateinit var tvDistance: TextView
    private lateinit var tvRoadNumber: TextView
    private lateinit var tvRoadName: TextView
    private lateinit var layoutNextAction: LinearLayout
    private lateinit var tvNextAction: TextView
    private lateinit var layoutTripSummary: LinearLayout
    private lateinit var tvTripSummary: TextView
    private lateinit var tvStandbyHint: TextView

    // 時速與測速照相
    private lateinit var layoutSpeedometer: LinearLayout
    private lateinit var tvSpeedValue: TextView
    private lateinit var tvSpeedUnit: TextView
    private lateinit var layoutCameraAlert: LinearLayout
    private lateinit var tvCameraTag: TextView
    private lateinit var tvCameraDistance: TextView
    private lateinit var tvMapSourceTag: TextView

    // 距離倒數進度光條
    private lateinit var layoutProgressTrack: View
    private lateinit var viewProgressBar: View

    // 轉向呼吸流光動畫
    private var arrowFlowAnimator: ValueAnimator? = null
    private var isGlanceHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            setBackgroundDrawableResource(R.color.hud_background)
        }

        setContentView(R.layout.presentation_xreal_hud)

        initViews()
        setupFlowAnimation()
        observeNavState()
    }

    private fun initViews() {
        hudContainer = findViewById(R.id.hudContainer)
        ivManeuver = findViewById(R.id.ivManeuver)
        tvDistance = findViewById(R.id.tvDistance)
        tvRoadNumber = findViewById(R.id.tvRoadNumber)
        tvRoadName = findViewById(R.id.tvRoadName)
        layoutNextAction = findViewById(R.id.layoutNextAction)
        tvNextAction = findViewById(R.id.tvNextAction)
        layoutTripSummary = findViewById(R.id.layoutTripSummary)
        tvTripSummary = findViewById(R.id.tvTripSummary)
        tvStandbyHint = findViewById(R.id.tvStandbyHint)

        layoutSpeedometer = findViewById(R.id.layoutSpeedometer)
        tvSpeedValue = findViewById(R.id.tvSpeedValue)
        tvSpeedUnit = findViewById(R.id.tvSpeedUnit)
        layoutCameraAlert = findViewById(R.id.layoutCameraAlert)
        tvCameraTag = findViewById(R.id.tvCameraTag)
        tvCameraDistance = findViewById(R.id.tvCameraDistance)
        tvMapSourceTag = findViewById(R.id.tvMapSourceTag)

        layoutProgressTrack = findViewById(R.id.layoutProgressTrack)
        viewProgressBar = findViewById(R.id.viewProgressBar)
    }

    private fun setupFlowAnimation() {
        // 轉向箭頭平滑呼吸流光動畫 (1.0 -> 0.4 -> 1.0)
        arrowFlowAnimator = ValueAnimator.ofFloat(1.0f, 0.45f, 1.0f).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val alphaVal = animator.animatedValue as Float
                ivManeuver.alpha = alphaVal
            }
        }
    }

    private fun observeNavState() {
        // 監聽導航狀態與各類數據更新
        scope.launch {
            NavStateRepository.navState.collectLatest { navInfo ->
                renderNavInfo(navInfo)
            }
        }

        // 監聽時速表開關
        scope.launch {
            NavStateRepository.isSpeedometerEnabled.collectLatest { enabled ->
                layoutSpeedometer.visibility = if (enabled) View.VISIBLE else View.GONE
            }
        }

        // 監聽位置與縮放微調
        scope.launch {
            NavStateRepository.hudOffsetX.collectLatest { x ->
                hudContainer.translationX = x
            }
        }
        scope.launch {
            NavStateRepository.hudOffsetY.collectLatest { y ->
                hudContainer.translationY = y
            }
        }
        scope.launch {
            NavStateRepository.hudScale.collectLatest { scale ->
                hudContainer.scaleX = scale
                hudContainer.scaleY = scale
            }
        }
    }

    private fun renderNavInfo(info: NavInfo) {
        if (!info.isNavigating) {
            hudContainer.visibility = View.GONE
            layoutSpeedometer.visibility = View.GONE
            tvStandbyHint.visibility = View.VISIBLE
            arrowFlowAnimator?.cancel()
            ivManeuver.alpha = 1.0f
            return
        }

        tvStandbyHint.visibility = View.GONE
        layoutSpeedometer.visibility = if (NavStateRepository.isSpeedometerEnabled.value) View.VISIBLE else View.GONE

        // 1. 處理 GPS 時速與超速顏色
        tvSpeedValue.text = info.currentSpeed.toString()
        if (info.isSpeeding()) {
            // 超速：高對比警告琥珀黃
            tvSpeedValue.setTextColor(ContextCompat.getColor(context, R.color.hud_accent_amber))
        } else {
            // 正常速度：霓虹青綠
            tvSpeedValue.setTextColor(ContextCompat.getColor(context, R.color.hud_accent_cyan))
        }

        // 2. 處理測速照相機警示 Chip
        if (!info.cameraWarning.isNullOrBlank()) {
            tvCameraTag.text = info.cameraWarning
            tvCameraDistance.text = "m"
            layoutCameraAlert.visibility = View.VISIBLE
        } else {
            layoutCameraAlert.visibility = View.GONE
        }

        // 3. 地圖來源標示
        tvMapSourceTag.text = info.mapSource

        // 4. Smart Glance 智能自動喚醒與淡出邏輯
        handleSmartGlance(info)

        // 5. 轉向向量圖標與動態呼吸流光
        ivManeuver.setImageResource(info.maneuver.iconResId)
        val isTurnManeuver = info.maneuver != ManeuverType.STRAIGHT && info.maneuver != ManeuverType.DESTINATION
        if (isTurnManeuver && info.distanceMeters <= 350) {
            if (arrowFlowAnimator?.isRunning != true) {
                arrowFlowAnimator?.start()
            }
        } else {
            arrowFlowAnimator?.cancel()
            ivManeuver.alpha = 1.0f
        }

        // 6. 轉彎距離與道路標示
        tvDistance.text = info.distance
        if (!info.roadNumber.isNullOrBlank()) {
            tvRoadNumber.text = info.roadNumber
            tvRoadNumber.visibility = View.VISIBLE
        } else {
            tvRoadNumber.visibility = View.GONE
        }
        tvRoadName.text = info.roadName

        // 7. 距離倒數進度光條（逼近路口 500m ➔ 0m）
        updateProgressBar(info.distanceMeters)

        // 8. 接下來轉向與總覽
        if (!info.nextAction.isNullOrBlank()) {
            tvNextAction.text = info.nextAction
            layoutNextAction.visibility = View.VISIBLE
        } else {
            layoutNextAction.visibility = View.GONE
        }

        val summaryText = buildString {
            if (info.remainingTime != "--") append(info.remainingTime)
            if (info.remainingDistance != "--") append(" · ").append(info.remainingDistance)
            if (info.eta != "--:--") append(" · ").append(info.eta)
        }
        if (summaryText.isNotBlank()) {
            tvTripSummary.text = summaryText
            layoutTripSummary.visibility = View.VISIBLE
        } else {
            layoutTripSummary.visibility = View.GONE
        }
    }

    /**
     * 處理 Smart Glance 智能淡出：長途直行自動淡出以維持 100% 透明視界，逼近路口自動淡入
     */
    private fun handleSmartGlance(info: NavInfo) {
        val smartGlanceOn = NavStateRepository.isSmartGlanceEnabled.value
        val threshold = NavStateRepository.smartGlanceThreshold.value

        hudContainer.visibility = View.VISIBLE

        if (smartGlanceOn) {
            // 若為長途直行且距離大於閾值
            val shouldHide = info.maneuver == ManeuverType.STRAIGHT && info.distanceMeters > threshold
            if (shouldHide && !isGlanceHidden) {
                isGlanceHidden = true
                ObjectAnimator.ofFloat(hudContainer, "alpha", hudContainer.alpha, 0.15f).apply {
                    duration = 500
                    start()
                }
            } else if (!shouldHide && isGlanceHidden) {
                isGlanceHidden = false
                ObjectAnimator.ofFloat(hudContainer, "alpha", hudContainer.alpha, 1.0f).apply {
                    duration = 400
                    start()
                }
            }
        } else {
            if (isGlanceHidden) {
                isGlanceHidden = false
                hudContainer.alpha = 1.0f
            }
        }
    }

    /**
     * 動態縮減 2dp 微光倒數進度光條
     */
    private fun updateProgressBar(distanceMeters: Int) {
        val totalTrackWidthPx = layoutProgressTrack.layoutParams.width.toFloat()
        val clampedMeters = min(max(distanceMeters, 0), 500)
        // 剩餘比例 (從 500m 到 0m)
        val ratio = clampedMeters / 500f
        val newWidth = (totalTrackWidthPx * ratio).toInt()

        val params = viewProgressBar.layoutParams
        params.width = max(newWidth, 6)
        viewProgressBar.layoutParams = params
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        arrowFlowAnimator?.cancel()
        scope.cancel()
    }
}
