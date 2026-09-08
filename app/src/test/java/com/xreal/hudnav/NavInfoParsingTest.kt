package com.xreal.hudnav

import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NavInfoParsingTest {

    @Test
    fun testManeuverTypeInference() {
        // Google Maps 用語
        assertEquals(ManeuverType.TURN_LEFT, ManeuverType.fromText("100 公尺後左轉"))
        assertEquals(ManeuverType.TURN_LEFT, ManeuverType.fromText("接下來 ↰ 106縣道"))
        assertEquals(ManeuverType.STRAIGHT, ManeuverType.fromText("前往 106 民族路"))
        assertEquals(ManeuverType.U_TURN, ManeuverType.fromText("前方迴轉"))

        // 高德地圖用語
        assertEquals(ManeuverType.U_TURN, ManeuverType.fromText("前方請調頭"))
        assertEquals(ManeuverType.U_TURN, ManeuverType.fromText("前方請掉頭"))
        assertEquals(ManeuverType.SLIGHT_RIGHT, ManeuverType.fromText("80米後 向右前方行駛"))
        assertEquals(ManeuverType.SLIGHT_LEFT, ManeuverType.fromText("200米後 向左前方行駛"))
        assertEquals(ManeuverType.TURN_RIGHT, ManeuverType.fromText("向右轉 進入文化路"))
    }

    @Test
    fun testSpeedingLogic() {
        val normalInfo = NavInfo(currentSpeed = 45, speedLimit = 50)
        assertFalse(normalInfo.isSpeeding())

        val speedingInfo = NavInfo(currentSpeed = 56, speedLimit = 50)
        assertTrue(speedingInfo.isSpeeding())
    }
}
