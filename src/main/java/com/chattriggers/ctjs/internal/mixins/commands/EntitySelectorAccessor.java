package com.chattriggers.ctjs.internal.mixins.commands;

import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
    @Accessor("maxResults")
    int getLimit();

    @Accessor("includesEntities")
    boolean getIncludesNonPlayers();

    @Accessor("contextFreePredicates")
    List<Predicate<Entity>> getPredicates();

    @Accessor("range")
    MinMaxBounds.Doubles getDistance();

    @Accessor("position")
    Function<Vec3, Vec3> getPositionOffset();

    @Accessor("aabb")
    AABB getBox();

    @Accessor("order")
    BiConsumer<Vec3, List<? extends Entity>> getSorter();

    @Accessor("currentEntity")
    boolean getSenderOnly();

    @Accessor
    String getPlayerName();

    @Accessor("entityUUID")
    UUID getUuid();

    @Accessor("type")
    EntityTypeTest<Entity, ?> getEntityFilter();
}
