package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void injectRender(BlockEntity blockEntity, float tickDelta,
                              ModelFeatureRenderer.CrumblingOverlay breakProgress,
                              CallbackInfoReturnable<BlockEntityRenderState> ci) {
        if (blockEntity.hasLevel() && Objects.requireNonNull(blockEntity.getLevel()).isClientSide()) {
            CTEvents.RENDER_BLOCK_ENTITY.invoker().render(new PoseStack(), blockEntity, tickDelta, ci);
        }
    }
}
