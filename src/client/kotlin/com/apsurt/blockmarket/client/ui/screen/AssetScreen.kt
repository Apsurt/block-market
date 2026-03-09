package com.apsurt.blockmarket.client.ui.screen

import com.apsurt.blockmarket.network.MarketSyncPayload
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class AssetScreen(private val data: MarketSyncPayload) : Screen(Text.literal("Market: ${data.assetId}")) {

    private val backgroundWidth = 320
    private val backgroundHeight = 220
    private var x: Int = 0
    private var y: Int = 0

    override fun init() {
        super.init()
        // Center the UI on the screen
        this.x = (this.width - backgroundWidth) / 2
        this.y = (this.height - backgroundHeight) / 2
    }

    // 1. Draw the panels BEHIND the widgets
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Let Minecraft apply the blur exactly once
        super.renderBackground(context, mouseX, mouseY, delta)

        // Draw the main UI Panel (Dark Slate)
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, -0xefeff0)

        // Draw the border manually
        val borderColor = -0x555556
        context.fill(x, y, x + backgroundWidth, y + 1, borderColor) // Top
        context.fill(x, y + backgroundHeight - 1, x + backgroundWidth, y + backgroundHeight, borderColor) // Bottom
        context.fill(x, y, x + 1, y + backgroundHeight, borderColor) // Left
        context.fill(x + backgroundWidth - 1, y, x + backgroundWidth, y + backgroundHeight, borderColor) // Right
    }

    // 2. Draw text and widgets ON TOP of the background
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // This automatically calls our renderBackground() above, then draws any buttons/widgets
        super.render(context, mouseX, mouseY, delta)

        // Draw Header Text
        val assetNameText = "Asset: ${data.assetId.substringAfter(':').uppercase()}"
        context.drawText(this.textRenderer, assetNameText, x + 10, y + 10, 0xFFFFFFFF.toInt(), false) // White

        val balanceText = "Balance: ${data.playerBalance} Coins"
        context.drawText(this.textRenderer, balanceText, x + backgroundWidth - this.textRenderer.getWidth(balanceText) - 10, y + 10, 0xFF55FF55.toInt(), false) // Green
    }

    override fun shouldPause(): Boolean = false
}