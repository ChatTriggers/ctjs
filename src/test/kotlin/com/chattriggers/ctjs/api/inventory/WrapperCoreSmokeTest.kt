package com.chattriggers.ctjs.api.inventory

import com.chattriggers.ctjs.api.world.block.BlockPos
import kotlin.test.Test
import kotlin.test.assertEquals

class WrapperCoreSmokeTest {
    @Test
    fun `block position wrapper retains flooring and offsets`() {
        val pos = BlockPos(1.9, 2.1, -3.2)
        assertEquals(listOf(1, 2, -4), listOf(pos.x, pos.y, pos.z))
        assertEquals(listOf(1, 3, -4), pos.up().let { listOf(it.x, it.y, it.z) })
    }
}
