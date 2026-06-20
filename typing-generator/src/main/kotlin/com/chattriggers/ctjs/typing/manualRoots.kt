package com.chattriggers.ctjs.typing

import org.jsoup.Jsoup

object ManualRoots {
    private const val FABRIC_API_URL = "https://maven.fabricmc.net/docs/fabric-api-0.151.0+26.1.2/allclasses-index.html"

    val roots = mutableSetOf(
        "java.awt.Color",
        "java.util.ArrayList",
        "java.util.HashMap",
        "org.lwjgl.glfw.GLFW",
        "org.spongepowered.asm.mixin.injection.callback.CallbackInfo",
        "net.minecraft.client.Minecraft",
        "net.minecraft.util.ARGB",
        "net.minecraft.client.renderer.ShapeRenderer",
        "net.minecraft.client.renderer.rendertype.RenderTypes",
        "com.mojang.brigadier.arguments.ArgumentType",
        "com.mojang.brigadier.arguments.BoolArgumentType",
        "com.mojang.brigadier.arguments.DoubleArgumentType",
        "com.mojang.brigadier.arguments.FloatArgumentType",
        "com.mojang.brigadier.arguments.IntegerArgumentType",
        "com.mojang.brigadier.arguments.LongArgumentType",
        "com.mojang.brigadier.arguments.StringArgumentType", "net.minecraft.commands.arguments.item.ItemArgument",
        "net.minecraft.commands.arguments.SlotArgument",
        "net.minecraft.commands.arguments.TeamArgument",
        "net.minecraft.commands.arguments.TimeArgument",
        "net.minecraft.commands.arguments.UuidArgument",
        "net.minecraft.commands.arguments.coordinates.Vec2Argument",
        "net.minecraft.commands.arguments.coordinates.Vec3Argument",
        "net.minecraft.commands.arguments.AngleArgument",
        "net.minecraft.commands.arguments.RangeArgument",
        "net.minecraft.commands.arguments.SlotsArgument",
        "net.minecraft.commands.arguments.StyleArgument",
        "net.minecraft.commands.arguments.EntityArgument",
        "net.minecraft.commands.arguments.NbtTagArgument",
        "net.minecraft.commands.arguments.SignedArgument",
        "net.minecraft.commands.arguments.MessageArgument",
        "net.minecraft.commands.arguments.NbtPathArgument",
        "net.minecraft.commands.arguments.coordinates.SwizzleArgument",
        "net.minecraft.commands.arguments.coordinates.BlockPosArgument",
        "net.minecraft.commands.arguments.item.FunctionArgument",
        "net.minecraft.commands.arguments.GameModeArgument",
        "net.minecraft.commands.arguments.HexColorArgument",
        "net.minecraft.commands.arguments.ParticleArgument",
        "net.minecraft.commands.arguments.ResourceArgument",
        "net.minecraft.commands.arguments.coordinates.RotationArgument",
        "net.minecraft.commands.arguments.WaypointArgument",
        "net.minecraft.commands.arguments.coordinates.ColumnPosArgument",
        "net.minecraft.commands.arguments.ComponentArgument",
        "net.minecraft.commands.arguments.DimensionArgument",
        "net.minecraft.commands.arguments.ObjectiveArgument",
        "net.minecraft.commands.arguments.OperationArgument",
        "net.minecraft.commands.arguments.TeamColorArgument",
        "net.minecraft.commands.arguments.blocks.BlockStateArgument",
        "net.minecraft.commands.arguments.IdentifierArgument",
        "net.minecraft.commands.arguments.CompoundTagArgument",
        "net.minecraft.commands.arguments.GameProfileArgument",
        "net.minecraft.commands.arguments.ResourceKeyArgument",
        "net.minecraft.commands.arguments.ScoreHolderArgument",
        "net.minecraft.commands.arguments.EntityAnchorArgument",
        "net.minecraft.commands.arguments.ResourceOrIdArgument",
        "net.minecraft.commands.arguments.HeightmapTypeArgument",
        "net.minecraft.commands.arguments.item.ItemPredicateArgument",
        "net.minecraft.commands.arguments.ResourceOrTagArgument",
        "net.minecraft.commands.arguments.blocks.BlockPredicateArgument",
        "net.minecraft.commands.arguments.ScoreboardSlotArgument",
        "net.minecraft.commands.arguments.TemplateMirrorArgument",
        "net.minecraft.commands.arguments.ResourceOrTagKeyArgument",
        "net.minecraft.commands.arguments.ResourceSelectorArgument",
        "net.minecraft.commands.arguments.TemplateRotationArgument",
        "net.minecraft.commands.arguments.ObjectiveCriteriaArgument",
        "net.minecraft.commands.arguments.StringRepresentableArgument",
    )

    private fun collectClasses(url: String) {
        val doc = Jsoup.connect(url).get()
        val links = doc.getElementsByClass("col-first")
        links.forEach { link ->
            val a = link.getElementsByTag("a")
            if (a.isEmpty()) return@forEach
            val href = a.first()?.attr("href") ?: return@forEach
            roots += href.replace("/", ".").replace(".html", "").trim()
        }
    }

    init {
        collectClasses(FABRIC_API_URL)
    }
}


