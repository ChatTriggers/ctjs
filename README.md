<div style="text-align:center;">
  <p>
    <a href="https://chattriggers.com">
      <img src="https://chattriggers.com/assets/images/logo-final.png" width="546" alt="ChatTriggers.js" />
    </a>
  </p>
  <p>
    <a href="https://discord.gg/chattriggers">
      <img src="https://discordapp.com/api/guilds/119493402902528000/embed.png" alt="Discord" />
    </a>
  </p>
</div>

ChatTriggers (CT) is a framework for Minecraft that enables live scripting and client modification using JavaScript.

This version has been specifically written by me (srockw) for my own purposes, and it is NOT backwards compatible with
previous CT versions due to a couple of reasons:

- Most objects and libraries (such as ChatLib, TextComponent, Renderer) have been removed.
- Most of the registers have been removed.
- Custom mixins have been removed (but I might add them back later if I get the motivation).

These changes were made to keep the mod as simple as possible and easy to update to newer versions when they come out.
You can make your own libraries within modules or even fork the project to add them yourself if you want.
Making additional registers can require using Fabric events or sometimes mixins, so they're not able to be added through
modules currently, you can let me know in Discord if you have one you need, and I'll look into it.

> [!NOTE]
> If you want these libraries and registers, I recommend going to [DocilElm's fork](https://github.com/Synnerz/ctjs),
> but note that as of writing this it is not being updated.

### Getting Started

I don't currently have any plans to make releases, so the only way for you to download this mod would be
through [GitHub actions](https://github.com/srockw/ctjs/actions), which require you to login and will expire after a
certain period of time.

The currently supported version is 26.1.2.

### Writing Modules

To create a module, follow these steps:

1. Navigate to the `.minecraft/config/ChatTriggers/modules` folder. If you don't know where this is, you can also
   execute `/ct files`, which will open the correct directory automatically.
2. Create a new directory with the name of your module.
3. Inside the directory, create a file called `metadata.json`, and inside of that, put the following text:
    ```json
    {
        "name": "<module name>",
        "entry": "index.js"
    }
    ```
   This means that when our module first runs, CT will run the `index.js` file

   _Note that `<module name>` must match the name of your folder exactly!_
4. Create the `index.js` file, and put some code in there.
5. Do `/ct load` to reload all modules after modifying them.
6. (Optional, but recommended) After downloading the mod from the latest action you will have also gotten a file called
   `typings.d.ts`, the purpose of this file is to provide autocompletions and suggestions in an IDE. For example, in
   VSCode it is sufficient to have the file open while editing your JavaScript files, but it depends on the IDE.

### Documentation

Since the game is no longer obfuscated, you can access all the vanilla MC classes and methods by just going through
their path, such as `net.minecraft.client.Minecraft` and use all their methods as normal. The `typings.d.ts` file
mentioned previously will help you through this, but note that it does NOT include all classes available to you, I
recommend
going to [mcsrc.dev](https://mcsrc.dev) if there is something you can't find.

While most of the libraries have been removed, some essential ones still remain:

#### Register

You can easily view all available registers using the autocompletions from the `typings.d.ts` file, they should be
pretty self-explanatory, and besides `renderOverlay` the other ones starting with `render` are for 3D rendering.

```js
// Hide all messages including "something"
register("chat", (msg, event) => {
    if (msg.getString().includes("something")) {
        cancel(event);
    }
});
```

#### CustomKeyMapping

Allows you to create your own keybindings, which will show up in the options screen just as any other.

```js
const GLFW = org.lwjgl.glfw.GLFW;

// Create your own keybind.
// Note that the last parameter, the category, has to be a valid identifier, which cannot include spaces and it won't be translated. (So no nice looking categories, at least for now)
const myKey = CustomKeyMapping.register("My Key", GLFW.GLFW_KEY_P, "my_category"); // This returns a vanilla KeyMapping object.

// Can also get the KeyMapping object of other keys.
const vanillaSneak = CustomKeyMapping.find("key.sneak");

register("tick", () => {
    // Using the typings file you can easily see which methods are available through you.
    if (myKey.consumeClick()) {
        // Do something
    }
});
```

#### CustomCommand

Allows you to create brigadier commands:

```js
const IntegerArgumentType = com.mojang.brigadier.arguments.IntegerArgumentType;

CustomCommand.register("mytime", (node) => {
    node.literal("set", (node) => {
        node.literal("day", (node) => {
            node.exec((ctx) => {
                // called when '/mytime set day' is used
            });
        });

        node.literal("night", (node) => {
            node.exec((ctx) => {
                // called when '/mytime set night' is used
            });
        });
    });

    node.literal("add", (node) => {
        // here "time" is the name of this argument, 'IntegerArgumentType.integer()' defines the type of the argument, you can use any ArgumentType.
        node.argument("time", IntegerArgumentType.integer(), (node) => {
            node.exec((ctx) => {
                // called when '/mytime add <int>' is used

                // access the time parameter we defined earlier
                const time = IntegerArgumentType.getInteger(ctx, "time");
            });
        });
    });
});
```
