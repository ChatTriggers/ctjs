(function () {
    const JFile = Java.type("java.io.File");
    const FileWriter = Java.type("java.io.FileWriter");
    const Files = Java.type("java.nio.file.Files");
    const moduleDir = new JFile("./config/ChatTriggers/modules/P1RuntimeProbe").getCanonicalFile();
    const markerFile = new JFile(moduleDir, "generation.txt");
    const mixinEventsFile = new JFile(moduleDir, "mixin-events.jsonl");

    function readMarker() {
        if (!markerFile.isFile()) return 0;
        return parseInt(String(Files.readString(markerFile.toPath())).trim(), 10) || 0;
    }

    function append(row) {
        const writer = new FileWriter(mixinEventsFile, true);
        try {
            writer.write(JSON.stringify(row) + "\n");
        } finally {
            writer.close();
        }
    }

    // mixinEntry runs before the ordinary entrypoint. The next marker is therefore
    // the generation that will own this handler.
    const ownerMarker = readMarker() + 1;
    let calls = 0;
    const callback = new Mixin("net.minecraft.client.Minecraft").inject({
        method: "tick()V",
        at: new At("HEAD")
    });

    if (callback !== null) {
        callback.attach(function () {
            calls++;
            if (calls <= 3) {
                append({
                    marker: ownerMarker,
                    event: "mixinInvoke",
                    call: calls,
                    wallMillis: Number(java.lang.System.currentTimeMillis())
                });
            }
        });
    }
})();
