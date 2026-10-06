package com.metro.widgets.data

import com.metro.system.MetroClockFace
import com.metro.system.MetroClockFaceParts
import java.time.LocalDateTime

typealias ClockFaceParts = MetroClockFaceParts

object TimeFaceLogic {
    fun parts(
        now: LocalDateTime = LocalDateTime.now(),
        use24Hour: Boolean = false,
    ): ClockFaceParts = MetroClockFace.parts(now, use24Hour)
}
