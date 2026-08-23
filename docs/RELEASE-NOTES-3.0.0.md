# CTJS Reloaded 3.0.0 Release Notes

CTJS Reloaded 3.0.0 brings ChatTriggers scripting to Minecraft 26.1.2 on Fabric. This release is a substantial runtime and API migration, not a binary emulator for Minecraft 1.8.9. Modules using CTJS APIs and the documented compatibility surfaces can migrate incrementally; modules tied directly to old Minecraft, Forge, LWJGL2, or Rhino internals require a source port.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.2+26.1.2 or newer compatible build
- Fabric Language Kotlin 1.13.11+kotlin.2.3.21 or newer compatible build
- Java 25

## Highlights

### Modern Minecraft and CTJS API

The codebase now uses current, unobfuscated Minecraft 26.1.2 APIs. ChatLib, Client, Player, World, entities, inventories, items, blocks, GUI, Sound, Image, commands, scoreboards, tab lists, Renderer, and Renderer3d were adapted to the modern client. Public Kotlin ABI and JS globals are validated separately so changes to either surface are visible in CI.

### Deterministic `/ct reload`

Every module runtime has a generation owner. Reload transitions the old generation from ACTIVE to STOPPING to DEAD, detaches its work, publishes the new generation only after setup succeeds, and prevents old callbacks from executing afterward. Scheduled tasks, timeouts, triggers, commands, key bindings, CTJS-owned GUI/Toast callbacks, Image/Sound resources, and custom scoreboard/tab-list state follow the same lifecycle.

Ten consecutive reloads were tested without old-generation callback, task, timeout, resource, UI, command, or render-state leakage.

### Module JAR and import lifecycle

Module JARs use a closeable classloader owned by the active generation. Updating a JAR at the same path loads its new classes, and stopping the generation releases JAR handles. Deleting or reimporting one module does not close shared/bootstrap loaders or affect another module.

Module dependency ordering, cycle and setup failures, dynamic imports, CommonJS `module.exports`, named exports, default fallback, delete/reimport, and owned asset updates now have automated contract coverage.

### Trigger contracts

StepTrigger cannot hang on extreme FPS values. Entity death and block highlight callbacks use CTJS wrappers. Tooltip lore is mutable and cancelable, GUI opening is genuinely cancelable, and disconnect events are exactly-once. GUI key, item drop, SoundSource, and BreakBlock edge behavior is documented and registered in the complete TriggerType contract table.

### Rendering and GUI

Renderer and Renderer3d implement scoped culling with nested and exception-safe restoration. The existing blend, depth, texture, matrix, Shape, Gui, and Tessellator boundaries remain intact.

The legacy `GuiHandler.openGui(...)` facade delegates to the modern current-GUI API and accepts CTJS Gui, supported Screen wrappers, raw modern Screen instances, and null to close. Elementa and Vigilance GUI paths were verified in Prism.

### Buffered legacy Tessellator

The compatibility Tessellator buffers legacy vertices until `draw()`, determines the attributes actually used, selects a safe modern vertex format, and supplies required defaults. It supports position, color, texture coordinates, and their combinations without UV0 build failures. LINE_LOOP, POLYGON, and QUAD_STRIP are converted to supported modern topologies. Texture enable/disable calls are compatibility-session intent and do not mutate global modern Renderer state.

### Additional compatibility

Safe facades and aliases cover legacy Message usage, ChatLib helpers, selected World and Config names, Keyboard constants, Thread and Hand globals, string formatting helpers, CommonJS default fallback, and selected deprecated getters. These aliases delegate to current APIs and do not recreate removed Minecraft or JVM binary contracts.

### Reliability and validation

FileLib archive extraction rejects traversal and malformed output and cleans partial files. Console queues, sockets, worker shutdown, reconnect, and eval futures are bounded. HTTP and input streams used by statistics, module updates, and Image loading close and disconnect consistently.

The 3.0.0 release candidate passed 104 automated tests, ABI and JS-global checks, ten-generation reload stress, command/module/classloader contracts, a 300-frame Prism runtime, and Compat Runtime A for GuiHandler, Elementa, Vigilance, and buffered Tessellator.

## Known limitations

The following are explicitly outside the 3.0.0 compatibility contract:

- Legacy packet classes and constructors.
- Direct old `net.minecraft.*` classes, obfuscated names, and removed methods.
- Forge APIs and events.
- LWJGL2 fixed-function OpenGL and GLU APIs.
- Binary compatibility with libraries compiled against an old Rhino ABI.
- The legacy global meaning of `Renderer.color`.
- Adding a new dynamic Mixin transform without restarting Minecraft.

These cases require module source migration. They are not treated as CTJS core release failures.

## Known future improvements

The following P2 work is intentionally non-blocking for 3.0.0:

- Tessellator normal-aware pipeline support.
- Tessellator POINTS rendering.
- Richer Item component wrappers.
- Additional Chunk API coverage.
- Wider console standards conformance.

See [MIGRATION.md](MIGRATION.md) for module-porting guidance and [CHANGELOG.md](CHANGELOG.md) for the complete change list.
