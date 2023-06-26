# 3.0.0 Migration Guide

3.0.0 include a massive list of exciting features and breaking changes; this document
will serve as a guide for module authors updating their modules.

## New Features

#### Mixins

The most exciting new feature is Mixins! Module authors now have the ability to write
Mixins in their modules. This replaces the old ASM system, which was clunky and hard
to work with. Check out the 
[Mixins wiki page](https://github.com/ChatTriggers/ctjs/wiki/Dynamic-Mixins) to get
started.

#### Automatic Remapping

For all code, both normal module code and Mixin code, Rhino (the JavaScript engine CT
uses) will now automatically remap all MC names (fields, methods, and classes)! This
means you no longer have to use obfuscated name _at all_ when writing modules. This is
a huge ergonomic win which will greatly increase readability for code which goes
outside the built-in API that CT provides. 

## Breaking Changes

This update includes _many_ API changes that will break a wide range of modules. Some
of this is due to the fact that the mod has been updated from 1.8.9, released in 2015, 
to 1.19, released in 2022. That's a 7-year jump, and Minecraft changed a lot during 
that time, changing many APIs and concept in their codebase. However, we are also
taking this opportunity to update many of our APIs to be a bit more polished. Both of
these result in quite the list of breaking changes:

### Large API Changes

These are API changes that typically involve consistency and affect multiple different
APIs.

- All MC wrapper classes (`Entity`, `Player`, etc.) now have their wrapped object named `mcValue`, and will have a `.toMC()` method that exposes this value. This change makes these APIs much more uniform, as previously some adhered to this scheme and others didn't.
- We have removed all server-side methods, which wouldn't have done anything anyway if used on a server. Specifically, the following methods have been removed:
  - `Entity`: `setAir()`, `dropItem()`, `setIsOutsideBorder()`, `setPosition()`, `setAngles()`, `setOnFire()`, `extinguish()`, `move()`, `setIsSilent()`, `addVelocity()`, `setIsSneaking()`, `setIsSprinting()`, and `setIsInvisible()`
  - `LivingEntity`: `addPotionEffect()`, `clearPotionEffects()`, `setHP()`, and `setAbsorption()`
  - `Settings`: `setDifficulty()`

### API-Specific Changes

Here is a list of targeted changes for various different APIs:

- Triggers
  - The following less-used trigger have been removed: `screenshotTaken`, `pickupItem`, `chatComponentClicked`, `chatComponentHovered`, `renderSlot`, `guiDrawBackground`, `renderBossHealth`, `renderDebug`, `renderCrosshair`, `renderHotbar`, `renderExperience`, `renderArmor`, `renderHealth`, `renderFood`, `renderMountHealth`, `renderAir`, `renderPortal`, `renderJumpBar`, `renderChat`, `renderHelmet`, `renderHand`, `renderScoreboard`, `renderTitle`, `preItemRender`, `renderItemIntoGui`, `noteBlockPlay`, `noteBlockChange`, `renderItemOverlayIntoGui`, `renderSlotHighlight`, `postRenderEntity`, and `postRenderTileEntity`
  - The following triggers have been removed in favor of other triggers: `attackEntity`, `hitBlock`, and `blockBreak` (replaced by `playerInteract`); `guiMouseRelease` (replaced by a parameter in `guiMouseClick`)
  - `renderTileEntity` has been renamed to `renderBlockEntity`
  - `ClassFilterTrigger`: Removed `setPacketClass` and `setPacketClasses`. Use `setFilteredClass` and `setFilteredClasses` instead
  - The full message for `chat` triggers is no longer accessed with `EventLib` (which no longer exists). Instead, use `event.message`, which will return a `UTextComponent`. This has the `getFormattedText()` and `getUnformattedText()` methods, which replace the second parameter of the old `EventLib` method
- The `/ct` command
  - Removed `/ct copy`. Replace this with `Client.copy(text: String)`
  - Removed the following aliases: 
    - `reload` (an alias of `load`)
    - `file` (an alias of `files`)
    - `settings` and `setting` (an alias of `config`)
    - `sim` (an alias of `simulate`)
- `Entity`
  - Removed `getRider()`. Entities can have multiple riders, so this method doesn't make sense. Replace all usages with the `getRiders()` method
  - Removed `isAirborne()`, which no longer exists in the MC API
  - `getDimension()` now returns an `Entity.DimensionType` enum value instead of an int
- `LivingEntity`
  - Name changed from `EntityLivingBase`
  - Renamed `getItemInSlot()` to `getStackInSlot()`, which matches the method of the same name in `Inventory`
- `PotionEffect`
  - This API has been completely reworked, and is now similar to the `Block` API. The `PotionEffect` object represents a specific instance of an effect, and will have a `PotionEffectType`, which represents the kind of effect it is. Check out the docs for more info on how to use this new API
  - Renamed `isDurationMax()` to `isInfinite()`
- `Item`
  - This API has also been completely rework, similarly to `PotionEffect` and `Block`. It has been split into an `Item` class which represents a single stack of items in an inventory, and an `ItemType` class which represents the type of the `Item`.
  - Renamed `isDamagable()` to `isDamageable()`, fixing the typo
  - Removed `getRawNBT()`, prefer using `getNBT()` which gives access to a wide range of powerful NBT-related APIs
- `NBTTagList.removeTag()` now wraps the removed element in CT's NBT wrappers
- `TileEntity` is now `BlockEntity`
- Remove `Scoreboard.getScoreboardTitle()` in favor of the less verbose `Scoreboard.getTitle()`
- `Book` now uses `UTextComponent` instead of `UMessage`
- `Settings`
  - Renamed all methods in the `skin` object to indicate they return whether the part is enabled, not the actual part themselves (i.e. `getCape()` -> `isCapeEnabled()`)
  - Renamed `video.getGraphics()` to `video.getGraphicsMode()`
  - Removed `video.get3dAnaglyph` (3D Anaglyph no longer exists in MC)
  - The following methods have had their return values changed to enums:
    - `video.getGraphicsMode()` now returns `Settings.GraphicsMode` instead of `number`
    - `video.getClouds()` now returns `Settings.CloudRenderMode` instead of `number`
    - `video.getParticles()` now returns `Settings.ParticlesMode` instead of `number`
    - `chat.getVisibility()` now returns `Settings.ChatVisibility` instead of `string`
- `Sound`
  - Removed `priority`
  - `loop` and `loopDelay` can now be changed after creating the `Sound` object with the respective setter methods
  - The `attenuation` field is now the distance. Added an `attenuationType` field which takes a `Sound.AttenuationType`
  - Added a `loopDelay` field which takes a `number`
  - `setCategory` now takes a `Sound.Category`
- `DisplayLine`
  - The `register...` methods now return the `DisplayLine` instance for method chaining. Use the `unregister...` methods to unregister the respective trigger.
- `Image`
  - Remove deprecated constructors. Instead, use the static helper methods: `Image.fromFile(File)`, `Image.fromFile(string)`, `Image.fromAsset(string)`, `Image.fromUrl(String[, String])`
- `ChatLib`
  - `ChatLib.clearChat()` no longer takes any chat line IDs, and instead will always clear the chat. To selectively-delete message using their ID, use `ChatLib.deleteChat(id: number)`
- `BlockFace`
  - The enum values are now UPPER_CASE

### Misc Changes

- The assets directory has changed from `config/ChatTriggers/images` to `config/ChatTriggers/assets`
