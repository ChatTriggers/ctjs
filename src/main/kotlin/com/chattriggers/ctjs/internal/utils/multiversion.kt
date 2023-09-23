package com.chattriggers.ctjs.internal.utils

import net.minecraft.client.util.math.MatrixStack

//#if MC>=12000
import net.minecraft.client.gui.DrawContext

fun DrawContext.toMatrixStack(): MatrixStack = matrices
//#endif

fun MatrixStack.toMatrixStack() = this
