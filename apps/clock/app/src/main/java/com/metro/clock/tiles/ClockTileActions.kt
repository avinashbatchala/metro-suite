package com.metro.clock.tiles

object ClockTileActions {
    const val PRIMARY = "primary"

    fun alarmTile(id: Long) = "alarm:$id"
    fun timerTile(id: Long) = "timer:$id"
    fun stopwatchTile(id: Long) = "stopwatch:$id"
    fun worldTile(cityId: String) = "world:$cityId"

    /** Maps a tile id to the in-app deep link it should open. */
    fun deepLinkFor(tileId: String): String = when {
        tileId.startsWith("alarm:") -> "metro://clock/alarm/${tileId.substringAfter(':')}"
        tileId.startsWith("timer:") -> "metro://clock/timer/${tileId.substringAfter(':')}"
        tileId.startsWith("stopwatch:") -> "metro://clock/stopwatch/${tileId.substringAfter(':')}"
        tileId.startsWith("world:") -> "metro://clock/world/${tileId.substringAfter(':')}"
        else -> "metro://clock/alarms"
    }
}
