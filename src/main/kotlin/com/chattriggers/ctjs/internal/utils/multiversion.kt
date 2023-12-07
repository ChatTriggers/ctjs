package com.chattriggers.ctjs.internal.utils

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.HoverEvent

//#if MC>=12000
import net.minecraft.client.gui.DrawContext

fun DrawContext.toMatrixStack(): MatrixStack = matrices
//#endif

fun MatrixStack.toMatrixStack() = this

fun hoverEventActionByName(name: String): HoverEvent.Action<*> {
    //#if MC>=12004
    return when (name.uppercase()) {
        "SHOW_TEXT" -> HoverEvent.Action.SHOW_TEXT
        "SHOW_ITEM" -> HoverEvent.Action.SHOW_ITEM
        "SHOW_ENTITY" -> HoverEvent.Action.SHOW_ENTITY
        else -> throw IllegalStateException("Unknown hover event action \"$name\"")
    }
    //#else
    //$$ return HoverEvent.Action.byName(name.lowercase())
    //$$     ?: throw IllegalStateException("Unknown hover event action \"$name\"")
    //#endif
}
