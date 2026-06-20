package com.chattriggers.ctjs.internal.mixins;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(CommandNode.class)
public interface CommandNodeAccessor {
    @Accessor("children")
    Map<String, CommandNode<?>> getChildren();

    @Accessor("literals")
    Map<String, LiteralCommandNode<?>> getLiterals();
}
