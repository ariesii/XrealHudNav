package com.xreal.hudnav.model

/**
 * 導航即時資訊資料結構
 *
 * @property maneuver 當前轉向動作類型
 * @property distance 距離下一個轉彎處的距離（例如 "100 公尺"）
 * @property roadNumber 道路或國道/縣道編號（例如 "106"、"台64"）
 * @property roadName 道路名稱（例如 "民族路"）
 * @property nextAction 接下來的連續轉彎提醒（例如 "接下來 ↰ 106縣道"）
 * @property eta 預計抵達時間（例如 "上午10:12"）
 * @property remainingTime 剩餘行車時間（例如 "23 分鐘"）
 * @property remainingDistance 剩餘總里程（例如 "11 公里"）
 * @property isNavigating 目前是否處於有效導航狀態
 */
data class NavInfo(
    val maneuver: ManeuverType = ManeuverType.STRAIGHT,
    val distance: String = "直行",
    val roadNumber: String? = null,
    val roadName: String = "等待導航開始...",
    val nextAction: String? = null,
    val eta: String = "--:--",
    val remainingTime: String = "--",
    val remainingDistance: String = "--",
    val isNavigating: Boolean = false
) {
    /**
     * 組合道路標示顯示字串
     */
    fun getFullRoadDisplay(): String {
        return if (!roadNumber.isNullOrBlank()) {
            "[] "
        } else {
            roadName
        }
    }
}
