package com.chattriggers.ctjs.internal.mixins.commands;

import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
    @Accessor
    int getLimit();

    @Accessor
    boolean getIncludesNonPlayers();

    @Accessor
    List<Predicate<Entity>> getPredicates();

    @Accessor
    NumberRange.DoubleRange getDistance();

    @Accessor
    Function<Vec3d, Vec3d> getPositionOffset();

    @Accessor
    Box getBox();

    @Accessor
    BiConsumer<Vec3d, List<? extends Entity>> getSorter();

    @Accessor
    boolean getSenderOnly();

    @Accessor
    String getPlayerName();

    @Accessor
    UUID getUuid();

    @Accessor
    TypeFilter<Entity, ?> getEntityFilter();
}
