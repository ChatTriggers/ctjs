package com.chattriggers.ctjs.typing

val manualRoots = setOf(
    "java.awt.Color",
    "java.util.ArrayList",
    "java.util.HashMap",
    "gg.essential.universal.UKeyboard",
    "net.minecraft.util.Hand",
    "org.lwjgl.opengl.GL11",
    "org.lwjgl.opengl.GL12",
    "org.lwjgl.opengl.GL13",
    "org.lwjgl.opengl.GL14",
    "org.lwjgl.opengl.GL15",
    "org.lwjgl.opengl.GL20",
    "org.lwjgl.opengl.GL21",
    "org.lwjgl.opengl.GL30",
    "org.lwjgl.opengl.GL31",
    "org.lwjgl.opengl.GL32",
    "org.lwjgl.opengl.GL33",
    "org.lwjgl.opengl.GL40",
    "org.lwjgl.opengl.GL41",
    "org.lwjgl.opengl.GL42",
    "org.lwjgl.opengl.GL43",
    "org.lwjgl.opengl.GL44",
    "org.lwjgl.opengl.GL45",
    "org.spongepowered.asm.mixin.injection.callback.CallbackInfo",
)

val prologue = """
    /// <reference no-default-lib="true" />
    /// <reference lib="es2015" />
    export {};
    
    declare interface String {
      addFormatting(): string;
      addColor(): string;
      removeFormatting(): string;
      replaceFormatting(): string;
    }
    
    declare interface Number {
      easeOut(to: number, speed: number, jump: number): number;
      easeColor(to: number, speed: number, jump: number): java.awt.Color;
    }
  
    declare global {
      const Java: {
        /**
         * Returns the Java Class or Package given by name. If you want to
         * enforce the name is a class, use Java.class() instead.
         */
        type(name: string): java.lang.Package | java.lang.Class<any>;
  
        /**
         * Returns the Java Class given by `className`. Throws an error if the
         * name is not a valid class name.
         */
        class(className: string): java.lang.Class<any>;
      };

      /**
       * Runs `func` in a Java synchronized() block with `lock` as the synchronizer
       */
      function sync(func: () => void, lock: unknown);
  
      /**
       * Runs `func` after `delayInMs` milliseconds. A new thread is spawned to accomplish
       * this, which means this function is asynchronous. If you want to avoid the Thread
       * instantiation, use `Client.scheduleTask(delayInTicks, func)`.
       */
      function setTimeout(func: () => void, delayInMs: number);

      type ArrayList<T> = java.util.ArrayList<T>;
      type HashMap<K, V> = java.util.HashMap<K, V>;
      type Keyboard = gg.essential.universal.UKeyboard;
      type Hand = net.minecraft.util.Hand;

      type Client = com.chattriggers.ctjs.api.client.Client;
      type CPS = com.chattriggers.ctjs.api.client.CPS;
      type FileLib = com.chattriggers.ctjs.api.client.FileLib;
      type KeyBind = com.chattriggers.ctjs.api.client.KeyBind;
      type MathLib = com.chattriggers.ctjs.api.client.MathLib;
      type Player = com.chattriggers.ctjs.api.client.Player;
      type Settings = com.chattriggers.ctjs.api.client.Settings;
      type Sound = com.chattriggers.ctjs.api.client.Sound;

      type Commands = com.chattriggers.ctjs.api.commands.DynamicCommands;

      type BlockEntity = com.chattriggers.ctjs.api.entity.BlockEntity;
      type Entity = com.chattriggers.ctjs.api.entity.Entity;
      type LivingEntity = com.chattriggers.ctjs.api.entity.LivingEntity;
      type Particle = com.chattriggers.ctjs.api.entity.Particle;
      type PlayerMP = com.chattriggers.ctjs.api.entity.PlayerMP;
      type Team = com.chattriggers.ctjs.api.entity.Team;

      type Action = com.chattriggers.ctjs.api.inventory.action.Action;
      type ClickAction = com.chattriggers.ctjs.api.inventory.action.ClickAction;
      type DragAction = com.chattriggers.ctjs.api.inventory.action.DragAction;
      type DropAction = com.chattriggers.ctjs.api.inventory.action.DropAction;
      type KeyAction = com.chattriggers.ctjs.api.inventory.action.KeyAction;
      type NBT = com.chattriggers.ctjs.api.inventory.nbt.NBT;
      type NBTBase = com.chattriggers.ctjs.api.inventory.nbt.NBTBase;
      type NBTTagCompound = com.chattriggers.ctjs.api.inventory.nbt.NBTTagCompound;
      type NBTTagList = com.chattriggers.ctjs.api.inventory.nbt.NBTTagList;
      type Inventory = com.chattriggers.ctjs.api.inventory.Inventory;
      type Item = com.chattriggers.ctjs.api.inventory.Item;
      type ItemType = com.chattriggers.ctjs.api.inventory.ItemType;
      type Slot = com.chattriggers.ctjs.api.inventory.Slot;

      type ChatLib = com.chattriggers.ctjs.api.message.ChatLib;
      type Message = com.chattriggers.ctjs.api.message.Message;
      type TextComponent = com.chattriggers.ctjs.api.message.TextComponent;

      type Book = com.chattriggers.ctjs.api.render.Book;
      type Display = com.chattriggers.ctjs.api.render.Display;
      type Gui = com.chattriggers.ctjs.api.render.Gui;
      type Image = com.chattriggers.ctjs.api.render.Image;
      type Rectangle = com.chattriggers.ctjs.api.render.Rectangle;
      type Renderer = com.chattriggers.ctjs.api.render.Renderer;
      type Renderer3d = com.chattriggers.ctjs.api.render.Renderer3d;
      type Shape = com.chattriggers.ctjs.api.render.Shape;
      type Text = com.chattriggers.ctjs.api.render.Text;

      // For module authors to use with custom triggers
      type CancellableEvent = com.chattriggers.ctjs.api.triggers.CancellableEvent;

      type Vec2f = com.chattriggers.ctjs.api.vec.Vec2f;
      type Vec3f = com.chattriggers.ctjs.api.vec.Vec3f;
      type Vec3i = com.chattriggers.ctjs.api.vec.Vec3i;

      type Block = com.chattriggers.ctjs.api.world.block.Block;
      type BlockFace = com.chattriggers.ctjs.api.world.block.BlockFace;
      type BlockPos = com.chattriggers.ctjs.api.world.block.BlockPos;
      type BlockType = com.chattriggers.ctjs.api.world.block.BlockType;
      type BossBars = com.chattriggers.ctjs.api.world.BossBars;
      type Chunk = com.chattriggers.ctjs.api.world.Chunk;
      type PotionEffect = com.chattriggers.ctjs.api.world.PotionEffect;
      type PotionEffectType = com.chattriggers.ctjs.api.world.PotionEffectType;
      type Scoreboard = com.chattriggers.ctjs.api.world.Scoreboard;
      type Server = com.chattriggers.ctjs.api.world.Server;
      type TabList = com.chattriggers.ctjs.api.world.TabList;
      type World = com.chattriggers.ctjs.api.world.World;

      type Config = com.chattriggers.ctjs.api.Config;

      // Misc

      type TriggerRegister = com.chattriggers.ctjs.engine.Register;
      type Thread = com.chattriggers.ctjs.engine.WrappedThread;
      type Priority = com.chattriggers.ctjs.api.triggers.Trigger${'$'}Priority;
      type ChatTriggers = com.chattriggers.ctjs.CTJS;
      type Console = com.chattriggers.ctjs.engine.Console;

      // GL
      type GL11 = org.lwjgl.opengl.GL11;
      type GL12 = org.lwjgl.opengl.GL12;
      type GL13 = org.lwjgl.opengl.GL13;
      type GL14 = org.lwjgl.opengl.GL14;
      type GL15 = org.lwjgl.opengl.GL15;
      type GL20 = org.lwjgl.opengl.GL20;
      type GL21 = org.lwjgl.opengl.GL21;
      type GL30 = org.lwjgl.opengl.GL30;
      type GL31 = org.lwjgl.opengl.GL31;
      type GL32 = org.lwjgl.opengl.GL32;
      type GL33 = org.lwjgl.opengl.GL33;
      type GL40 = org.lwjgl.opengl.GL40;
      type GL41 = org.lwjgl.opengl.GL41;
      type GL42 = org.lwjgl.opengl.GL42;
      type GL43 = org.lwjgl.opengl.GL43;
      type GL44 = org.lwjgl.opengl.GL44;
      type GL45 = org.lwjgl.opengl.GL45;
  
      interface RegisterTypes {
        chat(...args: (string | unknown)[]): com.chattriggers.ctjs.api.triggers.ChatTrigger;
        actionBar(...args: (string | unknown)[]): com.chattriggers.ctjs.api.triggers.ChatTrigger;
        worldLoad(): com.chattriggers.ctjs.api.triggers.Trigger;
        worldUnload(): com.chattriggers.ctjs.api.triggers.Trigger;
        clicked(mouseX: number, mouseY: number, button: number, isPressed: boolean): com.chattriggers.ctjs.api.triggers.Trigger;
        scrolled(mouseX: number, mouseY: number, scrollDelta: number): com.chattriggers.ctjs.api.triggers.Trigger;
        dragged(mouseXDelta: number, mouseYDelta: number, mouseX: number, mouseY: number, mouseButton: number): com.chattriggers.ctjs.api.triggers.Trigger;
        soundPlay(position: com.chattriggers.ctjs.api.vec.Vec3f, name: string, volume: number, pitch: number, category: net.minecraft.sound.SoundCategory, event: org.spongepowered.asm.mixin.injection.callback.CallbackInfo): com.chattriggers.ctjs.api.triggers.SoundPlayTrigger;
        tick(ticksElapsed: number): com.chattriggers.ctjs.api.triggers.Trigger;
        step(stepsElapsed: number): com.chattriggers.ctjs.api.triggers.StepTrigger;
        renderWorld(partialTicks: number): com.chattriggers.ctjs.api.triggers.Trigger;
        preRenderWorld(partialTicks: number): com.chattriggers.ctjs.api.triggers.Trigger;
        postRenderWorld(partialTicks: number): com.chattriggers.ctjs.api.triggers.Trigger;
        renderOverlay(): com.chattriggers.ctjs.api.triggers.Trigger;
        renderPlayerList(event: org.spongepowered.asm.mixin.injection.callback.CallbackInfo): com.chattriggers.ctjs.api.triggers.EventTrigger;
        drawBlockHighlight(position: BlockPos, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.EventTrigger;
        gameLoad(): com.chattriggers.ctjs.api.triggers.Trigger;
        gameUnload(): com.chattriggers.ctjs.api.triggers.Trigger;
        command(...args: string[]): com.chattriggers.ctjs.api.triggers.Trigger;
        guiOpened(screen: net.minecraft.client.gui.screen.Screen, event: org.spongepowered.asm.mixin.injection.callback.CallbackInfo): com.chattriggers.ctjs.api.triggers.EventTrigger;
        guiClosed(screen: net.minecraft.client.gui.screen.Screen): com.chattriggers.ctjs.api.triggers.Trigger;
        dropItem(item: Item, entireStack: boolean, event: org.spongepowered.asm.mixin.injection.callback.CallbackInfo): com.chattriggers.ctjs.api.triggers.EventTrigger;
        messageSent(message: string, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.EventTrigger;
        itemTooltip(lore: TextComponent[], item: Item, event: org.spongepowered.asm.mixin.injection.callback.CallbackInfo): com.chattriggers.ctjs.api.triggers.EventTrigger;
        playerInteract(interaction: com.chattriggers.ctjs.api.entity.PlayerInteraction, interactionTarget: Entity | Block | Item, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.EventTrigger;
        entityDamage(entity: Entity, attacker: PlayerMP): com.chattriggers.ctjs.api.triggers.Trigger;
        entityDeath(entity: Entity): com.chattriggers.ctjs.api.triggers.Trigger;
        guiRender(mouseX: number, mouseY: number, screen: net.minecraft.client.gui.screen.Screen): com.chattriggers.ctjs.api.triggers.Trigger;
        guiKey(char: String, keyCode: number, screen: net.minecraft.client.gui.screen.Screen, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.EventTrigger;
        guiMouseClick(mouseX: number, mouseY: number, mouseButton: number, isPressed: boolean, screen: net.minecraft.client.gui.screen.Screen, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.EventTrigger;
        guiMouseDrag(mouseXDelta: number, mouseYDelta: number, mouseX: number, mouseY: number, mouseButton: number, screen: net.minecraft.client.gui.screen.Screen, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.EventTrigger;
        packetSent(packet: net.minecraft.network.packet.Packet<unknown>, event: org.spongepowered.asm.mixin.injection.callback.CallbackInfo): com.chattriggers.ctjs.api.triggers.PacketTrigger;
        packetReceived(packet: net.minecraft.network.packet.Packet<unknown>, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.PacketTrigger;
        serverConnect(): com.chattriggers.ctjs.api.triggers.Trigger;
        serverDisconnect(): com.chattriggers.ctjs.api.triggers.Trigger;
        renderEntity(entity: Entity, partialTicks: number, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.RenderEntityTrigger;
        renderBlockEntity(blockEntity: BlockEntity, partialTicks: number, event: CancellableEvent): com.chattriggers.ctjs.api.triggers.RenderBlockEntityTrigger;
        postGuiRender(mouseX: number, mouseY: number, screen: net.minecraft.client.gui.screen.Screen): com.chattriggers.ctjs.api.triggers.Trigger;
        spawnParticle(particle: Particle, event: org.spongepowered.asm.mixin.injection.callback.CallbackInfo): com.chattriggers.ctjs.api.triggers.EventTrigger;
      }

      /**
       * Registers a new trigger and returns it.
       */
      function register<T extends keyof RegisterTypes>(
        name: T, 
        cb: (...args: Parameters<RegisterTypes[T]>) => void,
      ): ReturnType<RegisterTypes[T]>;

      /**
       * Cancels the given event
       */
      function cancel(event: CancellableEvent | org.spongepowered.asm.mixin.injection.callback.CallbackInfo);

      /**
       * Creates a custom trigger. `name` can be used as the first argument of a
       * subsequent call to `register`. Returns an object that can be used to
       * invoke the trigger.
       */
      function createCustomTrigger(name: string): { trigger(...args: unknown[]) };
      
      function easeOut(start: number, finish: number, speed: number, jump?: number): number;
      function easeColor(start: number, finish: number, speed: number, jump?: number): java.awt.Color;

      function print(message: string, color?: java.awt.Color);
      function println(message: string, color?: java.awt.Color, end?: string);

      const console: {
        assert(condition: boolean, message: string);
        clear();
        count(label?: string);
        debug(args: unknown[]);
        dir(obj: object);
        dirxml(obj: object);
        error(...args: unknown[]);
        group(...args: unknown[]);
        groupCollapsed(...args: unknown[]);
        groupEnd(...args: unknown[]);
        info(...args: unknown[]);
        log(...args: unknown[]);
        table(data: object, columns?: string[]);
        time(label?: string);
        timeEnd(label?: string);
        trace(...args: unknown[]);
        warn(...args: unknown[]);
      };

""".trimIndent()

fun wrapInPrologue(types: String): String {
    return buildString {
        append(prologue)
        append('\n')
        append(types.prependIndent("  "))
        append("\n}\n")
    }
}
