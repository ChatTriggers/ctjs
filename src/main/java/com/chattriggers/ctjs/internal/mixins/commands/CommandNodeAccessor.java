package com.chattriggers.ctjs.internal.mixins.commands;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = CommandNode.class, remap = false)
public interface CommandNodeAccessor {
    @Accessor("children")
    Map<String, CommandNode<?>> getChildNodes();

    @Accessor
    Map<String, LiteralCommandNode<?>> getLiterals();
}
