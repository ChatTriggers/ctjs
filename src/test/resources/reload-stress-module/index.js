(function () {
    const JFile = Java.type("java.io.File");
    const FileWriter = Java.type("java.io.FileWriter");
    const Files = Java.type("java.nio.file.Files");
    const StandardCopyOption = Java.type("java.nio.file.StandardCopyOption");
    const BufferedImage = Java.type("java.awt.image.BufferedImage");
    const GLFW = Java.type("org.lwjgl.glfw.GLFW");
    const TitleScreen = Java.type("net.minecraft.client.gui.screens.TitleScreen");
    const Diagnostics = Java.type("com.chattriggers.ctjs.internal.diagnostics.ReloadStressDiagnostics");
    const ProbeClass = Java.type("reloadstress.Probe");

    const moduleDir = new JFile("./config/ChatTriggers/modules/ReloadStressProbe").getCanonicalFile();
    const markerFile = new JFile(moduleDir, "generation.txt");
    const eventsFile = new JFile(moduleDir, "events.jsonl");
    const completeFile = new JFile(moduleDir, "complete.json");
    const activeJar = new JFile(moduleDir, "probe.jar");
    const versionsDir = new JFile(moduleDir, "versions");
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

    const entrySnapshot = JSON.parse(String(Diagnostics.snapshotJson()));
    const runtimeGeneration = Number(entrySnapshot.generationId);
    let eventSequence = 0;

    const record = sync(function (event, data) {
        const row = {
            sequence: ++eventSequence,
            wallMillis: Number(java.lang.System.currentTimeMillis()),
            marker: marker,
            runtimeGeneration: runtimeGeneration,
            event: String(event),
            data: data === undefined ? null : data
        };
        writeText(eventsFile, JSON.stringify(row) + "\n", true);
    }, recordLock);

    function snapshot(label) {
        try {
            record("snapshot", {
                label: String(label),
                values: JSON.parse(String(Diagnostics.snapshotJson()))
            });
        } catch (error) {
            record("diagnosticsException", String(error));
        }
    }

    function expectedJarVersion() {
        if (marker <= 3) return "v1";
        if (marker <= 6) return "v2";
        if (marker <= 9) return "v3";
        return "v4";
    }

    function prepareNextJar() {
        const nextMarker = marker + 1;
        let version = null;
        if (nextMarker === 4) version = "v2";
        if (nextMarker === 7) version = "v3";
        if (nextMarker === 10) version = "v4";
        if (version === null) return;

        const source = new JFile(versionsDir, version + ".bin");
        Files.copy(source.toPath(), activeJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        record("sourceJarUpdated", version);
    }

    record("entry", entrySnapshot);
    const actualJarVersion = String(ProbeClass.version());
    record("jarVersion", actualJarVersion);
    if (actualJarVersion !== expectedJarVersion()) {
        record("assertionFailure", "Expected " + expectedJarVersion() + " but loaded " + actualJarVersion);
    }

    let started = false;
    let tickCount = 0;
    let renderFrames = 0;
    let guiDraws = 0;
    let toastDraws = 0;
    let activeImage = null;
    let activeSound = null;
    let lateImage = null;
    let lateSound = null;

    register("gameLoad", function () {
        record("gameLoad");
        if (marker === 1) {
            Client.scheduleTask(20, function () {
                if (World.isLoaded()) {
                    startGeneration("alreadyInWorld");
                    return;
                }
                record("worldOpenRequested", "test");
                Client.getMinecraft().createWorldOpenFlows().openWorld("test", function () {
                    record("worldOpenFailure", "test");
                });
            });
        } else if (World.isLoaded()) {
            Client.scheduleTask(1, function () {
                startGeneration("gameLoadWorldPresent");
            });
        }
    });

    register("gameUnload", function () {
        record("gameUnload");
    });

    register("worldLoad", function () {
        record("worldLoad");
        startGeneration("worldLoad");
    });

    register("worldUnload", function () {
        record("worldUnload");
    });

    register("tick", function () {
        tickCount++;
        if (tickCount <= 3) record("tick", tickCount);
    });

    register("command", function () {
        record("staticCommand");
    }).setName("ctstressstatic", true);

    Commands.registerCommand("ctstressdynamic", function () {
        Commands.exec(function () {
            record("dynamicCommand");
        });
    });

    const keyBind = new KeyBind("CTJS reload stress", GLFW.GLFW_KEY_F24, "CTJS Reload Stress");
    keyBind.registerKeyDown(function () {
        record("keybindDown");
    });

    const customTrigger = createCustomTrigger("ctjs:reload_stress");
    register("ctjs:reload_stress", function (value) {
        record("customTrigger", Number(value));
    });

    register("renderOverlay", function () {
        if (!started || renderFrames >= 6) return;
        renderFrames++;
        try {
            const x = Player.getX();
            const y = Player.getY() + 1;
            const z = Player.getZ();

            Renderer.disableCull();
            Renderer3d.begin(Renderer.DrawMode.TRIANGLES, Renderer.VertexFormat.POSITION_COLOR);
            Renderer3d.pos(x, y, z).color(255, 0, 0, 160);
            Renderer3d.pos(x + 0.25, y, z).color(0, 255, 0, 160);
            Renderer3d.pos(x, y + 0.25, z).color(0, 0, 255, 160);
            Renderer3d.draw();
            Renderer.enableCull();

            Renderer.drawRect(Renderer.getColor(20, 40, 60, 120), 2, 2, 6, 6);
            Tessellator.begin(GL11.GL_TRIANGLES)
                .pos(12, 2, 0).color(255, 0, 0, 255)
                .pos(18, 2, 0).color(0, 255, 0, 255)
                .pos(12, 8, 0).color(0, 0, 255, 255)
                .draw();

            if (activeImage !== null) activeImage.draw(22, 2, 2, 2);
            if (renderFrames === 6) snapshot("renderBoundary");
        } catch (error) {
            record("renderException", String(error));
            try { Renderer3d.draw(); } catch (ignored) {}
            try { Renderer.enableCull(); } catch (ignored2) {}
        }
    });

    function startGeneration(reason) {
        if (started) return;
        started = true;
        record("generationStarted", String(reason));

        if (marker === 4) {
            const state = JSON.parse(String(Diagnostics.snapshotJson()));
            record("oldGuiInvalidated", !state.ctjsGuiOpen);
        }
        if (marker === 6) {
            const state = JSON.parse(String(Diagnostics.snapshotJson()));
            record("externalScreenRetained", String(state.screenClass).indexOf("TitleScreen") >= 0);
            Client.scheduleTask(5, function () { GuiHandler.openGui(null); });
        }

        TabList.setHeader("CTJS reload stress " + marker);
        TabList.setFooter("generation " + marker);
        Scoreboard.setTitle("CTJS reload stress " + marker);

        activeImage = new Image(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));
        const destroyedImage = new Image(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        destroyedImage.destroy();
        destroyedImage.destroy();

        activeSound = new Sound({
            source: "minecraft:ui.button.click",
            x: Player.getX(),
            y: Player.getY(),
            z: Player.getZ(),
            volume: 0.01
        });
        const destroyedSound = new Sound({
            source: "minecraft:ui.button.click",
            x: Player.getX(),
            y: Player.getY(),
            z: Player.getZ(),
            volume: 0.01
        });
        destroyedSound.destroy();
        destroyedSound.destroy();

        if (marker <= 10) activeSound.play(140);
        else activeSound.play(1);

        const toast = new Toast({
            title: "Reload stress " + marker,
            displayTime: 20000,
            render: function () {
                toastDraws++;
                if (toastDraws <= 2) record("toastRender", toastDraws);
                Renderer.drawString(new java.lang.String("Reload " + marker), 8, 8);
            }
        });
        toast.show();

        if (marker === 3) {
            const gui = new Gui();
            gui.registerDraw(function () {
                guiDraws++;
                if (guiDraws <= 2) record("guiDraw", guiDraws);
            });
            Client.scheduleTask(15, function () {
                record("ctjsGuiOpenRequested");
                GuiHandler.openGui(gui);
            });
        }

        if (marker === 5) {
            Client.scheduleTask(15, function () {
                record("externalScreenOpenRequested");
                GuiHandler.openGui(new TitleScreen());
            });
        }

        customTrigger.trigger(marker);
        Client.scheduleTask(0, function () { record("taskImmediate"); });
        Client.scheduleTask(15, function () { record("taskDelayedActive"); });
        setTimeout(function () { record("timeoutActive"); }, 500);

        Client.scheduleTask(10, function () {
            ChatLib.command("ctstressstatic", true);
            ChatLib.command("ctstressdynamic", true);
        });

        Client.scheduleTask(5, function () {
            keyBind.setState(true);
        });
        Client.scheduleTask(6, function () {
            keyBind.setState(false);
        });

        if (marker <= 10) {
            Client.scheduleTask(140, function () { record("oldTaskLeak"); });
            setTimeout(function () { record("oldTimeoutLeak"); }, 8000);
            Client.scheduleTask(58, function () {
                lateImage = new Image(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));
                lateSound = new Sound({
                    source: "minecraft:ui.button.click",
                    x: Player.getX(),
                    y: Player.getY(),
                    z: Player.getZ(),
                    volume: 0.01
                });
                lateSound.play(100);
                record("lateResourcesCreated");
            });
            Client.scheduleTask(60, function () {
                snapshot("preReload");
                prepareNextJar();
                record("reloadRequested");
                ChatLib.command("ct reload", true);
            });
        } else {
            Client.scheduleTask(20, function () {
                if (activeSound !== null) activeSound.stop();
            });
            setTimeout(function () {
                Client.scheduleTask(0, function () {
                    snapshot("final");
                    record("stressComplete");
                    writeText(completeFile, JSON.stringify({
                        status: "READY_FOR_ANALYSIS",
                        marker: marker,
                        runtimeGeneration: runtimeGeneration,
                        jarVersion: actualJarVersion
                    }), false);
                });
            }, 9000);
        }

        Client.scheduleTask(20, function () { snapshot("active"); });
    }

    snapshot("entry");
})();
