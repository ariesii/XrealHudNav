package com.xreal.hudnav.presentation

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.xreal.hudnav.R
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.NavStateRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * XREAL Air 2 Pro 眼鏡端專用 Presentation 渲染視窗
 *
 * @param outerContext 應用程式 Context
 * @param display XREAL 眼鏡對應的外接 Display 物件
 */
class XrealHudPresentation(
    outerContext: Context,
    display: Display
) : Presentation(outerContext, display) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 設定全螢幕、保持螢幕常亮、背景透明全黑
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            setBackgroundDrawableResource(R.color.hud_background)
        }

        setContentView(R.layout.presentation_xreal_hud)

        initViews()
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
    }

    private fun observeNavState() {
        // 監聽導航狀態更新
        scope.launch {
            NavStateRepository.navState.collectLatest { navInfo ->
                renderNavInfo(navInfo)
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

    /**
     * 渲染導航即時資料至眼鏡 HUD
     */
    private fun renderNavInfo(info: NavInfo) {
        if (!info.isNavigating) {
            // 待機狀態：縮減視覺元素，僅顯示微弱提示
            hudContainer.visibility = View.GONE
            tvStandbyHint.visibility = View.VISIBLE
            return
        }

        tvStandbyHint.visibility = View.GONE
        hudContainer.visibility = View.VISIBLE

        // 1. 轉向箭頭
        ivManeuver.setImageResource(info.maneuver.iconResId)

        // 2. 距離指示
        tvDistance.text = info.distance

        // 3. 道路編號標籤
        if (!info.roadNumber.isNullOrBlank()) {
            tvRoadNumber.text = info.roadNumber
            tvRoadNumber.visibility = View.VISIBLE
        } else {
            tvRoadNumber.visibility = View.GONE
        }

        // 4. 道路名稱
        tvRoadName.text = info.roadName

        // 5. 接下來轉向動作
        if (!info.nextAction.isNullOrBlank()) {
            tvNextAction.text = info.nextAction
            layoutNextAction.visibility = View.VISIBLE
        } else {
            layoutNextAction.visibility = View.GONE
        }

        // 6. 總覽資訊 (時間 · 里程 · ETA)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
