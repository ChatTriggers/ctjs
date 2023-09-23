package com.chattriggers.ctjs.internal.commands

import com.chattriggers.ctjs.api.message.ChatLib
import java.io.File
import java.nio.file.Files

internal object Migration {
    private val migrationPatterns = listOf(
        """Renderer\.color\(""".toRegex() to "Renderer.getColor(",
        """register\((['"`])renderTileEntity['"`]""".toRegex(RegexOption.IGNORE_CASE) to "register($1renderBlockEntity$1",
        """\.setPacketClass\(""".toRegex() to ".setFilteredClass(",
        """\.setPacketClasses\(""".toRegex() to ".setFilteredClasses(",
        """EventLib\.cancel\((.+?)\)""".toRegex() to "cancel($1)",
        """EventLib\.getMessage\((.+?)\)""".toRegex() to "$1.message",
        """EntityLivingBase""".toRegex() to "LivingEntity",
        """\.getItemInSlot\(""".toRegex() to ".getStackInSlot(",
        """TileEntity""".toRegex() to "BlockEntity",
        """\.isDurationMax\(""".toRegex() to ".isInfinite(",
        """\.isDamagable\(""".toRegex() to ".isDamageable(",
        """\.getAllTileEntities\(\)""".toRegex() to ".getAllBlockEntities()",
        """\.getAllTileEntitiesOfType\(""".toRegex() to ".getAllBlockEntitiesOfType(",
        """\.isPowered\(\)""".toRegex() to ".isReceivingPower()",
        """\.getRedstoneStrength\(\)""".toRegex() to ".getReceivingPower()",
        """\.fromMCEnumFacing\(""".toRegex() to ".fromMC(",
        """Scoreboard\.getScoreboardTitle\(\)""".toRegex() to "Scoreboard.getTitle()",
        *listOf("Cape", "Jacket", "LeftSleeve", "RightSleeve", "LeftPantsLeg", "RightPantsLeg", "Hat").map {
            """Settings\.skin\.get$it\(\)""".toRegex() to "Settings.skin.is${it}Enabled"
        }.toTypedArray(),
        """GuiHandler\.openGui\((.+?)\)""".toRegex() to "Client.currentGui.set($1)",
        *listOf("Control", "Alt", "Shift").map {
            """\.is${it}Down\(\)""".toRegex() to ".has${it}Down()"
        }.toTypedArray(),
        """TabList\.getFooterMessage\(\)""".toRegex() to "TabList.getFooterComponent()",
        """TabList\.getHeaderMessage\(\)""".toRegex() to "TabList.getHeaderComponent()",
        """Client\.getChatGUI\(\)""".toRegex() to "Client.getChatGui()",
        """Config\.modulesFolder""".toRegex() to "ChatTriggers.MODULES_FOLDER",
    )

    private val removedTriggersRegex = let {
        val triggers = setOf(
            "screenshotTaken",
            "pickupItem",
            "chatComponentClicked",
            "chatComponentHovered",
            "renderSlot",
            "guiDrawBackground",
            "renderBossHealth",
            "renderDebug",
            "renderCrosshair",
            "renderHotbar",
            "renderExperience",
            "renderArmor",
            "renderHealth",
            "renderFood",
            "renderMountHealth",
            "renderAir",
            "renderPortal",
            "renderJumpBar",
            "renderChat",
            "renderHelmet",
            "renderHand",
            "renderScoreboard",
            "renderTitle",
            "preItemRender",
            "renderItemIntoGui",
            "noteBlockPlay",
            "noteBlockChange",
            "renderItemOverlayIntoGui",
            "renderSlotHighlight",
            "postRenderEntity",
            "postRenderTileEntity",
            "attackEntity",
            "hitBlock",
            "blockBreak",
            "guiMouseRelease"
        )

        """
            register\(['"`](${triggers.joinToString("|")})['"`]
        """.trimIndent().toRegex(RegexOption.IGNORE_CASE)
    }

