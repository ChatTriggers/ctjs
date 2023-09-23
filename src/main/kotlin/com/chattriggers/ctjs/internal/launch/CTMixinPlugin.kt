package com.chattriggers.ctjs.internal.launch

import com.chattriggers.ctjs.api.Mappings
import com.chattriggers.ctjs.engine.printTraceToConsole
import com.chattriggers.ctjs.internal.engine.module.ModuleManager
import com.llamalad7.mixinextras.MixinExtrasBootstrap
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo
import java.io.OutputStream
import java.io.PrintStream

class CTMixinPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String?) {
        redirectIO()

        Mappings.initialize()
        ModuleManager.setup()
        MixinExtrasBootstrap.init()

        try {
            DynamicMixinManager.initialize()
            DynamicMixinManager.applyAccessWideners()
        } catch (e: Throwable) {
            IllegalStateException("Error generating dynamic mixins", e).printTraceToConsole()
        }
    }

    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String?, mixinClassName: String?): Boolean = true

    override fun acceptTargets(myTargets: MutableSet<String>?, otherTargets: MutableSet<String>?) {
    }

    override fun getMixins(): MutableList<String>? = null

    override fun preApply(
        targetClassName: String?,
        targetClass: ClassNode?,
        mixinClassName: String?,
        mixinInfo: IMixinInfo?
    ) {
    }

    override fun postApply(
        targetClassName: String?,
        targetClass: ClassNode?,
        mixinClassName: String?,
        mixinInfo: IMixinInfo?
    ) {
    }

    private fun redirectIO() {
        // This mimics what net.minecraft.Bootstrap does with the output streams, but
        // this happens earlier and allows stderr to show up in latest.log (which is
        // required for @Local(print = true)). Note that we copy the relevant classes
        // to avoid class-loading anything in the MC package.
        System.setErr(LoggerPrintStream("STDERR", System.err))
        System.setOut(LoggerPrintStream("STDOUT", STDOUT))
    }

    private open class LoggerPrintStream(protected val name: String, out: OutputStream) : PrintStream(out) {
        override fun println(message: String?) {
            log(message)
        }

        override fun println(obj: Any?) {
            log(obj.toString())
        }

        override fun printf(format: String, vararg args: Any?): PrintStream = apply {
            log(format.format(args))
        }

        protected open fun log(message: String?) {
            LOGGER.info("[{}]: {}", name, message)
        }
    }

    companion object {
        private val STDOUT = System.out
        private val STDERR = System.out
        private val LOGGER = LoggerFactory.getLogger(CTMixinPlugin::class.java)

        @JvmStatic
        fun restoreOutputStreams() {
            System.setErr(STDERR)
            System.setOut(STDOUT)
        }
    }
}
