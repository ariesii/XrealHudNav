package com.xreal.hudnav.model

import androidx.annotation.DrawableRes
import com.xreal.hudnav.R

/**
 * 導航轉向動作類型列舉（相容 Google Maps 與高德地圖用語）
 */
enum class ManeuverType(
    val description: String,
    @DrawableRes val iconResId: Int
) {
    STRAIGHT("直行", R.drawable.ic_arrow_straight),
    TURN_LEFT("左轉", R.drawable.ic_arrow_turn_left),
    TURN_RIGHT("右轉", R.drawable.ic_arrow_turn_right),
    SLIGHT_LEFT("微靠左", R.drawable.ic_arrow_slight_left),
    SLIGHT_RIGHT("微靠右", R.drawable.ic_arrow_slight_right),
    U_TURN("迴轉", R.drawable.ic_arrow_u_turn),
    DESTINATION("抵達目的地", R.drawable.ic_destination),
    UNKNOWN("導航中", R.drawable.ic_arrow_straight);

    companion object {
        /**
         * 根據文字描述自動推斷轉向類型（相容 Google Maps 與高德常用詞彙）
         *
         * @param text 包含方向指示的字串
         * @return 對應的 ManeuverType
         */
        fun fromText(text: String): ManeuverType {
            val lower = text.lowercase()
            return when {
                // 迴轉相容：Google「迴轉」、高德「調頭 / 掉頭 / u-turn」
                lower.contains("迴轉") || lower.contains("調頭") || lower.contains("掉頭") || lower.contains("u-turn") || text.contains("⤺") -> U_TURN
                
                // 微左相容：微靠左、向左前方、靠左、slight left
                lower.contains("靠左") || lower.contains("微左") || lower.contains("左前方") || lower.contains("slight left") || text.contains("↖") -> SLIGHT_LEFT
                
                // 微右相容：微靠右、向右前方、靠右、slight right
                lower.contains("靠右") || lower.contains("微右") || lower.contains("右前方") || lower.contains("slight right") || text.contains("↗") -> SLIGHT_RIGHT
                
                // 左轉相容：左轉、向左轉、↰、↲
                lower.contains("左轉") || lower.contains("向左") || lower.contains("left") || text.contains("↰") || text.contains("↲") -> TURN_LEFT
                
                // 右轉相容：右轉、向右轉、↱、↳
                lower.contains("右轉") || lower.contains("向右") || lower.contains("right") || text.contains("↱") || text.contains("↳") -> TURN_RIGHT
                
                // 抵達相容：抵達、到達、終點、目的地、arrive
                lower.contains("抵達") || lower.contains("到達") || lower.contains("目的地") || lower.contains("arrive") -> DESTINATION
                
                // 直行相容：直行、順行、沿著、前往、↑、straight
                lower.contains("直行") || lower.contains("前往") || lower.contains("進入") || lower.contains("順行") || text.contains("↑") || lower.contains("straight") -> STRAIGHT
                
                else -> UNKNOWN
            }
        }
    }
}