    private val guiMouseClickRegex = """register\(['"`]guiMouseClick['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val guiMouseDragRegex = """register\(['"`]guiMouseDrag['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val guiOpenedRegex = """register\(['"`]guiOpened['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val renderBlockEntityRegex = """register\(['"`]renderBlockEntity['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val serverConnectRegex =
        """register\(['"`](serverConnect|serverDisconnect)['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val scrolledRegex = """register\(['"`]scrolled['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val dropItemRegex = """register\(['"`]dropItem['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val renderEntityRegex = """register\(['"`]renderEntity['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val spawnParticleRegex = """register\(['"`]spawnParticle['"`]""".toRegex(RegexOption.IGNORE_CASE)
    private val ctCopyRegex = """(ChatLib\.command\(['"`]ct copy|ChatLib\.say\(['"`]/ct copy)""".toRegex()
    private val getRiderRegex = """\.getRider\(\)""".toRegex()
    private val isAirborneRegex = """\.isAirborne\(\)""".toRegex()
    private val getDimensionRegex = """\.getDimension\(\)""".toRegex()

    fun migrate(input: File, output: File) {
        require(input.exists()) { "Input file \"$input\" does not exist" }

        if (input != output) {
            require(!output.exists()) { "Output file \"$output\" already exists" }
            output.parentFile.mkdirs()
        }

        input.walk(FileWalkDirection.TOP_DOWN).forEach {
            val dest = File(output, it.toRelativeString(input))

            if (it.isFile) {
                if (it.extension == "js") {
                    migrateFile(it, dest)
                } else {
                    Files.copy(it.toPath(), dest.toPath())
                }
            } else {
                dest.mkdir()
            }
        }
    }

    private fun migrateFile(input: File, output: File) {
        var text = input.readText()
        for ((regex, replacement) in migrationPatterns)
            text = text.replace(regex, replacement)
        checkForErrors(output, text)
        output.writeText(text)
    }

    private fun checkForErrors(file: File, inputText: String) {
        val text = inputText.replace("\r\n", "\n").replace('\r', '\n')
        val errors = collectErrors(text)
        val nlIndices = text.withIndex().filter { it.value == '\n' }.map { it.index }

        for (error in errors) {
            val index = nlIndices.indexOfFirst { it > error.first }
            val (line, column) = when (index) {
                -1 -> nlIndices.size to error.first - nlIndices.last() + 1
                0 -> 1 to error.first + 1
                else -> index to error.first - nlIndices[index - 1] + 1
            }

            ChatLib.chat("[${file.parentFile.name}${File.separatorChar}${file.name}:$line:$column] ${error.second}")
        }

        ChatLib.chat("&oNote: The migrated files are not guaranteed to work")
    }

    private fun collectErrors(text: String): List<Pair<Int, String>> {
        val errors = mutableListOf<Pair<Int, String>>()

        removedTriggersRegex.findAll(text).forEach {
            errors.add(it.groups[0]!!.range.first to "&6Warning: trigger \"${it.groups[1]!!.value}\" was removed")
        }

        fun Regex.warn(block: (MatchResult) -> String) {
            findAll(text).forEach {
                errors.add(it.groups[0]!!.range.first to "&6Warning: ${block(it)}")
            }
        }

        fun Regex.error(block: (MatchResult) -> String) {
            findAll(text).forEach {
                errors.add(it.groups[0]!!.range.first to "&cError: ${block(it)}")
            }
        }

        guiMouseClickRegex.warn {
            "trigger \"guiMouseClick\" activates for both click and release now, and takes an additional boolean " +
                "parameter to distinguish the two"
        }

        guiMouseDragRegex.warn { "trigger \"guiMouseDrag\" now takes two extra parameters at the start: dx and dy" }

        guiOpenedRegex.warn { "trigger \"guiOpened\" now takes a Screen as its first argument" }

        renderBlockEntityRegex.warn {
            "trigger \"renderBlockEntity\" no longer passes in the entity's position as an argument. Access it by " +
                "calling `<entity>.getBlockPos()`"
        }

        serverConnectRegex.warn {
            "trigger \"${it.groups[1]!!.value}\" no longer passes an event as the third parameter"
        }

        scrolledRegex.warn { "trigger \"scrolled\" now passes in the total delta, not just 1 or -1" }

        dropItemRegex.warn {
            "trigger \"dropItem\" now takes different parameters. Check the migration docs for more info"
        }

        renderEntityRegex.warn {
            "trigger \"renderEntity\" no longer takes the entity's position as an argument. Use `<entity>.getPos()` " +
                "instead"
        }

        spawnParticleRegex.warn {
            "trigger \"spawnParticle\" no longer passes in the particle type. Instead, compare the particle's " +
                "Minecraft class (i.e. `<particle>.toMC() instanceof ...`)"
        }

        ctCopyRegex.error { "`/ct copy` no longer exists. Use Client.copy() instead" }

        getRiderRegex.error { "`Entity.getRider()` no longer exists. Replace with `Entity.getRiders()`" }

        isAirborneRegex.error { "`Entity.isAirborne()` no longer exists" }

        getDimensionRegex.warn { "`Entity.getDimension()` now returns `Entity.DimensionType` instead of a number" }

        return errors
    }
}
