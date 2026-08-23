package com.chattriggers.ctjs.internal.triggers

import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.world.block.BlockPos
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos as MCBlockPos
import net.minecraft.world.entity.Entity as MCEntity
import java.util.ArrayList

object TriggerContractAdapters {
    @JvmStatic
    fun entity(entity: MCEntity): Entity = Entity.fromMC(entity)

    @JvmStatic
    fun blockPos(pos: MCBlockPos): BlockPos = BlockPos(pos)

    @JvmStatic
    fun <T> mutableList(values: Collection<T>): MutableList<T> = ArrayList(values)

    @JvmStatic
    fun shouldTriggerGuiOpened(screen: Screen?): Boolean = screen != null
}
