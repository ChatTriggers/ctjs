package com.chattriggers.ctjs.api.render

import kotlin.test.Test
import kotlin.test.assertContentEquals

class GuiInputArgumentsTest {
    @Test
    fun `drag callback uses coordinates from the same event`() {
        assertContentEquals(
            arrayOf<Any>(3.5, -4.75, 123.25, 456.75, 2),
            GuiInputArguments.mouseDragged(3.5, -4.75, 123.25, 456.75, 2),
        )
    }
}
