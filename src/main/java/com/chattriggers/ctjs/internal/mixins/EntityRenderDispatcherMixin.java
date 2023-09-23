package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.api.render.Renderer;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "reload", at = @At("TAIL"))
    private void injectReload(ResourceManager manager, CallbackInfo ci, @Local EntityRendererFactory.Context context) {
        Renderer.initializePlayerRenderers$ctjs(context);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void injectRender(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                              CallbackInfo ci) {
        CTEvents.RENDER_ENTITY.invoker().render(matrices, entity, tickDelta, ci);
    }
}
