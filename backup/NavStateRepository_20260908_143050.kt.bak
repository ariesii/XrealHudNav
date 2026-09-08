package com.xreal.hudnav.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 導航即時狀態管理儲存庫（單例模式）
 * 供 NotificationListenerService、模擬器與 Presentation 共享狀態
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

    /**
     * 更新導航資料狀態
     *
     * @param info 新的導航資料
     */
    fun updateNavInfo(info: NavInfo) {
        _navState.value = info
    }

    /**
     * 更新 HUD 視圖微調參數
     *
     * @param offsetX 水平偏移像素
     * @param offsetY 垂直偏移像素
     * @param scale 縮放比例 (0.5 ~ 1.5)
     */
    fun updateHudOffset(offsetX: Float, offsetY: Float, scale: Float) {
        _hudOffsetX.value = offsetX
        _hudOffsetY.value = offsetY
        _hudScale.value = scale
    }
}
