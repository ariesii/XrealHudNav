package com.xreal.hudnav

import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.TrafficLightState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavInfoParsingTest {

    @Test
    fun testAmapIconCodeMapping() {
        // 測試高德代碼映射
        assertEquals(ManeuverType.TURN_RIGHT, ManeuverType.fromAmapIconCode(3))
        assertEquals(ManeuverType.TURN_LEFT, ManeuverType.fromAmapIconCode(2))
        assertEquals(ManeuverType.U_TURN, ManeuverType.fromAmapIconCode(8))
        assertEquals(ManeuverType.STRAIGHT, ManeuverType.fromAmapIconCode(9))
    }

    @Test
    fun testTrafficLightLogic() {
        val infoWithLight = NavInfo(
            trafficLightState = TrafficLightState.RED,
            trafficLightSeconds = 24
        )
        assertTrue(infoWithLight.hasTrafficLight())
        assertEquals(24, infoWithLight.trafficLightSeconds)
    }
}
