package com.chattriggers.ctjs.minecraft.wrappers

import com.chattriggers.ctjs.minecraft.CTEvents
import com.chattriggers.ctjs.utils.Initializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

object CPS : Initializer {
    private val leftClicks = ClicksTracker()
    private val rightClicks = ClicksTracker()

    override fun init() {
        ClientTickEvents.START_CLIENT_TICK.register {
            leftClicks.tick()
            rightClicks.tick()
        }

        CTEvents.MOUSE_CLICKED.register { _, _, button, pressed ->
            if (pressed) {
                when (button) {
                    0 -> leftClicks.click()
                    1 -> rightClicks.click()
                }
            }
        }
    }

    @JvmStatic
    fun getLeftClicksMax(): Int = leftClicks.maxClicks

    @JvmStatic
    fun getRightClicksMax(): Int = rightClicks.maxClicks

    @JvmStatic
    fun getLeftClicks(): Int = leftClicks.clicks.size

    @JvmStatic
    fun getRightClicks(): Int = rightClicks.clicks.size

    @JvmStatic
    fun getLeftClicksAverage(): Int = leftClicks.average()

    @JvmStatic
    fun getRightClicksAverage(): Int = rightClicks.average()

    private class ClicksTracker {
        val clicks = mutableListOf<Int>()
        var maxClicks = 0
        private val runningAverages = LinkedList<Int>()

        fun click() {
            clicks.add(20)
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
