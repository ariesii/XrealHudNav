package com.xreal.hudnav.presentation

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display

/**
 * 外接顯示器生命週期監聽輔助類別
 * 自動管理 XREAL 眼鏡插拔與 Presentation 初始化
 */
class DisplayAssistant(
    private val context: Context,
    private val listener: OnGlassesDisplayListener
) {
    interface OnGlassesDisplayListener {
        fun onGlassesConnected(display: Display)
        fun onGlassesDisconnected()
    }

    companion object {
        private const val TAG = "DisplayAssistant"
    }

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var activePresentation: XrealHudPresentation? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            Log.d(TAG, "偵測到外接顯示器加入: ID = ")
            checkAndConnectGlasses()
        }

        override fun onDisplayRemoved(displayId: Int) {
            Log.d(TAG, "外接顯示器移除: ID = ")
            if (activePresentation?.display?.displayId == displayId) {
                dismissGlassesPresentation()
            }
        }

        override fun onDisplayChanged(displayId: Int) {
            Log.d(TAG, "外接顯示器狀態變更: ID = ")
        }
    }

    fun startListening() {
        displayManager.registerDisplayListener(displayListener, null)
        checkAndConnectGlasses()
    }

    fun stopListening() {
        displayManager.unregisterDisplayListener(displayListener)
        dismissGlassesPresentation()
    }

    /**
     * 掃描並連接外接眼鏡顯示器
     */
    fun checkAndConnectGlasses() {
        val presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        val targetDisplay = presentationDisplays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }

        if (targetDisplay != null) {
            if (activePresentation?.display?.displayId != targetDisplay.displayId) {
                dismissGlassesPresentation()
                Log.i(TAG, "成功偵測到 XREAL 智慧眼鏡，啟動 HUD Presentation")
                activePresentation = XrealHudPresentation(context, targetDisplay).apply {
                    show()
                }
                listener.onGlassesConnected(targetDisplay)
            }
        } else {
            dismissGlassesPresentation()
        }
    }

    /**
     * 關閉並釋放眼鏡端 Presentation
     */
    private fun dismissGlassesPresentation() {
        if (activePresentation != null) {
            try {
                activePresentation?.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "釋放 Presentation 發生異常", e)
            }
            activePresentation = null
            listener.onGlassesDisconnected()
        }
    }

    fun isGlassesConnected(): Boolean = activePresentation != null
}
