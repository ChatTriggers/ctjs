# Changelog

## 3.0.0 - 2026-08-24

CTJS Reloaded 3.0.0 is the first release of this port for Minecraft 26.1.2, Fabric Loader, and Java 25. It modernizes the Minecraft integration while retaining safe CTJS-facing compatibility where the old behavior can be represented without emulating Minecraft 1.8.9 or Forge.

### Platform and core API

- Ported the mod, access wideners, Mixins, build, and metadata to Minecraft 26.1.2 using modern unobfuscated Minecraft names.
- Updated Fabric Loader, Fabric API, Fabric Language Kotlin, Kotlin, Loom, Gradle, Essential libraries, and the Java toolchain used by this release.
- Migrated ChatLib, Client, Player, World, entity, inventory, item, block, GUI, sound, image, scoreboard, tab-list, command, and rendering integrations to current APIs.
- Added modern API and wrapper smoke coverage for world-null and world-loaded states.

### Reload and module lifecycle

- Added monotonically increasing runtime generations with ACTIVE, STOPPING, and DEAD ownership states.
- Made scheduled tasks, timeouts, triggers, commands, key bindings, UI callbacks, Toasts, Images, Sounds, and custom tab-list/scoreboard state reload-aware.
- Added snapshot-based, idempotent resource cleanup so an old generation cannot destroy new-generation resources.
- Added generation-owned module JAR classloaders. Reloading the same JAR path loads current classes and closes the old loader/JAR handles.
- Added stable generation-swapped dynamic Mixin callback trampolines. Adding a new Mixin transform still requires restarting Minecraft.
- Hardened module dependency ordering, cycle/setup failure handling, deletion/reimport, dynamic import, CommonJS `module.exports`, named exports, and default fallback behavior.
- Added module-owned asset manifests and stale/renamed asset cleanup with collision and user-modification protection.

### Trigger and command contracts

- Fixed finite StepTrigger timing and bounded catch-up behavior.
- Standardized callback wrappers for entity death and block highlight.
- Made item tooltip lore mutable and enforced tooltip cancellation.
- Made `guiOpened` cancellation effective for non-null opens without firing on GUI close.
- Enforced exactly-once world unload/server disconnect behavior.
- Fixed edge contracts for nullable GUI keys, drop routes, sound categories, and BreakBlock cancellation.
- Added table-driven coverage for every TriggerType callback shape and cancellation contract.
- Hardened static/dynamic command registration, aliases, conflicts, unregister, reconnect, and reload cleanup.

### Rendering and GUI

- Implemented scoped cull intent for Renderer and Renderer3d with nesting and exception-safe restoration.
- Preserved blend, depth, texture, matrix, GUI, Shape, and Tessellator state boundaries.
- Added the legacy `GuiHandler.openGui(...)` facade for CTJS Gui, supported Screen wrappers, raw modern Screen, and null close.
- Fixed GUI drag callbacks to expose `dx, dy, x, y, button` from the same input event.
- Verified GUI drawing, Image, Shape, text, and matrix operations in current render phases.

### Legacy CTJS compatibility

- Added a buffered legacy Tessellator adapter with delayed vertex-format selection and default attribute completion.
- Supports position/color/UV combinations without `Missing elements in vertex: UV0` failures.
- Converts legacy LINE_LOOP, POLYGON, and QUAD_STRIP modes to supported modern topology.
- Tracks legacy texture enable/disable intent within the compatibility session without changing global Renderer behavior.
- Added safe compatibility surfaces and aliases for Message, ChatLib, World, Config, Keyboard, Thread, Hand, string formatting helpers, and selected deprecated getters.
- Preserved current Renderer color semantics; the old global `Renderer.color` meaning is not restored.

### Reliability

- Hardened FileLib unzip against traversal, absolute entries, malformed archives, partial output, and Windows file locks.
- Bounded Console host/client queues and eval futures and made reconnect/shutdown deterministic.
- Standardized HTTP/input-stream timeout, close, disconnect, and contextual error behavior for statistics, module updates, and Image loading.
- Added generation, classloader, trigger, renderer, Tessellator, command, module, FileLib, console, stream, JS-global, ABI, and core smoke test suites.
- Release preparation baseline: 104 automated tests passing, ten consecutive reload stress passing, release runtime and Compat Runtime A passing.

### Known limitations

- Legacy packet classes and old direct `net.minecraft.*` classes are not emulated.
- Forge APIs are not provided; modules must migrate to Fabric or CTJS APIs.
- Removed LWJGL2 fixed-function and GLU APIs are not recreated.
- Binary compatibility with libraries compiled against an old Rhino ABI is not provided.
- Legacy global `Renderer.color` semantics are intentionally not restored.
- New dynamic Mixin transforms require a full Minecraft restart.

### Known future improvements

- Tessellator normal-aware pipeline support.
- Tessellator POINTS rendering.
- Richer Item component wrappers.
- Additional Chunk APIs.
- Broader console standards conformance beyond the bounded lifecycle contract.
