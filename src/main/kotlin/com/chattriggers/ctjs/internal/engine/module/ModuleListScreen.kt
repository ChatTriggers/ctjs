package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.api.FileLib
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class ModuleListScreen : Screen(Component.translatable("ctjs.ui.modules")) {
    private lateinit var moduleList: ModuleListWidget
    private lateinit var openFolderButton: Button
    private lateinit var deleteButton: Button
    private lateinit var closeButton: Button

    override fun init() {
        moduleList = ModuleListWidget(minecraft, width, height - 96, 32, 36)

        openFolderButton = Button.builder(Component.translatable("ctjs.ui.openModulesFolder")) {
            FileLib.openModulesFolder()
        }.width(200).pos(width / 2 - 100, height - 56).build()

        deleteButton = Button.builder(Component.translatable("ctjs.ui.delete")) {
            moduleList.selected?.let {
                minecraft.setScreen(
                    ConfirmScreen(
                        { confirmed ->
                            if (confirmed) {
                                ModuleManager.deleteModule(it.module.name)
                                onClose()
                            } else minecraft.setScreen(this)
                        },
                        Component.translatable("ctjs.ui.deleteConfirmation", it.module.name),
                        Component.translatable("ctjs.ui.noRevert").withColor(ARGB.colorFromFloat(1f, 1f, 0f, 0f)),
                        CommonComponents.GUI_PROCEED,
                        CommonComponents.GUI_CANCEL
                    )
                )
            }
        }.width(128).pos(width / 2 + 4, height - 32).build()

        closeButton = Button.builder(CommonComponents.GUI_BACK) { onClose() }
            .width(128)
            .pos(width / 2 - 132, height - 32).build()

        addRenderableWidget(openFolderButton)
        addRenderableWidget(deleteButton)
        addRenderableWidget(closeButton)
        addRenderableWidget(moduleList)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks)
        context.textWithBackdrop(
            font,
            this.title,
            this.width / 2,
            20,
            -1,
            -1
        )
        deleteButton.active = moduleList.selected != null
    }
}

class ModuleEntry(val textRenderer: Font, val module: Module) :
    ObjectSelectionList.Entry<ModuleEntry>() {
    override fun getNarration(): Component {
        return Component.literal(module.name)
    }

    override fun extractContent(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        hovered: Boolean,
        tickProgress: Float
    ) {
        val stack = context.pose()

        stack.pushMatrix()
        stack.scale(1.25f, 1.25f)
        context.textWithBackdrop(
            textRenderer,
            Component.literal(module.name),
            (x / 1.25f + 4).toInt(),
            (y / 1.25f + 4).toInt(),
            -1,
            -1
        )
        stack.popMatrix()

        module.metadata.creator?.let {
            context.textWithBackdrop(
                textRenderer,
                Component.translatable("ctjs.ui.byCreator", it),
                x + 4,
                y + contentHeight - textRenderer.lineHeight / 2 - 2,
                ARGB.colorFromFloat(1f, 0.8f, 0.8f, 0.8f),
                -1
            )
        }

        module.metadata.description?.let {
            context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                INFO_TEXTURE,
                x + contentWidth - 22, y + 2,
                16, 16,
            )

            // If the info icon is hovered
            if (mouseX >= x + contentWidth - 22 && mouseX <= x + contentWidth - 6 && mouseY >= y + 2 && mouseY <= y + 18) {
                context.setTooltipForNextFrame(
                    textRenderer,
                    textRenderer.split(FormattedText.of(it), contentWidth),
                    mouseX, mouseY
                )
            }
        }

        module.metadata.version?.let {
            context.text(
                textRenderer,
                it,
                x + contentWidth - textRenderer.width(it) - 4,
                y + contentHeight - textRenderer.lineHeight,
                ARGB.colorFromFloat(1f, 0.4f, 0.4f, 0.4f)
            )
        }
    }

    companion object {
        val INFO_TEXTURE: Identifier = Identifier.parse("icon/info")
    }
}

class ModuleListWidget(client: Minecraft, width: Int, height: Int, y: Int, itemHeight: Int) :
    ObjectSelectionList<ModuleEntry>(client, width, height, y, itemHeight) {
    init {
        clearEntries()
        for (module in ModuleManager.cachedModules) {
            addEntry(ModuleEntry(client.font, module))
        }
    }

    // Default is 220
    override fun getRowWidth(): Int = 320
}
