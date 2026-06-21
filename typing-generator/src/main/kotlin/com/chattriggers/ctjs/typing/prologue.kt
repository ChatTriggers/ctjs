package com.chattriggers.ctjs.typing

private val providedTypes = mutableMapOf(
    "FileLib" to "com.chattriggers.ctjs.api.FileLib",
    "CustomKeyMapping" to "com.chattriggers.ctjs.api.CustomKeyMapping",
    "CustomCommand" to "com.chattriggers.ctjs.api.CustomCommand",
    "CancellableEvent" to "com.chattriggers.ctjs.api.triggers.CancellableEvent",
    "TriggerRegister" to "com.chattriggers.ctjs.engine.Register",
    "Thread" to "com.chattriggers.ctjs.engine.WrappedThread",
    "Priority" to "com.chattriggers.ctjs.api.triggers.Trigger\$Priority",
    "CTJS" to "com.chattriggers.ctjs.CTJS",
    "Console" to "com.chattriggers.ctjs.engine.Console",
)

val prologue = """
    /// <reference no-default-lib="true" />
    /// <reference lib="es2015" />
    export {};
    
    declare interface Number {
      easeOut(to: number, speed: number, jump: number): number;
    }
    
    interface RegisterTypes {
        renderOverlay(ctx: net.minecraft.client.gui.GuiGraphicsExtractor, tickCounter: net.minecraft.client.DeltaTracker);
        chat(message: net.minecraft.network.chat.Component, event: CancellableEvent);
        actionBar(message: net.minecraft.network.chat.Component, event: CancellableEvent);
        messageSent(message: string, isCommand: boolean, event: CancellableEvent);
        tick();
        
        renderLevelExtraction(ctx: net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext)
        renderEndMain(ctx: net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext);
        renderBeforeGizmos(ctx: net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext);
        renderAfterTranslucentTerrain(ctx: net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext);
        renderAfterSolidFeatures(ctx: net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext);
        renderAfterTranslucentFeatures(ctx: net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext);
        renderBeforeTranslucentTerrain(ctx: net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext);
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
      function sync(func: () => void, lock: unknown): void;
  
      /**
       * Runs `func` after `delayInMs` milliseconds. A new thread is spawned to accomplish
       * this, which means this function is asynchronous. If you want to avoid the Thread
       * instantiation, use `Client.scheduleTask(delayInTicks, func)`.
       */
      function setTimeout(func: () => void, delayInMs: number): void;
      
${
    providedTypes.entries.joinToString("") { (name, type) ->
        "const $name: typeof $type;\ninterface $name extends $type {}\n"
    }.prependIndent("      ")
}

      /**
       * Registers a new trigger and returns it.
       */
      function register<T extends keyof RegisterTypes>(
        name: T, 
        cb: (...args: Parameters<RegisterTypes[T]>) => void,
      ): com.chattriggers.ctjs.api.triggers.Trigger;

      /**
       * Cancels the given event
       */
      function cancel(event: CancellableEvent | org.spongepowered.asm.mixin.injection.callback.CallbackInfo): void;

      /**
       * Creates a custom trigger. `name` can be used as the first argument of a
       * subsequent call to `register`. Returns an object that can be used to
       * invoke the trigger.
       */
      function createCustomTrigger(name: string): { trigger(...args: unknown[]) };
      
      function easeOut(start: number, finish: number, speed: number, jump?: number): number;

      function print(message: string, color?: java.awt.Color): void;
      function println(message: string, color?: java.awt.Color, end?: string): void;

      const console: {
        assert(condition: boolean, message: string): void;
        clear(): void;
        count(label?: string): void;
        debug(args: unknown[]): void;
        dir(obj: object): void;
        dirxml(obj: object): void;
        error(...args: unknown[]): void;
        group(...args: unknown[]): void;
        groupCollapsed(...args: unknown[]): void;
        groupEnd(...args: unknown[]): void;
        info(...args: unknown[]): void;
        log(...args: unknown[]): void;
        table(data: object, columns?: string[]): void;
        time(label?: string): void;
        timeEnd(label?: string): void;
        trace(...args: unknown[]): void;
        warn(...args: unknown[]): void;
      };
""".trimIndent()
