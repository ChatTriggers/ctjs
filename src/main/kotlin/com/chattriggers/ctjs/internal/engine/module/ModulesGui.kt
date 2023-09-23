package com.chattriggers.ctjs.internal.engine.module

import com.chattriggers.ctjs.api.client.Player
import com.chattriggers.ctjs.api.message.ChatLib
import com.chattriggers.ctjs.api.render.Renderer
import com.chattriggers.ctjs.api.render.Text
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UScreen

object ModulesGui : UScreen(unlocalizedName = "Modules") {
    private val window = object {
        var title = Text("Modules").setScale(2f).setShadow(true)
        var exit = Text(ChatLib.addColor("&cx")).setScale(2f)
        var height = 0f
        var scroll = 0f
    }

    override fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)

        Renderer.pushMatrix()

        val middle = Renderer.screen.getWidth() / 2f
        var width = Renderer.screen.getWidth() - 100f
        if (width > 500) width = 500f

        Renderer.drawRect(
            0x50000000,
            0f,
            0f,
            Renderer.screen.getWidth().toFloat(),
            Renderer.screen.getHeight().toFloat()
        )

        if (-window.scroll > window.height - Renderer.screen.getHeight() + 20)
            window.scroll = -window.height + Renderer.screen.getHeight() - 20
        if (-window.scroll < 0) window.scroll = 0f

        if (-window.scroll > 0) {
            Renderer.drawRect(0xaa000000, Renderer.screen.getWidth() - 20f, Renderer.screen.getHeight() - 20f, 20f, 20f)
            Renderer.drawString("^", Renderer.screen.getWidth() - 12f, Renderer.screen.getHeight() - 12f)
        }

        Renderer.drawRect(0x50000000, middle - width / 2f, window.scroll + 95f, width, window.height - 90)

        Renderer.drawRect(0xaa000000, middle - width / 2f, window.scroll + 95f, width, 25f)
        window.title.draw((middle - width / 2f + 5) / 2f, (window.scroll + 100f) / 2f)
        window.exit.setString(ChatLib.addColor("&cx")).draw((middle + width / 2f - 17) / 2f, (window.scroll + 99f) / 2f)

        window.height = 125f
        ModuleManager.cachedModules.forEach {
            window.height += it.draw(middle - width / 2f, window.scroll + window.height, width)
        }

        Renderer.popMatrix()
    }

    override fun onMouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        super.onMouseClicked(mouseX, mouseY, mouseButton)

        var width = Renderer.screen.getWidth() - 100f
        if (width > 500) width = 500f

        if (mouseX > Renderer.screen.getWidth() - 20 && mouseY > Renderer.screen.getHeight() - 20) {
            window.scroll = 0f
            return
        }

        if (mouseX > Renderer.screen.getWidth() / 2f + width / 2f - 25 && mouseX < Renderer.screen.getWidth() / 2f + width / 2f
            && mouseY > window.scroll + 95 && mouseY < window.scroll + 120
        ) {
            Player.toMC()?.closeScreen()
            return
        }

        ModuleManager.cachedModules.toList().forEach {
            it.click(mouseX, mouseY, width)
        }
    }

    override fun onMouseScrolled(delta: Double) {
        super.onMouseScrolled(delta)
        window.scroll += delta.toFloat()
    }
}
