# 3.0.0 Migration Guide

3.0.0 includes a massive list of exciting features and breaking changes. This document will serve as a guide for module authors updating their modules.

## New Features

### Mixins

The most exciting new feature is Mixins! Module authors now have the ability to write Mixins in their modules. This replaces the old ASM system, which was clunky and hard to work with. Check out the [Mixins wiki page](https://github.com/ChatTriggers/ctjs/wiki/Dynamic-Mixins) to get started. Existing injected callbacks are safely rebound to the current `/ct reload` generation. Adding a new mixin or injector still requires a full Minecraft restart; hot-adding a transform after its target class has loaded is not supported.

### Automatic Remapping

For all code, both normal module code and Mixin code, Rhino (the JavaScript engine CT uses) will now automatically remap all MC names (fields, methods, and classes)! This means you no longer have to use obfuscated name _at all_ when writing modules. This is a huge ergonomic win which will greatly increase readability for code which goes outside the built-in API that CT provides. 

### Custom Triggers

Libraries can now provide their own custom trigger types. Here is an example:

```js
// MyLib/index.js
const customTrigger = createCustomTrigger('mylib:seconds');

let numSeconds = 0;
register('step', () => {
  customTrigger.trigger(numSeconds++);
});

// MyModule/index.js
register('mylib:seconds', second => {
  ChatLib.chat(`Second ${second}`);
});
```

`MyModule` of course needs to depend on `MyLib` so it runs first and registers its trigger. Custom trigger names must be unique, so it is a good idea to prefix them with a unique identifier (in the example, this is `mylib:`). They are also case-insensitive like builtin triggers. 

If you need to allow users to use `cancel`, you should create a `CancellableEvent` and pass it in as the last parameter. After calling `trigger`, you can check `event.isCancelled()`

These custom triggers are designed to be used with Mixins to provide triggers for arbitrary MC functionality.

### API Additions

- `Sound` now supports playing sounds from Minecraft. To do this, set `source` to something like `minecraft:entity.experience_orb.pickup`
- Added a new `BossBars` API
- Added `Client.copy(text: string)`
- Added `Sound.setLoop()` and `Sound.setLoopDelay()`
- Added middle-click tracking to `CPS`

TODO: Add more things here?

## Breaking Changes

This update includes _many_ API changes that will break a wide range of modules. CTJS Reloaded 3.0.0 targets Minecraft 26.1.2 using its modern, unobfuscated API rather than the Minecraft 1.8.9 runtime. Minecraft changed many APIs and concepts across that gap. We are also taking this opportunity to make the CTJS APIs more consistent. Both changes result in the compatibility notes below.

### Large API Changes

These are API changes that typically involve consistency and affect multiple different APIs.

- All MC wrapper classes (`Entity`, `Player`, etc.) now all have a `.toMC()` method that exposes the underlying Minecraft value. This change makes these APIs much more uniform, as previously some adhered to this scheme and others didn't.
- We have removed all server-side methods, which wouldn't have done anything anyway if used outside of singleplayer. Specifically, the following methods have been removed:
  - `Entity`: `setAir()`, `dropItem()`, `setIsOutsideBorder()`, `setPosition()`, `setAngles()`, `setOnFire()`, `extinguish()`, `move()`, `setIsSilent()`, `addVelocity()`, `setIsSneaking()`, `setIsSprinting()`, and `setIsInvisible()`
  - `LivingEntity`: `addPotionEffect()`, `clearPotionEffects()`, `setHP()`, and `setAbsorption()`
  - `Settings`: `setDifficulty()`
  - `World`: `getSeed()` and `getType()`
  - `Particle`: `multiplyVelocity()`
- Methods which use to return an enum as a string or number now return an enum

### API-Specific Changes

Here is a list of targeted changes for various different APIs:

- Triggers
  - The following less-used trigger have been removed: `screenshotTaken`, `pickupItem`, `chatComponentClicked`, `chatComponentHovered`, `renderSlot`, `guiDrawBackground`, `renderBossHealth`, `renderDebug`, `renderCrosshair`, `renderHotbar`, `renderExperience`, `renderArmor`, `renderHealth`, `renderFood`, `renderMountHealth`, `renderAir`, `renderPortal`, `renderJumpBar`, `renderChat`, `renderHelmet`, `renderHand`, `renderScoreboard`, `renderTitle`, `preItemRender`, `renderItemIntoGui`, `noteBlockPlay`, `noteBlockChange`, `renderItemOverlayIntoGui`, `renderSlotHighlight`, `postRenderEntity`, and `postRenderTileEntity`
    - All of these triggers had less than 10 uses over all releases on our website. If you maintain one of the few releases who used one of these triggers, they can be replaced with a custom Mixin.
  - The following triggers have been removed in favor of other triggers: `attackEntity`, `hitBlock`, and `blockBreak` (replaced by `playerInteract`); `guiMouseRelease` (replaced by a parameter in `guiMouseClick`)
  - `playerInteract` now passes the interacted-with object as the second argument instead of the object's position (which can be retrieved via a method on the object wrapper, which is either an `Entity`, `Block`, or `Item`). The list of events has also changed.
  - `guiMouseClick` now takes a boolean after the mouse button which indicates if the mouse button was pressed (`true`) or released (`false`)
  - `guiMouseDrag` now takes two mouse deltas as its first two arguments. The rest of the arguments are unchanged.
  - `guiOpened` now takes the opened `Screen` as its first argument.
  - `renderTileEntity` has been renamed to `renderBlockEntity`, and no longer passes in the position as an argument (access it by calling `BlockEntity.getBlockPos()`)
  - `ClassFilterTrigger`: Removed `setPacketClass` and `setPacketClasses`. Use `setFilteredClass` and `setFilteredClasses` instead
  - The full message for `chat` triggers is no longer accessed with `EventLib` (which no longer exists). Instead, use `event.message`, which will return a `TextComponent`. This has the `getFormattedText()` and `getUnformattedText()` methods, which replace the second parameter of the old `EventLib` method
  - `serverConnect` and `serverDisconnect` no longer pass an event as the third parameter
  - `scrolled` now passes in the actual scroll amount, not just -1 or 1 to indicate a direction
  - `dropItem` takes different parameters:
    - It now takes the `Item`, a boolean to indicate whether the user is dropping just 1 (`false`) or the entire stack (`true`), and the event which can still be cancelled.
    - It previously took a `PlayerMP` (which was always the player since this is a client mod) and the item's position/motion, both of which can be obtained by methods on `Item`
  - `renderEntity` no longer takes the entity's position as an argument. Instead, call `Entity.getPos()`
  - `spawnParticle` no longer passes in the particle type (which no longer exists in the MC codebase). Instead, the class can be access from the particle wrapper's underlying MC type
  - `renderOverlay` no longer passes in the event, as it was unused previously
  - `itemTooltip` now receives a list of `TextComponent` objects instead of a list of strings
  - Removed all Trigger classes from the global namespace
  - Removed `CancellableEvent` from the global scope
- `Message`/`TextComponent`
  - `Message` has been removed, and its primary functionality (i.e. `chat()`/`actionBar()`) has been added to `TextComponent`
  - `TextComponent` has been heavily changed such that it can be easily introspected. It now implements `List<NativeObject>`, and each object is of the form `{ text: '...', bold: true, underline: true, ... }`. This form can also be used to construct and create new `TextComponent`s
  - `TextComponent` is now immutable. Methods such as `withText()` can be used to return a modified `TextComponent` based on the original
- The `/ct` command
  - Removed `/ct copy`. Replace this with `Client.copy(text: String)`
  - Removed the following aliases: 
    - `reload` (an alias of `load`)
    - `file` (an alias of `files`)
    - `settings` and `setting` (an alias of `config`)
    - `sim` (an alias of `simulate`)
  - `/ct files` now opens the modules folder instead of its parent folder
  - `/ct console` now opens the JS console. Use `/ct console general` to open the general console
- `Entity`
  - Removed `getRider()`. Entities can have multiple riders, so this method doesn't make sense. Replace all usages with the `getRiders()` method
  - Removed `isAirborne()`, which no longer exists in the MC API
  - `getDimension()` now returns an `Entity.DimensionType` enum value instead of an int
- `LivingEntity`
  - Name changed from `EntityLivingBase`
  - Renamed `getItemInSlot()` to `getStackInSlot()`, which matches the method of the same name in `Inventory`
- `TileEntity` is now `BlockEntity`
- `PotionEffect`
  - This API has been completely reworked, and is now similar to the `Block` API. The `PotionEffect` object represents a specific instance of an effect, and will have a `PotionEffectType`, which represents the kind of effect it is. Check out the docs for more info on how to use this new API
  - Renamed `isDurationMax()` to `isInfinite()`
- `Item`
  - This API has also been completely reworked, similarly to `PotionEffect` and `Block`. It has been split into an `Item` class which represents a single stack of items in an inventory, and an `ItemType` class which represents the type of the `Item`.
  - Renamed `isDamagable()` to `isDamageable()`, fixing the typo
  - Removed `getRawNBT()`, prefer using `getNBT()` which gives access to a wide range of powerful NBT-related APIs
  - You can no longer wrap empty ItemStacks. Creating an Item with an empty stack will throw an error. Use `Item.fromMC` instead.
- `NBTTagList.removeTag()` now wraps the removed element in CT's NBT wrappers
- `NBTTagCompound.getTag()` and `NBTTagCompound.getTagList()` now returns a wrapped version instead of the raw MC version
- `Chunk`
  - Renamed `getAllTileEntities()` to `getAllBlockEntities()`
  - Renamed `getAllTilesEntitiesOfType()` to `getAllBlockEntitiesOfType()`
- `Block`
  - Removed `getMetadata()` as blocks no longer have this in newer MC versions
  - Renamed `isPowered()` and `getRedstoneStrength()` to `isReceivingPower()` and `getReceivingPower()`, respectively, to differentiate them from the new methods `isEmittingPower(BlockFace)` and `getEmittingPower(BlockFace)`
- `BlockFace`
  - Renamed `fromMCEnumFacing()` to `fromMC()`
  - Enum values are now UPPER_CASE
- `BlockType`: Removed `getDefaultMetadata()` and `getHarvestLevel()`
- `Scoreboard`
  - Remove `Scoreboard.getScoreboardTitle()` in favor of the less verbose `Scoreboard.getTitle()`
  - `Scoreboard.getTitle()` now returns `TextComponent` instead of `String`
  - `Score`
    - is now mutable. You can now edit the score, name, number format, and team
    - `getPoints`/`setPoints` are renamed to `getScore`/`setScore`
  - Added `addLine()`, `createTeam()`, `removeIndex()`, `removeScores()` methods
  - `getLines` now actually sorts by descending instead of ascending
- `Book` now uses `TextComponent` instead of `Message`
- `Settings`
  - Renamed all methods in the `skin` object to indicate they return whether the part is enabled, not the actual part themselves (i.e. `getCape()` -> `isCapeEnabled()`)
  - Renamed `video.getGraphics()` to `video.getGraphicsMode()`
  - Removed `video.get3dAnaglyph()` (3D Anaglyph no longer exists in MC)
  - The following methods have had their return values changed to enums:
    - `video.getGraphicsMode()` now returns `Settings.GraphicsMode` instead of `number`
    - `video.getClouds()` now returns `Settings.CloudRenderMode` instead of `number`
    - `video.getParticles()` now returns `Settings.ParticlesMode` instead of `number`
    - `chat.getVisibility()` now returns `Settings.ChatVisibility` instead of `string`
- `ChatLib`
  - `clearChat()` no longer takes any chat line IDs, and instead will always clear the chat. To selectively-delete message using their ID, use `deleteChat(id: number)`
  - Removed `getChatMessage()`. Instead, you can access the entire message as a `TextComponent` via `event.message`
- `Player`
  - Removed `getRawYaw()` as it provided no extra value
  - `getUUID()` now returns the `UUID` object instead of a `string`
  - `lookingAt()` now returns `null` when looking at nothing instead of a `BlockType`
  - `draw()` now takes an object to align with `Renderer.drawPlayer()`
- `PlayerMP.draw()` now takes an object to align with `Renderer.drawPlayer()`
- `World`
  - Removed all `Sound`-related methods. Instead, use the `Sound` class
  - `getDifficulty()` now returns `Settings.Difficulty?`
  - Renamed `getAllTileEntities()` to `getAllBlockEntities()`
  - Renamed `getAllTilesEntitiesOfType()` to `getAllBlockEntitiesOfType()`
- `Sound`
  - Removed `priority`
  - The `attenuation` field is now the distance. Added an `attenuationType` field which takes a `Sound.AttenuationType`
  - `setCategory` now takes a `Sound.Category`
- `KeyBind.register...()` methods now return the `KeyBind` instead of the trigger. Use the respective `unregister...()` methods if necessary.
- `Display`
  - Removed `DisplayLine`, lines are now instances of `Text`
  - The user must now call the `draw()` method manually. This allows it to be rendered in any arbitrary trigger
- `Text`
  - Added background color and alignment to replace the functionality in `DisplayLine`
- `Image`
  - Remove deprecated constructors. Instead, use the static helper methods: `Image.fromFile(File)`, `Image.fromFile(string)`, `Image.fromAsset(string)`, and `Image.fromUrl(String[, String])`
- `Renderer`/`Tessellator`
  - New modules should use `Renderer3d`. The legacy global `Tessellator` remains available as a buffered compatibility adapter.
  - Legacy vertices are collected until `draw()`, where POSITION/COLOR/UV use is analyzed and missing color or UV attributes receive safe defaults. This avoids modern `Missing elements in vertex: UV0` failures.
  - `LINE_LOOP` is converted to `LINE_STRIP` with the first vertex appended, convex `POLYGON` uses `TRIANGLE_FAN`, and `QUAD_STRIP` is expanded into legacy-winding quads.
  - Legacy `normal()` values are accepted but ignored by the generic compatibility pipeline. `POINTS` is not drawn. These remain non-blocking P2 limitations.
  - `Renderer.color()` has been replaced with `Renderer.getColor()`. The new `color()` method is used to color the vertices instead
  - Removed `drawShape`. Instead, create a `Shape` and invoke its `draw()` method
  - `begin()` now no longer translates to the player's camera position. Instead, use `Renderer.translateToPlayer()`
  - `begin()` now takes a `Renderer.VertexFormat` as an optional second argument
  - `drawString` now takes an optional `color` parameter as its 4th argument
  - `drawPlayer` now takes an object, as even more parameters were added. Check the javadocs for a full description of the parameters
  - Removed `drawLine()`'s `drawMode` argument
  - Removed `drawCircle()`'s `drawMode` argument
  - Removed `getDrawMode()` and `setDrawMode()`. Pass the drawMode to `begin`
  - Removed `retainTransforms()`
  - Most of `Renderer3d`'s rendering should be in `postRenderWorld`
  - Removed `enableAlpha()` and `disableAlpha()` as they do nothing on modern versions
- `Gui`/`GuiHandler`
  - `GuiHandler.openGui(value)` remains as a compatibility facade and delegates to `Client.currentGui.set()`. It accepts a CTJS `Gui`, raw modern `Screen`, supported screen wrapper, or `null`; `null` closes the current GUI.
  - Removed `isControlDown()`, `isAltDown()`, and `isShiftDown()`. Instead, use the method that already exist on `Screen`: `hasControlDown()`, `hasAltDown()`, and `hasShiftDown()`
  - The various `register...()` methods now return the `Gui` instance for method chaining. Use the `unregister...()` methods for unregistering the respective triggers.
  - The `mouseDragged` trigger no longer takes `timeSinceLastClick`. If you _really_ need this, you can track it yourself
  - `addButton` now returns the ID instead of returning the `Gui` instance. This ID is used in various button APIs, primarily to indicate which button is clicked. This is a change in the MC API that we propogated to our API.
  - Removed a bunch of random draw method that didn't really belong in the class. They delegated to existing methods on `Screen`, so if you really want to, you can still call them, albeit with slightly different names and parameters.
- `TabList`
  - Renamed `getHeaderMessage()` to `getHeaderComponent()`, and it now returns a `TextComponent` instead of a `Message`
  - Renamed `getFooterMessage()` to `getFooterComponent()`, and it now returns a `TextComponent` instead of a `Message`
  - Added `addName()`, `getList()`, and `removeNames()`
  - `getNames()` now returns a list of `Name`
  - Added `Name`
    - acts similarly to `Scoreboard.Score`, with the following methods
    - `getLatency()`, `setLatency()`, `getName()`, `setName()`, `getTeam()`, `setTeam()`, and `remove()`
- `Team`
  - `Team.getNameTagVisibility()` and `Team.getDeathMessageVisibility()` now return a `Team.Visibility` instead of a string
  - Added `setColor()`
- `Client`
  - `getChatGUI` was renamed to `getChatGui` to match the naming of `getTabGui`
- `Server.getPing()` now returns -1 if not in a world
- `Config.modulesFolder` remains as a read-only compatibility alias. New code should use `ChatTriggers.MODULES_FOLDER` or the string `"./config/ChatTriggers/modules"`.
- Renamed `ChatTriggers.loadCT()` and `ChatTriggers.unloadCT()` to `load()` and `unload()`
- Provided JS API: 
  - Split `print` into `print` and `println`. `print` will no longer emit a trailing newline

### Misc Changes

- The assets directory has changed from `config/ChatTriggers/images` to `config/ChatTriggers/assets`

### Legacy runtime boundaries

- The global `Thread` is a compatibility wrapper. `start()`, `sleep()`, and `currentThread()` are supported. Unsafe legacy lifecycle operations such as `stop()`, `suspend()`, `resume()`, `destroy()`, and `interrupt()` are no-ops; use `Client.scheduleTask()` or `setTimeout()` for reload-owned work.
- Global legacy `Renderer.color` value construction is not restored. Use `Renderer.getColor()` to construct a packed color; `Renderer.color()` applies vertex color.
- Old packet classes, old raw `net.minecraft.*` class names, Forge APIs, LWJGL2 fixed-function APIs, and Rhino binary ABI are not emulated. Modules using those boundaries require a source-level manual port.
