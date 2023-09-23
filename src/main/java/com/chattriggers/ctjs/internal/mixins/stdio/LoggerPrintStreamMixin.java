package com.chattriggers.ctjs.internal.mixins.stdio;

import net.minecraft.util.logging.LoggerPrintStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.PrintStream;

// Add additional overrides so that org.spongepowered.asm.util.PrettyPrinter
// will output to the log file.
@Mixin(LoggerPrintStream.class)
public class LoggerPrintStreamMixin {
    @Shadow
    protected void log(String message) {
        throw new IllegalStateException();
    }

    public PrintStream printf(String format, Object... args) {
        // The incoming format string will have a trailing newline, but this is
        // going to a slf4j method, which will add the newline for us. So we strip
        // the last newline, if one exists.
        String formatted = format.formatted(args);
        if (!formatted.isEmpty() && formatted.charAt(formatted.length() - 1) == '\n')
            formatted = formatted.substring(0, formatted.length() - 1);
        log(formatted);
        return (LoggerPrintStream) (Object) this;
    }
}
