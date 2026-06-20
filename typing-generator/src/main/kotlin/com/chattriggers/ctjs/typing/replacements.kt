package com.chattriggers.ctjs.typing

val replacements = listOf(
    $$"literal(s: string, callback: org.mozilla.javascript.Function): com.chattriggers.ctjs.api.CustomCommand$NodeBuilder;" to
            $$"literal(s: string, f: (node: com.chattriggers.ctjs.api.CustomCommand$NodeBuilder) => any): com.chattriggers.ctjs.api.CustomCommand$NodeBuilder;",

    $$"argument<T>(name: string, type: com.mojang.brigadier.arguments.ArgumentType<T>, callback: org.mozilla.javascript.Function): com.chattriggers.ctjs.api.CustomCommand$NodeBuilder;" to
            $$"argument<T>(name: string, type: com.mojang.brigadier.arguments.ArgumentType<T>, f: (node: com.chattriggers.ctjs.api.CustomCommand$NodeBuilder) => any): com.chattriggers.ctjs.api.CustomCommand$NodeBuilder;",

    "exec(callback: org.mozilla.javascript.Function): void;" to
            "exec(f: (ctx: com.mojang.brigadier.context.CommandContext<any>, obj: {[x: string]: any}) => any): void;",

    "register(name: string, callback: org.mozilla.javascript.Function): void;"
            to $$"register(name: string, callback: (node: com.chattriggers.ctjs.api.CustomCommand$NodeBuilder) => any): void;"
)
