package com.chattriggers.ctjs.internal.mixins.commands;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommandDispatcher.class, remap = false)
public class CommandDispatcherMixin {
    @Inject(
        method = "parseNodes",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/brigadier/context/CommandContextBuilder;withCommand(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/context/CommandContextBuilder;",
            shift = At.Shift.AFTER
        )
    )
    private <S> void addRedirectCommandToContextIfNecessary(
        CommandNode<S> node,
        StringReader originalReader,
        CommandContextBuilder<S> contextSoFar,
        CallbackInfoReturnable<ParseResults<S>> cir,
        @Local(ordinal = 1) CommandNode<S> child,
        @Local(ordinal = 1) CommandContextBuilder<S> context
    ) {
        // TODO: If there is a redirect modifier, this fix will ignore it.

        if (context.getCommand() == null && child.getRedirect() != null && child.getRedirect().getCommand() != null)
            context.withCommand(child.getRedirect().getCommand());
    }
}
