(function (global) {
    // A restricted version of Java.type
    global.Java = {
        type(arg) {
            if (typeof arg !== 'string')
                throw new Error('Java.type expects a string as its only object');
            if (arg.startsWith('net.minecraft') || arg.startsWith('com.mojang.blaze3d'))
                throw new Error(`Attempt to classload MC class ${arg} during Mixin application`);
            return Packages[arg]
        }
    }
    const JSLoader = Java.type('com.chattriggers.ctjs.engine.langs.js.JSLoader').INSTANCE;
    global.Console = JSLoader.getConsole();

    const MixinObj = Java.type('com.chattriggers.ctjs.launch.Mixin');
    const AtObj = Java.type('com.chattriggers.ctjs.launch.At');
    const SliceObj = Java.type('com.chattriggers.ctjs.launch.Slice');
    const LocalObj = Java.type('com.chattriggers.ctjs.launch.Local');
    const InjectObj = Java.type('com.chattriggers.ctjs.launch.Inject');

    global.Opcodes = Java.type('org.objectweb.asm.Opcodes');

    // Descriptor helpers
    global.void_ = 'V';
    global.boolean = 'Z';
    global.char = 'C';
    global.byte = 'B';
    global.short = 'S';
    global.int = 'I';
    global.float = 'F';
    global.long = 'J';
    global.double = 'D';

    global.desc = function desc(options) {
        let str = options.owner ?? '';
        str += options.name ?? '';
        if (options.type) {
            str += `:${options.type}`;
        } else if (options.args) {
            str += '(';
            for (let arg of options.args)
                str += arg;
            str += ')';
            str += options.ret ?? '';
        }
        return str;
    }

    global.J = function(strings, exprs) {
        // Transform 'a.b.c' into 'La/b/c;';
        let s = strings[0];
        for (let i = 0; exprs && i < exprs.length; i++) {
            s += exprs[i];
            s += strings[i + 1];
        }
        return `L${s.replaceAll('.', '/')};`;
    }

    // Type assertions
    const checkType = (value, expectedType) => {
        if (typeof expectedType === 'string') {
            if (typeof value !== expectedType)
                return false;
        } else {
            if (!(value instanceof expectedType))
                return false;
        }

        return true;
    };

    const assertType = (value, expectedType, obj) => {
        if (value !== null && value !== undefined && !checkType(value, expectedType))
            throw new Error(`${obj} must be of type ${expectedType}, found type ${typeof value}`);
    };

    const assertArrayType = (value, expectedType, obj) => {
        if (value !== null && value !== undefined && (!Array.isArray(value) || !value.every(v => checkType(v, expectedType))))
            throw new Error(`${obj} must be an array of ${expectedType}`);
    };

    class At {
        static Shift = {
            NONE: AtObj.Shift.NONE,
            BEFORE: AtObj.Shift.BEFORE,
            AFTER: AtObj.Shift.AFTER,
            BY: AtObj.Shift.BY,
        };

        constructor(obj) {
            if (typeof obj === 'string') {
                this._initProps({ value: obj });
            } else {
                this._initProps(obj);
            }
        }

        _initProps(obj) {
            const value = obj.value ?? throw new Error('At.value must be specified');
            const id = obj.id;
            const slice = obj.slice;
            const shift = obj.shift;
            const by = obj.by;
            let args = obj.args;
            if (typeof args === 'string')
                args = [args];
            const target = obj.target;
            const ordinal = obj.ordinal;
            const opcode = obj.opcode;
            const remap = obj.remap;

            assertType(id, 'string', 'At.id');
            assertType(value, 'string', 'At.value');
            assertType(slice, 'string', 'At.slice');
            assertType(shift, AtObj.Shift, 'At.shift');
            assertType(by, 'number', 'At.by');
            assertArrayType(args, 'string', 'At.args');
            assertType(target, 'string', 'At.target');
            assertType(ordinal, 'number', 'At.ordinal');
            assertType(opcode, 'number', 'At.opcode');
            assertType(remap, 'boolean', 'At.remap');

            this.atObj = new AtObj(
                value, id, slice, shift, by, args, target, ordinal, opcode, remap,
            )
        }
    }

    class Slice {
        constructor(obj) {
            if (typeof obj !== 'object')
                throw new Error('Slice() expects an object as its first argument');
            this._initProps(obj);
        }

        _initProps(obj) {
            const id = obj.id;
            const from = obj.from;
            const to = obj.to;

            assertType(id, 'string', 'Slice.id');
            assertType(from, At, 'Slice.from');
            assertType(to, At, 'Slice.to');

            this.sliceObj = new SliceObj(id, from?.atObj, to?.atObj);
        }
    }

    class Local {
        constructor(obj) {
            if (typeof obj === 'string') {
                this._initProps({ parameterName: obj });
            } else if (typeof obj === 'number') {
                this._initProps({ index: obj });
            } else {
                this._initProps(obj);
            }
        }

        _initProps(obj) {
            const print = obj.print;
            const parameterName = obj.parameterName;
            const index = obj.index;
            const ordinal = obj.ordinal;
            const type = obj.type;
            const mutable = obj.mutable;

            assertType(print, 'boolean', 'Local.print');
            assertType(parameterName, 'string', 'Local.parameterName');
            assertType(index, 'number', 'Local.index');
            assertType(ordinal, 'number', 'Local.ordinal');
            assertType(type, 'string', 'Local.type');
            assertType(mutable, 'boolean', 'Local.mutable');

            this.localObj = new LocalObj(print, parameterName, index, ordinal, type, mutable);
        }
    }

    class Mixin {
        constructor(obj) {
            if (typeof obj === 'string') {
                this._initProps({ target: obj });
            } else {
                this._initProps(obj);
            }
        }

        _initProps(obj) {
            const target = obj.target ?? throw new Error('Mixin.target must be specified');
            const priority = obj.priority;
            const remap = obj.remap;

            assertType(target, 'string', 'Mixin.target');
            assertType(priority, 'number', 'Mixin.priority');
            assertType(remap, 'boolean', 'Mixin.remap');

            this.mixinObj = new MixinObj(target, priority, remap);
        }

        inject(obj) {
            if (typeof obj !== 'object')
                throw new Error('Mixin.inject() expects an object as its first argument');
            return this._createInject(obj);
        }

        _createInject(obj) {
            const method = obj.method ?? throw new Error('Inject.method must be specified');
            const id = obj.id;
            let slices = obj.slice;
            if (slices instanceof Slice)
                slices = [slices];
            let at = obj.at;
            if (at instanceof At)
                at = [at]
            const cancellable = obj.cancellable;
            let locals = obj.locals;
            if (locals instanceof Local)
                locals = [locals];
            const remap = obj.remap;
            const require = obj.require;
            const expect = obj.expect;
            const allow = obj.allow;
            const constraints = obj.constraints;

            assertType(id, 'string', 'Inject.id');
            assertType(method, 'string', 'Inject.method');
            assertArrayType(slices, Slice, 'Inject.slice');
            assertArrayType(at, At, 'Inject.at');
            assertType(cancellable, 'boolean', 'Inject.cancellable');
            assertArrayType(locals, Local, 'Inject.locals');
            assertType(remap, 'boolean', 'Inject.remap');
            assertType(require, 'number', 'Inject.require');
            assertType(expect, 'number', 'Inject.expect');
            assertType(allow, 'number', 'Inject.allow');
            assertType(constraints, 'string', 'Inject.constraints');

            const injectObj = new InjectObj(
                method,
                id,
                slices?.map(s => s.sliceObj),
                at?.map(a => a.atObj),
                cancellable,
                locals?.map(l => l.localObj),
                remap,
                require,
                expect,
                allow,
                constraints,
            );

            return JSLoader.registerInjector(this.mixinObj, injectObj);
        }
    }

    global.Mixin = Mixin;
    global.Slice = Slice;
    global.At = At;
    global.Local = Local;


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
