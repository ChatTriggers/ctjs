package com.chattriggers.ctjs.mixins;

import com.chattriggers.ctjs.minecraft.CTEvents;
import com.chattriggers.ctjs.minecraft.libs.renderer.Renderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Invoker
    public abstract void invokeRenderFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity);

    @Inject(method = "reload", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectReload(ResourceManager manager, CallbackInfo ci, EntityRendererFactory.Context context) {
        Renderer.initializePlayerRenderers$ctjs(context);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void injectRender(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                              CallbackInfo ci) {
        CTEvents.RENDER_ENTITY.invoker().render(matrices, entity, tickDelta, ci);
    }
}
