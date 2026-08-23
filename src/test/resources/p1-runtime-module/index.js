(function () {
    const JFile = Java.type("java.io.File");
    const FileWriter = Java.type("java.io.FileWriter");
    const Files = Java.type("java.nio.file.Files");
    const BufferedImage = Java.type("java.awt.image.BufferedImage");
    const TitleScreen = Java.type("net.minecraft.client.gui.screens.TitleScreen");
    const Character = Java.type("java.lang.Character");
    const MouseButtonEvent = Java.type("net.minecraft.client.input.MouseButtonEvent");
    const MouseButtonInfo = Java.type("net.minecraft.client.input.MouseButtonInfo");

    const moduleDir = new JFile("./config/ChatTriggers/modules/P1RuntimeProbe").getCanonicalFile();
    const markerFile = new JFile(moduleDir, "generation.txt");
    const eventsFile = new JFile(moduleDir, "events.jsonl");
    const completeFile = new JFile(moduleDir, "complete.json");
    const recordLock = new java.lang.Object();

    function readText(file) {
        if (!file.isFile()) return "";
        return String(Files.readString(file.toPath()));
    }

    function writeText(file, text, append) {
        const writer = new FileWriter(file, Boolean(append));
        try {
            writer.write(String(text));
        } finally {
            writer.close();
        }
    }

    const marker = (parseInt(readText(markerFile).trim(), 10) || 0) + 1;
    writeText(markerFile, String(marker), false);
    let sequence = 0;
    let started = false;
    let renderFrames = 0;
    let guiFrames = 0;
    let activeImage = null;
    let activeSound = null;

    const record = sync(function (event, data) {
        writeText(eventsFile, JSON.stringify({
            sequence: ++sequence,
            marker: marker,
            event: String(event),
            data: data === undefined ? null : data,
            wallMillis: Number(java.lang.System.currentTimeMillis())
        }) + "\n", true);
    }, recordLock);

    function fail(area, error) {
        record("failure", { area: String(area), error: String(error) });
    }

    function smokeWorldNull() {
        try {
            record("worldNull", {
                loaded: Boolean(World.isLoaded()),
                time: Number(World.getTime()),
                playerX: Number(Player.getX()),
                playerName: String(Player.getName()),
                currentGui: Client.currentGui.get() === null
            });
        } catch (error) {
            fail("worldNull", error);
        }
    }

    register("gameLoad", function () {
        record("gameLoad");
        if (marker === 1) {
            smokeWorldNull();
            Client.scheduleTask(20, function () {
                if (World.isLoaded()) {
                    startGeneration("alreadyInWorld");
                } else {
                    record("worldOpenRequested", "test");
                    Client.getMinecraft().createWorldOpenFlows().openWorld("test", function () {
                        fail("worldOpen", "test world failed to open");
                    });
                }
            });
        } else if (World.isLoaded()) {
            Client.scheduleTask(1, function () { startGeneration("gameLoadWorldPresent"); });
        }
    });

    register("gameUnload", function () { record("gameUnload"); });
    register("worldLoad", function () {
        record("worldLoad");
        startGeneration("worldLoad");
    });
    register("worldUnload", function () { record("worldUnload"); });

    register("chat", function (event) {
        record("chatTrigger");
        cancel(event);
    }).setCriteria("__CTJS_RELEASE_CHAT__");

    const staticCommand = register("command", function () {
        record("staticCommand");
    }).setName("p1command", true).setAliases("p1alias");

    Commands.registerCommand("p1dynamic", function () {
        Commands.exec(function () { record("dynamicCommand"); });
    });

    register("guiRender", function () {
        if (!started) return;
        try {
            Renderer.drawString("P1 GUI", 4, 4);
            if (activeImage !== null) activeImage.draw(4, 14, 2, 2);
            new Shape(Renderer.getColor(90, 180, 255, 100))
                .addVertex(8, 8).addVertex(16, 8).addVertex(16, 16).addVertex(8, 16).draw();
            Renderer.pushMatrix();
            Renderer.translate(1, 1, 0);
            Renderer.popMatrix();
            guiFrames++;
        } catch (error) {
            fail("guiRender", error);
        }
    });

    register("postGuiRender", function () {
        if (!started) return;
        try {
            Renderer.drawRect(Renderer.getColor(255, 255, 255, 20), 1, 1, 2, 2);
        } catch (error) {
            fail("postGuiRender", error);
        }
    });

    register("renderOverlay", function () {
        if (!started) return;
        renderFrames++;
        try {
            Renderer.disableCull();
            Renderer3d.begin(Renderer.DrawMode.TRIANGLES, Renderer.VertexFormat.POSITION_COLOR);
            Renderer3d.pos(Player.getX(), Player.getY() + 1, Player.getZ()).color(255, 0, 0, 120);
            Renderer3d.pos(Player.getX() + 0.2, Player.getY() + 1, Player.getZ()).color(0, 255, 0, 120);
            Renderer3d.pos(Player.getX(), Player.getY() + 1.2, Player.getZ()).color(0, 0, 255, 120);
            Renderer3d.draw();
            Renderer.enableCull();

            Renderer.drawString("P1 " + marker, 2, 2);
            new Shape(Renderer.getColor(40, 120, 220, 80))
                .addVertex(2, 12).addVertex(8, 12).addVertex(8, 18).addVertex(2, 18).draw();
            Tessellator.enableTexture2D()
                .begin(GL11.GL_TRIANGLES)
                .pos(12, 2, 0).tex(0, 0).color(255, 0, 0, 255)
                .pos(18, 2, 0).tex(1, 0).color(0, 255, 0, 255)
                .pos(12, 8, 0).tex(0, 1).color(0, 0, 255, 255)
                .draw();
            Tessellator.disableTexture2D()
                .begin(GL11.GL_LINE_LOOP)
                .pos(20, 2, 0).color(255, 255, 255, 255)
                .pos(26, 2, 0).color(255, 255, 255, 255)
                .pos(26, 8, 0).color(255, 255, 255, 255)
                .draw();
            if (activeImage !== null) activeImage.draw(30, 2, 2, 2);
        } catch (error) {
            fail("render", error);
            try { Renderer3d.draw(); } catch (ignored) {}
            try { Renderer.enableCull(); } catch (ignored2) {}
        }

        if (marker === 4 && renderFrames === 300) {
            finish();
        }
    });

    function exerciseGui() {
        try {
            const gui = new Gui();
            gui.registerOpened(function () { record("guiOpened"); });
            gui.registerClosed(function () { record("guiClosed"); });
            gui.registerDraw(function () { record("guiDraw"); });
            gui.registerClicked(function (x, y, button) {
                record("guiClick", { x: Number(x), y: Number(y), button: Number(button) });
            });
            gui.registerMouseDragged(function (dx, dy, x, y, button) {
                record("guiDrag", {
                    dx: Number(dx), dy: Number(dy), x: Number(x), y: Number(y), button: Number(button)
                });
            });
            gui.registerKeyTyped(function (typed, key) {
                record("guiKey", { typed: String(typed), key: Number(key) });
            });
            GuiHandler.openGui(gui);
            Client.scheduleTask(5, function () {
                try {
                    gui.onMouseClicked(23.5, 45.5, 1);
                    gui.mouseDragged(
                        new MouseButtonEvent(41.25, 62.75, new MouseButtonInfo(0, 0)),
                        3.5,
                        -4.75
                    );
                    gui.onKeyPressed(65, Character.valueOf("A"), null);
                    record("guiSyntheticInputComplete");
                } catch (error) {
                    fail("guiInput", error);
                }
                GuiHandler.openGui(null);
            });
        } catch (error) {
            fail("gui", error);
        }
    }

    function coreSmoke() {
        try {
            ChatLib.chat("&a[P1] core smoke " + marker);
            ChatLib.chat("__CTJS_RELEASE_CHAT__");
            ChatLib.actionBar("&bP1 actionbar " + marker);
            record("chatLib", ChatLib.removeFormatting("&aok") === "ok");
            Client.showTitle("P1", "generation " + marker, 1, 5, 1);
            record("clientPlayerWorld", {
                player: String(Player.getName()),
                x: Number(Player.getX()),
                time: Number(World.getTime()),
                entities: Number(World.getAllEntities().size())
            });

            const pos = new BlockPos(Math.floor(Player.getX()), Math.floor(Player.getY()) - 1, Math.floor(Player.getZ()));
            const block = World.getBlockAt(pos);
            const stone = new BlockType("minecraft:stone");
            const item = new Item(new ItemType("minecraft:stone"));
            record("wrappers", {
                block: String(block.type.getRegistryName()),
                stone: String(stone.getRegistryName()),
                item: String(item.getName()),
                held: Player.getHeldItem() === null ? null : String(Player.getHeldItem().getName())
            });

            FileLib.write("P1RuntimeProbe", "runtime-io.txt", "a", true);
            FileLib.append("P1RuntimeProbe", "runtime-io.txt", "b");
            const readBack = FileLib.read("P1RuntimeProbe", "runtime-io.txt");
            const deleted = FileLib.delete("P1RuntimeProbe", "runtime-io.txt");
            record("fileLib", { readBack: String(readBack), deleted: Boolean(deleted) });

            const originalHelp = Boolean(Config.moduleImportHelp);
            Config.moduleImportHelp = !originalHelp;
            Config.INSTANCE.writeData();
            Config.INSTANCE.loadData();
            Config.moduleImportHelp = originalHelp;
            Config.INSTANCE.writeData();
            record("config", { modulesFolder: String(Config.modulesFolder), restored: Config.moduleImportHelp === originalHelp });
            console.log("[P1RuntimeProbe] console smoke generation " + marker);

            activeImage = new Image(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));
            const destroyedImage = new Image(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
            destroyedImage.destroy();
            destroyedImage.destroy();

            activeSound = new Sound({
                source: "minecraft:ui.button.click",
                x: Player.getX(), y: Player.getY(), z: Player.getZ(), volume: 0.01
            });
            activeSound.play(1);
            Client.scheduleTask(4, function () { activeSound.pause(); });
            Client.scheduleTask(6, function () { activeSound.play(); });
            Client.scheduleTask(8, function () { activeSound.stop(); });
            const destroyedSound = new Sound({
                source: "minecraft:ui.button.click",
                x: Player.getX(), y: Player.getY(), z: Player.getZ(), volume: 0.01
            });
            destroyedSound.destroy();
            destroyedSound.destroy();
            record("resourcesCreated");
        } catch (error) {
            fail("coreSmoke", error);
        }
    }

    function startGeneration(reason) {
        if (started) return;
        started = true;
        record("entry", String(reason));
        coreSmoke();
        exerciseGui();

        Client.scheduleTask(10, function () {
            ChatLib.command("p1command", true);
            ChatLib.command("p1alias", true);
            ChatLib.command("p1dynamic", true);
        });
        Client.scheduleTask(0, function () { record("taskCurrent"); });
        setTimeout(function () { record("timeoutCurrent"); }, 300);

        if (marker < 4) {
            Client.scheduleTask(120, function () { record("oldTaskLeak"); });
            setTimeout(function () { record("oldTimeoutLeak"); }, 7000);
            Client.scheduleTask(60, function () {
                record("reloadRequested");
                ChatLib.command("ct reload", true);
            });
        } else {
            Client.scheduleTask(15, function () {
                GuiHandler.openGui(new TitleScreen());
                Client.scheduleTask(5, function () {
                    record("rawScreen", Client.currentGui.get() instanceof TitleScreen);
                    GuiHandler.openGui(null);
                });
            });
        }
    }

    function finish() {
        if (completeFile.isFile()) return;
        try {
            if (activeSound !== null) {
                activeSound.stop();
                activeSound.destroy();
                activeSound.destroy();
            }
            if (activeImage !== null) {
                activeImage.destroy();
                activeImage.destroy();
            }
            record("complete", { renderFrames: renderFrames, guiFrames: guiFrames });
            writeText(completeFile, JSON.stringify({
                status: "READY_FOR_ANALYSIS",
                marker: marker,
                renderFrames: renderFrames,
                guiFrames: guiFrames
            }), false);
            Client.scheduleTask(5, function () { Client.getMinecraft().stop(); });
        } catch (error) {
            fail("finish", error);
        }
    }
})();
