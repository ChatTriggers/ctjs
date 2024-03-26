package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardScore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.scoreboard.Scoreboard$1")
public interface Scoreboard$1Accessor {
    @Accessor("field_47543")
    ScoreboardScore getScore();

    @Accessor("field_47547")
    ScoreHolder getHolder();
}
