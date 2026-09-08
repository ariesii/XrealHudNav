package com.xreal.hudnav

import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 導航資料模型與轉向解析測試
 */
class NavInfoParsingTest {

    @Test
    fun testManeuverTypeInference() {
        // 測試中文轉向推斷
        assertEquals(ManeuverType.TURN_LEFT, ManeuverType.fromText("100 公尺後左轉"))
        assertEquals(ManeuverType.TURN_LEFT, ManeuverType.fromText("接下來 ↰ 106縣道"))
        assertEquals(ManeuverType.TURN_RIGHT, ManeuverType.fromText("靠右走文化路"))
        assertEquals(ManeuverType.U_TURN, ManeuverType.fromText("前方迴轉"))
        assertEquals(ManeuverType.STRAIGHT, ManeuverType.fromText("前往 106 民族路"))
        assertEquals(ManeuverType.DESTINATION, ManeuverType.fromText("抵達目的地"))
    }

    @Test
    fun testNavInfoRoadDisplay() {
        val infoWithNumber = NavInfo(
            maneuver = ManeuverType.STRAIGHT,
            roadNumber = "106",
            roadName = "民族路"
        )
        assertEquals("[106] 民族路", infoWithNumber.getFullRoadDisplay())

        val infoWithoutNumber = NavInfo(
            maneuver = ManeuverType.STRAIGHT,
            roadNumber = null,
            roadName = "新府路"
        )
        assertEquals("新府路", infoWithoutNumber.getFullRoadDisplay())
    }
}
