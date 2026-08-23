package com.chattriggers.ctjs.internal.mixins;

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

    @Accessor("xd")
    double getVelocityX();

    @Accessor("xd")
    void setVelocityX(double value);

    @Accessor("yd")
    double getVelocityY();

    @Accessor("yd")
    void setVelocityY(double value);

    @Accessor("zd")
    double getVelocityZ();

    @Accessor("zd")
    void setVelocityZ(double value);

    @Accessor
    int getAge();

    @Accessor
    void setAge(int value);

    @Accessor("xo")
    double getPrevPosX();

    @Accessor("xo")
    void setPrevPosX(double value);

    @Accessor("yo")
    double getPrevPosY();

    @Accessor("yo")
    void setPrevPosY(double value);

    @Accessor("zo")
    double getPrevPosZ();

    @Accessor("zo")
    void setPrevPosZ(double value);

    @Accessor("removed")
    boolean getDead();

    @Accessor("removed")
    void setDead(boolean value);
}
