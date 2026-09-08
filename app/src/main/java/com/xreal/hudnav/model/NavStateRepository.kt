package com.xreal.hudnav.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 導航即時狀態管理儲存庫（單例模式）
 * 供 NotificationListenerService、GPS 時速、測速相機與 Presentation 共享狀態
 */
object NavStateRepository {
    private val _navState = MutableStateFlow(NavInfo())
    val navState: StateFlow<NavInfo> = _navState.asStateFlow()

    // HUD 位置與視覺偏移量偏好
    private val _hudOffsetX = MutableStateFlow(0f)
    val hudOffsetX: StateFlow<Float> = _hudOffsetX.asStateFlow()

    private val _hudOffsetY = MutableStateFlow(0f)
    val hudOffsetY: StateFlow<Float> = _hudOffsetY.asStateFlow()

    private val _hudScale = MutableStateFlow(1.0f)
    val hudScale: StateFlow<Float> = _hudScale.asStateFlow()

    // 智能自動喚醒 (Smart Glance) 設定
    private val _isSmartGlanceEnabled = MutableStateFlow(true)
    val isSmartGlanceEnabled: StateFlow<Boolean> = _isSmartGlanceEnabled.asStateFlow()

    // 喚醒距離閾值（公尺，預設 300 公尺）
    private val _smartGlanceThreshold = MutableStateFlow(300)
    val smartGlanceThreshold: StateFlow<Int> = _smartGlanceThreshold.asStateFlow()

    // 即時車速顯示開關
    private val _isSpeedometerEnabled = MutableStateFlow(true)
    val isSpeedometerEnabled: StateFlow<Boolean> = _isSpeedometerEnabled.asStateFlow()

    /**
     * 更新導航資料狀態
     */
    fun updateNavInfo(info: NavInfo) {
        _navState.value = info
    }

    /**
     * 僅更新即時 GPS 車速
     */
    fun updateSpeed(speedKmH: Int) {
        _navState.value = _navState.value.copy(currentSpeed = speedKmH)
    }

    /**
     * 更新測速照相警示
     */
    fun updateCameraAlert(warningTag: String?, distanceMeters: Int?, speedLimit: Int = 50) {
        _navState.value = _navState.value.copy(
            cameraWarning = warningTag,
            cameraDistance = distanceMeters,
            speedLimit = speedLimit
        )
    }

    /**
     * 設定 Smart Glance 自動喚醒
     */
    fun setSmartGlance(enabled: Boolean, thresholdMeters: Int = 300) {
        _isSmartGlanceEnabled.value = enabled
        _smartGlanceThreshold.value = thresholdMeters
    }

    /**
     * 設定時速表開關
     */
    fun setSpeedometerEnabled(enabled: Boolean) {
        _isSpeedometerEnabled.value = enabled
    }

    /**
     * 更新 HUD 視圖微調參數
     */
    fun updateHudOffset(offsetX: Float, offsetY: Float, scale: Float) {
        _hudOffsetX.value = offsetX
        _hudOffsetY.value = offsetY
        _hudScale.value = scale
    }
}
