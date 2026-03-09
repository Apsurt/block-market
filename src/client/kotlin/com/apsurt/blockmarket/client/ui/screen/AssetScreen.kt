package com.apsurt.blockmarket.client.ui.screen

import com.apsurt.blockmarket.client.ui.widget.OrderBookWidget
import com.apsurt.blockmarket.network.MarketSyncPayload
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class AssetScreen(private val data: MarketSyncPayload) : Screen(Text.literal("Market: ${data.assetId}")) {

    private val backgroundWidth = 320
    private val backgroundHeight = 220
    private var x: Int = 0
    private var y: Int = 0
    private lateinit var orderBookWidget: OrderBookWidget

    override fun init() {
        super.init()
        this.x = (this.width - backgroundWidth) / 2
        this.y = (this.height - backgroundHeight) / 2

        orderBookWidget = OrderBookWidget(
            x = this.x + 15, // Added 15px left margin
            y = this.y + 60, // Pushed down to leave room for static headers
            width = 130,     // Slightly narrower
            height = this.backgroundHeight - 75, // Leaves 15px bottom margin
            data = this.data,
            textRenderer = this.textRenderer
        )

        // Calculate best prices from the payload
        // Best Ask = Lowest price someone is willing to sell for
        val bestAsk = data.asks.minByOrNull { it.price }?.price?.toString() ?: "N/A"

        // Best Bid = Highest price someone is willing to buy for
        val bestBid = data.bids.maxByOrNull { it.price }?.price?.toString() ?: "N/A"

        // Right side panel coordinates
        val panelX = this.x + 170
        val panelY = this.y + 60

        // 1. Buy Button (Green text formatting §a)
        val buyButton = ButtonWidget.builder(Text.literal("§aBuy @ $bestAsk¢")) { _ ->
            // TODO: Open the Transaction Screen for Buying
            println("Buy clicked!")
        }.dimensions(panelX, panelY, 130, 20).build()

        // 2. Sell Button (Red text formatting §c)
        val sellButton = ButtonWidget.builder(Text.literal("§cSell @ $bestBid¢")) { _ ->
            // TODO: Open the Transaction Screen for Selling
            println("Sell clicked!")
        }.dimensions(panelX, panelY + 30, 130, 20).build()

        // Register the buttons so Minecraft handles rendering and clicking
        this.addDrawableChild(buyButton)
        this.addDrawableChild(sellButton)
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(context, mouseX, mouseY, delta)

        // Main Panel
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, -0xefeff0)

        // Border
        val borderColor = -0x555556
        context.fill(x, y, x + backgroundWidth, y + 1, borderColor)
        context.fill(x, y + backgroundHeight - 1, x + backgroundWidth, y + backgroundHeight, borderColor)
        context.fill(x, y, x + 1, y + backgroundHeight, borderColor)
        context.fill(x + backgroundWidth - 1, y, x + backgroundWidth, y + backgroundHeight, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // This calls renderBackground automatically, then draws the registered buttons
        super.render(context, mouseX, mouseY, delta)

        // Header Text
        val assetNameText = "Asset: ${data.assetId.substringAfter(':').uppercase()}"
        context.drawText(this.textRenderer, assetNameText, x + 10, y + 10, 0xFFFFFFFF.toInt(), false)

        val balanceText = "Balance: ${data.playerBalance} Coins"
        context.drawText(this.textRenderer, balanceText, x + backgroundWidth - this.textRenderer.getWidth(balanceText) - 10, y + 10, 0xFF55FF55.toInt(), false)

        // Draw a neat separator line under the header
        context.fill(x + 10, y + 25, x + backgroundWidth - 10, y + 26, 0xFF555556.toInt())

        // Draw a neat separator line under the main header
        context.fill(x + 10, y + 25, x + backgroundWidth - 10, y + 26, 0xFF555556.toInt())

        // Left Side: Order Book Titles
        context.drawText(this.textRenderer, "Order Book", x + 15, y + 32, 0xFFAAAAAA.toInt(), false)
        context.drawText(this.textRenderer, "PRICE", x + 15, y + 46, 0xFF777777.toInt(), false)
        context.drawText(this.textRenderer, "SHARES", x + 145 - this.textRenderer.getWidth("SHARES"), y + 46, 0xFF777777.toInt(), false)
        orderBookWidget.render(context, mouseX, mouseY, delta)

        // Right Side: Quick Trade Title
        context.drawText(this.textRenderer, "Quick Trade", x + 170, y + 32, 0xFFAAAAAA.toInt(), false)
    }

    // This catches the scroll wheel while the screen is open
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        // Pass it to our widget first. If the widget handles it (hovered), stop processing.
        if (orderBookWidget.onMouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun shouldPause(): Boolean = false
}