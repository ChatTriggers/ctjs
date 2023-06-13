package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(
            method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;runReported(Lnet/minecraft/block/entity/BlockEntity;Ljava/lang/Runnable;)V"
            ),
            cancellable = true
    )
    private void injectRender(BlockEntity blockEntity, float tickDelta, MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (blockEntity.hasWorld() && Objects.requireNonNull(blockEntity.getWorld()).isClient) {
            CTEvents.RENDER_BLOCK_ENTITY.invoker().render(matrices, blockEntity, tickDelta, ci);
        }
    }
}
