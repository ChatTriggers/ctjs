package com.chattriggers.ctjs.api.commands

// This really only exists so that we can hide away the DynamicCommand internals
// in the internals package
interface RootCommand {
    fun register()
}
