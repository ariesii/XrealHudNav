package com.xreal.hudnav

import com.xreal.hudnav.model.ManeuverType
import com.xreal.hudnav.model.NavInfo
import com.xreal.hudnav.model.PowerMode
import com.xreal.hudnav.model.TrafficLightState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavInfoParsingTest {

    @Test
    fun testAmapIconCodeMapping() {
        assertEquals(ManeuverType.TURN_RIGHT, ManeuverType.fromAmapIconCode(3))
        assertEquals(ManeuverType.TURN_LEFT, ManeuverType.fromAmapIconCode(2))
        assertEquals(ManeuverType.U_TURN, ManeuverType.fromAmapIconCode(8))
        assertEquals(ManeuverType.STRAIGHT, ManeuverType.fromAmapIconCode(9))
    }

    @Test
    fun testBatteryAndPowerMode() {
        val info = NavInfo(
            batteryLevel = 85,
            isCharging = true,
            powerMode = PowerMode.ECO
        )
        assertEquals(85, info.batteryLevel)
        assertTrue(info.isCharging)
        assertEquals(PowerMode.ECO, info.powerMode)
    }
}
