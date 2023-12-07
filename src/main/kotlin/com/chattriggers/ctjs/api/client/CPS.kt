package com.chattriggers.ctjs.api.client

import com.chattriggers.ctjs.api.world.World
import com.chattriggers.ctjs.internal.engine.CTEvents
import com.chattriggers.ctjs.internal.utils.Initializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

object CPS : Initializer {
    private val leftClicks = ClicksTracker()
    private val rightClicks = ClicksTracker()
    private val middleClicks = ClicksTracker()

    override fun init() {
        ClientTickEvents.START_CLIENT_TICK.register {
            leftClicks.tick()
            rightClicks.tick()
            middleClicks.tick()
        }

        CTEvents.MOUSE_CLICKED.register { _, _, button, pressed ->
            if (pressed) {
                when (button) {
                    0 -> leftClicks.click()
                    1 -> rightClicks.click()
                    2 -> middleClicks.click()
                }
            }
        }
    }

    @JvmStatic
    fun getLeftClicksMax(): Int = leftClicks.maxClicks

    @JvmStatic
    fun getRightClicksMax(): Int = rightClicks.maxClicks

    @JvmStatic
    fun getMiddleClicksMax(): Int = middleClicks.maxClicks

    @JvmStatic
    fun getLeftClicks(): Int = leftClicks.clicks.size

    @JvmStatic
    fun getRightClicks(): Int = rightClicks.clicks.size

    @JvmStatic
    fun getMiddleClicks(): Int = middleClicks.clicks.size

    @JvmStatic
    fun getLeftClicksAverage(): Int = leftClicks.average()

    @JvmStatic
    fun getRightClicksAverage(): Int = rightClicks.average()

    @JvmStatic
    fun getMiddleClicksAverage(): Int = middleClicks.average()

    private class ClicksTracker {
        val clicks = mutableListOf<Int>()
        var maxClicks = 0
        private val runningAverages = LinkedList<Int>()

        fun click() {
            clicks.add(World.getTicksPerSecond())
        }

        fun tick() {
            // Decrease all existing click values
            for (i in clicks.indices)
                clicks[i]--
            clicks.removeIf { it <= 0 }

            // Save the current CPS
            runningAverages.add(clicks.size)
            if (runningAverages.size > 100)
                runningAverages.removeAt(0)

            maxClicks = if (runningAverages.lastOrNull() == 0) {
                runningAverages.clear()
                0
            } else {
                max(maxClicks, clicks.size)
            }
        }

        fun average() = if (runningAverages.isNotEmpty()) {
            runningAverages.average().roundToInt()
        } else 0
    }
}
