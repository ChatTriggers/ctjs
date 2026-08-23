(function () {
    const JFile = Java.type("java.io.File");
    const FileWriter = Java.type("java.io.FileWriter");
    const TitleScreen = Java.type("net.minecraft.client.gui.screens.TitleScreen");
    const moduleDir = new JFile("./config/ChatTriggers/modules/CompatRuntimeAProbe").getCanonicalFile();
    const resultFile = new JFile(moduleDir, "result.json");
    let started = false;
    let renderFrames = 0;
    const result = {
        vigilanceElementa: false,
        vigilanceScreenClass: null,
        vigilanceClose: false,
        modernGui: false,
        rawScreen: false,
        nullClose: false,
        tessellatorFrames: 0,
        failures: []
    };

    function writeResult() {
        const writer = new FileWriter(resultFile, false);
        try {
            writer.write(JSON.stringify(result));
        } finally {
            writer.close();
        }
    }

    function fail(area, error) {
        result.failures.push({ area: String(area), error: String(error) });
    }

    register("gameLoad", function () {
        Client.scheduleTask(20, function () {
            if (World.isLoaded()) start();
            else Client.getMinecraft().createWorldOpenFlows().openWorld("test", function () {
                fail("worldOpen", "test world failed to open");
                finish();
            });
        });
    });

    register("worldLoad", function () { start(); });

    register("renderOverlay", function () {
        if (!started || renderFrames >= 40) return;
        renderFrames++;
        try {
            Tessellator.enableTexture2D()
                .begin(GL11.GL_TRIANGLES)
                .pos(2, 2, 0).tex(0, 0).color(255, 0, 0, 255)
                .pos(8, 2, 0).tex(1, 0).color(0, 255, 0, 255)
                .pos(2, 8, 0).tex(0, 1).color(0, 0, 255, 255)
                .draw();
            Tessellator.disableTexture2D()
                .begin(GL11.GL_POLYGON)
                .pos(10, 2, 0).color(255, 255, 255, 255)
                .pos(16, 2, 0).color(255, 255, 255, 255)
                .pos(16, 8, 0).color(255, 255, 255, 255)
                .pos(10, 8, 0).color(255, 255, 255, 255)
                .draw();
            result.tessellatorFrames = renderFrames;
        } catch (error) {
            fail("tessellator", error);
        }
    });

    function start() {
        if (started) return;
        started = true;
        Client.scheduleTask(10, function () {
            try {
                ChatLib.command("tm config", true);
            } catch (error) {
                fail("vigilanceCommand", error);
            }
        });
        Client.scheduleTask(30, function () {
            try {
                const screen = Client.currentGui.get();
                const className = screen === null ? "" : String(screen.getClass().getName());
                result.vigilanceScreenClass = className;
                const lower = className.toLowerCase();
                result.vigilanceElementa = lower.indexOf("vigilance") >= 0 || lower.indexOf("elementa") >= 0;
                GuiHandler.openGui(null);
                Client.scheduleTask(2, function () {
                    result.vigilanceClose = Client.currentGui.get() === null;
                    const gui = new Gui();
                    GuiHandler.openGui(gui);
                    Client.scheduleTask(2, function () {
                        result.modernGui = Client.currentGui.get() === gui;
                        GuiHandler.openGui(null);
                        Client.scheduleTask(2, function () {
                            const raw = new TitleScreen();
                            GuiHandler.openGui(raw);
                            Client.scheduleTask(2, function () {
                                result.rawScreen = Client.currentGui.get() === raw;
                                GuiHandler.openGui(null);
                                Client.scheduleTask(2, function () {
                                    result.nullClose = Client.currentGui.get() === null;
                                    finish();
                                });
                            });
                        });
                    });
                });
            } catch (error) {
                fail("guiHandler", error);
                finish();
            }
        });
    }

    function finish() {
        writeResult();
        Client.scheduleTask(5, function () { Client.getMinecraft().stop(); });
    }
})();
