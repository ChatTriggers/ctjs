package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SingleQuadParticle.class)
public interface SingleQuadParticleAccessor {
    @Accessor("rCol")
    float getRed();

    @Accessor("rCol")
    void setRed(float value);

    @Accessor("gCol")
    float getGreen();

    @Accessor("gCol")
    void setGreen(float value);

    @Accessor("bCol")
    float getBlue();

    @Accessor("bCol")
    void setBlue(float value);

    @Accessor
    float getAlpha();

    @Accessor
    void setAlpha(float value);
}
