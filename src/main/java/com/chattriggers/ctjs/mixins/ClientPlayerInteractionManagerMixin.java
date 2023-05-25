package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.wrappers.World;
import com.chattriggers.ctjs.minecraft.wrappers.world.block.BlockFace;
import com.chattriggers.ctjs.triggers.TriggerType;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    void injectAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        TriggerType.HIT_BLOCK.triggerAll(World.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).withFace(BlockFace.fromMC(direction)), cir);
    }

    @Inject(method = "breakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V"))
    void injectBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        TriggerType.BREAK_BLOCK.triggerAll(World.getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
    }
}
