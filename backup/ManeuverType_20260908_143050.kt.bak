package com.xreal.hudnav.model

import androidx.annotation.DrawableRes
import com.xreal.hudnav.R

/**
 * 導航轉向動作類型列舉
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
         * 根據文字描述自動推斷轉向類型
         *
         * @param text 包含方向指示的字串
         * @return 對應的 ManeuverType
         */
        fun fromText(text: String): ManeuverType {
            val lower = text.lowercase()
            return when {
                lower.contains("迴轉") || lower.contains("u-turn") -> U_TURN
                lower.contains("靠左") || lower.contains("微左") || lower.contains("slight left") -> SLIGHT_LEFT
                lower.contains("靠右") || lower.contains("微右") || lower.contains("slight right") -> SLIGHT_RIGHT
                lower.contains("左轉") || lower.contains("left") || text.contains("↰") || text.contains("↲") -> TURN_LEFT
                lower.contains("右轉") || lower.contains("right") || text.contains("↱") || text.contains("↳") -> TURN_RIGHT
                lower.contains("抵達") || lower.contains("目的地") || lower.contains("arrive") -> DESTINATION
                lower.contains("直行") || lower.contains("前往") || text.contains("↑") || lower.contains("straight") -> STRAIGHT
                else -> UNKNOWN
            }
        }
    }
}
