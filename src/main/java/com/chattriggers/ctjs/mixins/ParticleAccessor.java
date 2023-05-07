package com.chattriggers.ctjs.mixins;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor(value = "x")
    double getX();

    @Accessor(value = "x")
    void setX(double value);

    @Accessor(value = "y")
    double getY();

    @Accessor(value = "y")
    void setY(double value);

    @Accessor(value = "z")
    double getZ();

    @Accessor(value = "z")
    void setZ(double value);

    @Accessor
    double getVelocityX();

    @Accessor
    void setVelocityX(double value);

    @Accessor
    double getVelocityY();

    @Accessor
    void setVelocityY(double value);

    @Accessor
    double getVelocityZ();

    @Accessor
    void setVelocityZ(double value);

    @Accessor
    float getRed();

    @Accessor
    void setRed(float value);

    @Accessor
    float getGreen();

    @Accessor
    void setGreen(float value);

    @Accessor
    float getBlue();

    @Accessor
    void setBlue(float value);

    @Accessor
    float getAlpha();

    @Accessor
    void setAlpha(float value);

    @Accessor
    int getAge();

    @Accessor
    void setAge(int value);

    @Accessor
    double getPrevPosX();

    @Accessor
    void setPrevPosX(double value);

    @Accessor
    double getPrevPosY();

    @Accessor
    void setPrevPosY(double value);

    @Accessor
    double getPrevPosZ();

    @Accessor
    void setPrevPosZ(double value);

    @Accessor
    boolean getDead();

    @Accessor
    void setDead(boolean value);
}
