package com.chattriggers.ctjs.engine.langs.js

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.engine.IRegister
import com.chattriggers.ctjs.minecraft.objects.Gui
import com.chattriggers.ctjs.minecraft.objects.KeyBind
import com.chattriggers.ctjs.minecraft.objects.display.Display
import com.chattriggers.ctjs.minecraft.objects.display.DisplayLine
import com.chattriggers.ctjs.minecraft.wrappers.Client
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.option.KeyBinding
import org.mozilla.javascript.NativeObject

/*
 * This file holds the "glue" for this language.
 *
 * Certain classes have triggers inside of them that need to know what loader to use,
 * and that's where these implementations come in.
 */

object JSRegister : IRegister {
    override fun getImplementationLoader(): ILoader = JSLoader
}

class JSGui @JvmOverloads constructor(title: UTextComponent = UTextComponent("")) : Gui(title) {
    override fun getLoader(): ILoader = JSLoader
}

class JSDisplayLine : DisplayLine {
    constructor(text: String) : super(text)
    constructor(text: String, config: NativeObject) : super(text, config)

    override fun getLoader(): ILoader = JSLoader
}

class JSDisplay : Display {
    constructor() : super()
    constructor(config: NativeObject?) : super(config)

    override fun createDisplayLine(text: String): DisplayLine {
        return JSDisplayLine(text)
    }
}

class JSKeyBind : KeyBind {
    @JvmOverloads
    constructor(category: String, key: Int, description: String = "ChatTriggers") : super(category, key, description)
    constructor(keyBinding: KeyBinding) : super(keyBinding)

    override fun getLoader(): ILoader = JSLoader
}

object JSClient : Client() {
    override fun makeKeyBind(category: String, key: Int, description: String) = JSKeyBind(category, key, description)

    override fun makeKeyBind(keyBinding: KeyBinding) = JSKeyBind(keyBinding)
}
