package com.chattriggers.ctjs.engine.langs.js

import com.chattriggers.ctjs.engine.ILoader
import com.chattriggers.ctjs.engine.IRegister
import com.chattriggers.ctjs.minecraft.objects.KeyBind
import com.chattriggers.ctjs.minecraft.wrappers.Client
import net.minecraft.client.option.KeyBinding

/*
 * This file holds the "glue" for this language.
 *
 * Certain classes have triggers inside of them that need to know what loader to use,
 * and that's where these implementations come in.
 */

object JSRegister : IRegister {
    override fun getImplementationLoader(): ILoader = JSLoader
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
