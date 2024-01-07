(function(global) {
    global.Mappings = com.chattriggers.ctjs.api.Mappings;

    function getJavaType(clazz) {
        const mappedName = Mappings.mapClassName(clazz);
        if (mappedName)
            return Packages[mappedName.replaceAll("/", ".")]
        return Packages[clazz];
    }

    const Class = getJavaType("java.lang.Class");

    function getJavaClass(clazz) {
        const type = getJavaType(clazz);
        if (!(type.class instanceof Class))
            throw new Error(`Unknown class: ${clazz}`);
        return type;
    }

    global.Java = {
        type: getJavaType,
        class: getJavaClass,
    };

    global.sync = (func, lock) => new org.mozilla.javascript.Synchronizer(func, lock);

    global.setTimeout = function (func, delay) {
        new Thread(function () {
            Thread.sleep(delay);
            func();
        }).start();
    };

    const getClassName = path => path.substring(path.lastIndexOf('.') + 1)

    function loadClass(path, className = getClassName(path)) {
        global[className] = Java.class(path);
    }

    // API

    loadClass("java.util.ArrayList");
    loadClass("java.util.HashMap");
    loadClass("gg.essential.universal.UKeyboard", "Keyboard");
    loadClass("net.minecraft.util.Hand");

    loadClass("com.chattriggers.ctjs.api.client.Client");
    loadClass("com.chattriggers.ctjs.api.client.CPS");
    loadClass("com.chattriggers.ctjs.api.client.FileLib");
    loadClass("com.chattriggers.ctjs.api.client.KeyBind");
    loadClass("com.chattriggers.ctjs.api.client.MathLib");
    loadClass("com.chattriggers.ctjs.api.client.Player");
    loadClass("com.chattriggers.ctjs.api.client.Settings");
    loadClass("com.chattriggers.ctjs.api.client.Sound");

    loadClass("com.chattriggers.ctjs.api.commands.DynamicCommands", "Commands");

    loadClass("com.chattriggers.ctjs.api.entity.BlockEntity");
    loadClass("com.chattriggers.ctjs.api.entity.Entity");
    loadClass("com.chattriggers.ctjs.api.entity.LivingEntity");
    loadClass("com.chattriggers.ctjs.api.entity.Particle");
    loadClass("com.chattriggers.ctjs.api.entity.PlayerInteraction");
    loadClass("com.chattriggers.ctjs.api.entity.PlayerMP");
    loadClass("com.chattriggers.ctjs.api.entity.Team");

    loadClass("com.chattriggers.ctjs.api.inventory.action.Action");
    loadClass("com.chattriggers.ctjs.api.inventory.action.ClickAction");
    loadClass("com.chattriggers.ctjs.api.inventory.action.DragAction");
    loadClass("com.chattriggers.ctjs.api.inventory.action.DropAction");
    loadClass("com.chattriggers.ctjs.api.inventory.action.KeyAction");
    loadClass("com.chattriggers.ctjs.api.inventory.nbt.NBT");
    loadClass("com.chattriggers.ctjs.api.inventory.nbt.NBTBase");
    loadClass("com.chattriggers.ctjs.api.inventory.nbt.NBTTagCompound");
    loadClass("com.chattriggers.ctjs.api.inventory.nbt.NBTTagList");
    loadClass("com.chattriggers.ctjs.api.inventory.Inventory");
    loadClass("com.chattriggers.ctjs.api.inventory.Item");
    loadClass("com.chattriggers.ctjs.api.inventory.ItemType");
    loadClass("com.chattriggers.ctjs.api.inventory.Slot");

    loadClass("com.chattriggers.ctjs.api.message.ChatLib");
    loadClass("com.chattriggers.ctjs.api.message.TextComponent");

    loadClass("com.chattriggers.ctjs.api.render.Book");
    loadClass("com.chattriggers.ctjs.api.render.Display");
    loadClass("com.chattriggers.ctjs.api.render.Gui");
    loadClass("com.chattriggers.ctjs.api.render.Image");
    loadClass("com.chattriggers.ctjs.api.render.Rectangle");
    loadClass("com.chattriggers.ctjs.api.render.Renderer");
    loadClass("com.chattriggers.ctjs.api.render.Renderer3d");
    loadClass("com.chattriggers.ctjs.api.render.Shape");
    loadClass("com.chattriggers.ctjs.api.render.Text");
    loadClass("com.chattriggers.ctjs.api.render.Toast");

    // For module authors to use with custom triggers
    loadClass("com.chattriggers.ctjs.api.triggers.CancellableEvent");

    loadClass("com.chattriggers.ctjs.api.vec.Vec2f");
    loadClass("com.chattriggers.ctjs.api.vec.Vec3f");
    loadClass("com.chattriggers.ctjs.api.vec.Vec3i");

    loadClass("com.chattriggers.ctjs.api.world.block.Block");
    loadClass("com.chattriggers.ctjs.api.world.block.BlockFace");
    loadClass("com.chattriggers.ctjs.api.world.block.BlockPos");
    loadClass("com.chattriggers.ctjs.api.world.block.BlockType");
    loadClass("com.chattriggers.ctjs.api.world.BossBars");
    loadClass("com.chattriggers.ctjs.api.world.Chunk");
    loadClass("com.chattriggers.ctjs.api.world.PotionEffect");
    loadClass("com.chattriggers.ctjs.api.world.PotionEffectType");
    loadClass("com.chattriggers.ctjs.api.world.Scoreboard");
    loadClass("com.chattriggers.ctjs.api.world.Server");
    loadClass("com.chattriggers.ctjs.api.world.TabList");
    loadClass("com.chattriggers.ctjs.api.world.World");

    loadClass("com.chattriggers.ctjs.api.Config");

    // Misc

    loadClass("com.chattriggers.ctjs.engine.Register", "TriggerRegister");
    loadClass("com.chattriggers.ctjs.engine.WrappedThread", "Thread");
    global.Priority = Java.class("com.chattriggers.ctjs.api.triggers.Trigger").Priority;
    loadClass("com.chattriggers.ctjs.CTJS", "ChatTriggers");
    global.Console = Java.type("com.chattriggers.ctjs.engine.Console").INSTANCE;

    // GL
    loadClass("org.lwjgl.opengl.GL11");
    loadClass("org.lwjgl.opengl.GL12");
    loadClass("org.lwjgl.opengl.GL13");
    loadClass("org.lwjgl.opengl.GL14");
    loadClass("org.lwjgl.opengl.GL15");
    loadClass("org.lwjgl.opengl.GL20");
    loadClass("org.lwjgl.opengl.GL21");
    loadClass("org.lwjgl.opengl.GL30");
    loadClass("org.lwjgl.opengl.GL31");
    loadClass("org.lwjgl.opengl.GL32");
    loadClass("org.lwjgl.opengl.GL33");
    loadClass("org.lwjgl.opengl.GL40");
    loadClass("org.lwjgl.opengl.GL41");
    loadClass("org.lwjgl.opengl.GL42");
    loadClass("org.lwjgl.opengl.GL43");
    loadClass("org.lwjgl.opengl.GL44");
    loadClass("org.lwjgl.opengl.GL45");

    global.cancel = event => {
        if (event instanceof CancellableEvent) {
            event.setCanceled(true);
        } else if (event instanceof org.spongepowered.asm.mixin.injection.callback.CallbackInfo) {
            event.cancel();
        } else {
            throw new TypeError("event must be a CancellableEvent, CallbackInfo, or CallbackInfoReturnable")
        }
    };

    global.register = (type, method) => TriggerRegister.register(type, method);
    global.createCustomTrigger = name => TriggerRegister.createCustomTrigger(name);

    // String prototypes
    String.prototype.addFormatting = function () {
        return ChatLib.addColor(this);
    };

    String.prototype.addColor = String.prototype.addFormatting;

    String.prototype.removeFormatting = function () {
        return ChatLib.removeFormatting(this);
    };

    String.prototype.replaceFormatting = function () {
        return ChatLib.replaceFormatting(this);
    };

    // animation
    global.easeOut = (start, finish, speed, jump = 1) => {
        if (Math.floor(Math.abs(finish - start) / jump) > 0)
            return start + (finish - start) / speed;
        return finish;
    };

    Number.prototype.easeOut = function (to, speed, jump) {
        return easeOut(this, to, speed, jump);
    };

    global.easeColor = (start, finish, speed, jump) => Renderer.getColor(
        easeOut((start >> 16) & 0xFF, (finish >> 16) & 0xFF, speed, jump),
        easeOut((start >> 8) & 0xFF, (finish >> 8) & 0xFF, speed, jump),
        easeOut(start & 0xFF, finish & 0xFF, speed, jump),
        easeOut((start >> 24) & 0xFF, (finish >> 24) & 0xFF, speed, jump)
    );

    Number.prototype.easeColor = function (to, speed, jump) {
        return easeColor(this, to, speed, jump);
    };

    const LogType = com.chattriggers.ctjs.engine.LogType;

    global.print = function (toPrint, color = null) {
        println(toPrint, color, "");
    }

    global.println = function (toPrint, color = null, end = "\n") {
        if (toPrint === null) {
            toPrint = "null";
        } else if (toPrint === undefined) {
            toPrint = "undefined";
        }

        Console.println(toPrint, LogType.INFO, end, color);
    };


    /**
     * @fileoverview console.js
     * Implementation of the whatwg/console namespace for the Nashorn script engine.
     * Ported to CT/Rhino
     * See https://console.spec.whatwg.org
     *
     * @author https://github.com/fmartin5
     *
     * @license AGPL-3.0
     *
     * @environment Nashorn on JDK 9
     *
     * @globals es5
     *
     * @syntax es5 +arrow-functions +const +for-of +let
     *
     * @members
     * config (non-standard)
     * assert
     * clear
     * count
     * debug
     * dir
     * dirxml
     * error
     * group
     * groupCollapsed
     * groupEnd
     * info
     * log
     * table
     * time
     * timeEnd
     * trace
     * warn
     */

    (function (globalObject, factory) {
        Object.defineProperty(globalObject, "console", {
            "writable": true,
            "configurable": true,
            "enumerable": false,
            "value": factory()
        });
    }(global, function factory() {
        const console = {};

        console.config = {
            "indent": " | ",
            "showMilliseconds": false,
            "showMessageType": false,
            "showTimeStamp": false,
            "useColors": false,
            "colorsByLogLevel": {
                "error": "",
                "log": "",
                "info": "",
                "warn": ""
            }
        };

        // An object holding local data and functions.
        const _ = {};

        // The following line can be commented out in development for debugging purposes.
        // console._ = _;

        _.counters = {};

        _.defaultStringifier = function (anyValue) {
            // noinspection FallThroughInSwitchStatementJS
            switch (typeof anyValue) {
                case "function":
                    return "[object Function(" + anyValue.length + ")]";
                case "object": {
                    if (Array.isArray(anyValue)) return "[object Array(" + anyValue.length + ")]";
                }
                default:
                    return String(anyValue);
            }
        };

        _.formats = {
            "%d": (x) => typeof x === "symbol" ? NaN : parseInt(x),
            "%f": (x) => typeof x === "symbol" ? NaN : parseFloat(x),
            "%i": (x) => typeof x === "symbol" ? NaN : parseInt(x),
            "%s": (x) => String(x),
            "%%": (x) => "%",
            // @todo Implement less trivial behaviors for the following format specifiers.
            "%c": (x) => String(x),
            "%o": (x) => String(x),
            "%O": (x) => String(x)
        };

        _.indentLevel = 0;
        _.lineSep = java.lang.System.getProperty("line.separator");
        _.timers = {};


        // Formatter
        _.formatArguments = function (args) {
            const rv = [];
            for (let i = 0; i < args.length; i++) {
                const arg = args[i];
                if (typeof arg === "string") {
                    rv.push(arg.replace(/%[cdfiosO%]/g, (match) => {
                        if (match === "%%") return "%";
                        return ++i in args ? _.formats[match](args[i]) : match;
                    }));
                } else {
                    rv.push(String(arg));
                }
            }
            return rv.join(" ");
        };

        _.formatObject = function (object) {
            const sep = _.lineSep;
            return _.defaultStringifier(object) + (" {" + sep + " ") + Object.keys(object).map(key =>
                _.defaultStringifier(key) + ": " + _.defaultStringifier(object[key])).join("," + sep + " ") + (sep + "}");
        };

        _.makeTimeStamp = function () {
            if (console.config.showMilliseconds) {
                const date = new Date();
                return date.toLocaleTimeString() + "," + date.getMilliseconds();
            }
            return new Date().toLocaleTimeString();
        };


        // Printer
        _.printlnWarn = function (s) {
            Console.println(s, LogType.WARN);
        };

        _.printLineToStdErr = function (s) {
            Console.println(s, LogType.ERROR);
        };

        _.printLineToStdOut = function (s) {
            Console.println(s);
        };


        _.repeat = (s, n) => new Array(n + 1).join(s);


        // Logger
        _.writeln = function (msgType, msg) {
            if (arguments.length < 2) return;
            const showMessageType = console.config.showMessageType;
            const showTimeStamp = console.config.showTimeStamp;
            const msgTypePart = showMessageType ? msgType : "";
            const timeStampPart = showTimeStamp ? _.makeTimeStamp() : "";
            const prefix = (showMessageType || showTimeStamp
                ? "[" + msgTypePart + (msgTypePart && timeStampPart ? " - " : "") + timeStampPart + "] "
                : "");
            const indent = (_.indentLevel > 0
                ? _.repeat(console.config.indent, _.indentLevel)
                : "");
            switch (msgType) {
                case "assert":
                case "error": {
                    _.printLineToStdErr(prefix + indent + msg);
                    break;
                }
                case "warn": {
                    _.printlnWarn(prefix + indent + msg);
                    break;
                }
                default: {
                    _.printLineToStdOut(prefix + indent + msg);
                }
            }
        };


        /**
         * Tests whether the given expression is true. If not, logs a message with the visual "error" representation.
         */
        console.assert = function assert(booleanValue, arg) {
            if (!!booleanValue) return;
            const defaultMessage = "assertion failed";
            if (arguments.length < 2) return _.writeln("assert", defaultMessage);
            if (typeof arg !== "string") return _.writeln("assert", _.formatArguments([defaultMessage, arg]));
            _.writeln("assert", (defaultMessage + ": " + arg));
        };


        /**
         * Clears the console.
         * Works by invoking `cmd /c cls` on Windows and `clear` on other OSes.
         */
        console.clear = function clear() {
            try {
                Console.clearConsole();
            } catch (_) {
                // Pass
            }
        };


        /**
         * Prints the number of times that 'console.count()' was called with the same label.
         */
        console.count = function count(label = "default") {
            label = String(label);
            if (!(label in _.counters)) _.counters[label] = 0;
            _.counters[label]++;
            _.writeln("count", label + ": " + _.counters[label]);
        };


        /**
         * Logs a message, with a visual "debug" representation.
         * @todo Optionally include some information for debugging, like the file path or line number where the call occurred from.
         */
        console.debug = function debug(...args) {
            const s = _.formatArguments(args);
            _.writeln("debug", s);
        };

        /**
         * Logs a listing of the properties of the given object.
         * @todo Use the `options` argument.
         */
        console.dir = function dir(arg, options) {
            if (Object(arg) === arg) {
                _.writeln("dir", _.formatObject(arg));
                return;
            }
            _.writeln("dir", _.defaultStringifier(arg));
        };


        /**
         * Logs a space-separated list of formatted representations of the given arguments,
         * using DOM tree representation whenever possible.
         */
        console.dirxml = function dirxml(...args) {
            const list = [];
            args.forEach((arg) => {
                if (Object(arg) === arg) list.push(_.formatObject(arg));
                else list.push(_.defaultStringifier(arg));
            });
            _.writeln("dirxml", list.join(" "));
        };


        /**
         * Logs a message with the visual "error" representation.
         * @todo Optionally include some information for debugging, like the file path or line number where the call occurred from.
         */
        console.error = function error(...args) {
            const s = _.formatArguments(args);
            _.writeln("error", s);
        };


        /**
         * Logs a message as a label for and opens a nested block to indent future messages sent.
         * Call console.groupEnd() to close the block.
         * Representation of block is up to the platform,
         * it can be an interactive block or just a set of indented sub messages.
         */
        console.group = function group(...args) {
            if (args.length) {
                const s = _.formatArguments(args);
                _.writeln("group", s);
            }
            _.indentLevel++;
        };


        console.groupCollapsed = function groupCollapsed(...args) {
            console.group([]);
        };


        /**
         * Closes the most recently opened block created by a call
         * to 'console.group()' or 'console.groupCollapsed()'.
         */
        console.groupEnd = function groupEnd() {
            if (_.indentLevel < 1) return;
            _.indentLevel--;
        };


        /**
         * Logs a message with the visual "info" representation.
         */
        console.info = function info(...args) {
            const s = _.formatArguments(args);
            _.writeln("info", s);
        };


        /**
         * Logs a message with the visual "log" representation.
         */
        console.log = function log(...args) {
            const s = _.formatArguments(args);
            _.writeln("log", s);
        };


        console.time = function time(label = "default") {
            label = String(label);
            if (label in _.timers) return;
            _.timers[label] = Date.now();
        };


        /**
         * Stops a timer created by a call to `console.time(label)` and logs the elapsed time.
         */
        console.timeEnd = function timeEnd(label = "default") {
            label = String(label);
            const milliseconds = Date.now() - _.timers[label];
            delete _.timers[label];

            _.writeln("timeEnd", label + ": " + milliseconds + " ms");
        };


        /**
         * Logs a stack trace for where the call occurred from, using the given arguments as a label.
         */
        console.trace = function trace(...args) {
            const label = "Trace" + (args.length > 0 ? ": " + _.formatArguments(args) : "");
            const e = new Error();
            Error.captureStackTrace(e, trace);
            // Replaces the first line by our label.
            const s = label + "\n" + e.stack;
            _.writeln("trace", s);
        };


        /**
         * @todo Logs a tabular representation of the given data.
         * Fall back to just logging the argument if it can’t be parsed as tabular.
         */
        console.table = function table(tabularData, properties) {
            console.log(tabularData);
        };


        /**
         * Logs a message with the visual "warning" representation.
         */
        console.warn = function warn(...args) {
            const s = _.formatArguments(args);
            _.writeln("warn", s);
        };

        return console;
    }));
})(this);
