package com.chattriggers.ctjs.compat

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.engine.module.Module
import com.terraformersmc.modmenu.ModMenu
import com.terraformersmc.modmenu.config.ModMenuConfig
import com.terraformersmc.modmenu.util.mod.Mod
import com.terraformersmc.modmenu.util.mod.ModrinthData
import com.terraformersmc.modmenu.util.mod.fabric.FabricIconHandler
import net.minecraft.client.texture.NativeImageBackedTexture

internal class ModMenuModule(private val module: Module) : Mod {
    private var childHasUpdate = false

    override fun getId(): String = module.name.lowercase()

    override fun getName(): String = module.metadata.name ?: module.name

    override fun getIcon(iconHandler: FabricIconHandler, i: Int): NativeImageBackedTexture {
        return ModMenu.MODS["chattriggers"]!!.getIcon(iconHandler, i)
    }

    override fun getDescription(): String = module.metadata.description ?: ""

    override fun getVersion(): String = module.metadata.version ?: "0.0.1"

    override fun getPrefixedVersion(): String = "v$version"

    override fun getAuthors(): List<String> = module.metadata.creator?.let(::listOf) ?: listOf()

    override fun getContributors(): List<String> = listOf()

    override fun getCredits(): List<String> = authors

    override fun getBadges(): Set<Mod.Badge> = setOf(Mod.Badge.CLIENT)

    override fun getWebsite(): String = "${CTJS.WEBSITE_ROOT}/modules/v/$name"

    override fun getIssueTracker(): String? = null

    override fun getSource(): String? = null

    override fun getParent(): String = "chattriggers"

    override fun getLicense(): Set<String> = setOf()

    override fun getLinks(): Map<String, String> = mapOf()

    override fun isReal(): Boolean = true

    override fun getModrinthData(): ModrinthData? = null

    override fun allowsUpdateChecks(): Boolean = false

    override fun setModrinthData(modrinthData: ModrinthData?) {

    }

    override fun setChildHasUpdate() {
        childHasUpdate = true
    }

    override fun getChildHasUpdate(): Boolean = childHasUpdate

    override fun isHidden(): Boolean = id in ModMenuConfig.HIDDEN_MODS.value

    internal companion object {
        fun addModulesToScreen(modules: List<Module>) {
            modules.map(::ModMenuModule).forEach {
                ModMenu.MODS[it.id] = it
                ModMenu.PARENT_MAP.put(ModMenu.MODS["chattriggers"], it)
            }
        }

        fun clearModules() {
            val modules = ModMenu.PARENT_MAP.removeAll(ModMenu.MODS["chattriggers"])

            modules.forEach {
                ModMenu.MODS.remove(it.id)
            }
        }
    }
}
