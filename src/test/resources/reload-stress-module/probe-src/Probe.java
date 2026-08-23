package reloadstress;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class Probe {
    private Probe() {
    }

    public static String version() {
        try (InputStream stream = Probe.class.getResourceAsStream("/reloadstress-version.txt")) {
            if (stream == null) {
                throw new IllegalStateException("Missing reloadstress-version.txt");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read reload stress version", exception);
        }
    }
}
