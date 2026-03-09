package com.apsurt.blockmarket.client.ui.screen

import com.apsurt.blockmarket.client.ui.widget.OrderBookWidget
import com.apsurt.blockmarket.network.MarketSyncPayload
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.Locale.getDefault

class AssetScreen(private val data: MarketSyncPayload) : Screen(Text.literal("Market: ${data.assetId}")) {

    private val backgroundWidth = 320
    private val backgroundHeight = 220
    private var x: Int = 0
    private var y: Int = 0

    private lateinit var orderBookWidget: OrderBookWidget

    // --- TRADE PANEL STATE ---
    private var isBuyTab = true      // True = Buy, False = Sell
    private var isMarketOrder = true // True = Market, False = Limit

    private lateinit var priceField: TextFieldWidget
    private lateinit var sharesField: TextFieldWidget
    private lateinit var modeButton: ButtonWidget

    override fun init() {
        super.init()
        this.x = (this.width - backgroundWidth) / 2
        this.y = (this.height - backgroundHeight) / 2

        orderBookWidget = OrderBookWidget(
            x = this.x + 15, y = this.y + 60, width = 130, height = this.backgroundHeight - 75,
            data = this.data, textRenderer = this.textRenderer
        )

        val panelX = this.x + 160
        val panelY = this.y + 45
        val panelWidth = 145

        // --- TABS (Buy / Sell) ---
        val buyTab = ButtonWidget.builder(Text.literal("Buy")) { _ ->
            isBuyTab = true
            updateOptimalPrice()
        }.dimensions(panelX, panelY, 71, 20).build()

        val sellTab = ButtonWidget.builder(Text.literal("Sell")) { _ ->
            isBuyTab = false
            updateOptimalPrice()
        }.dimensions(panelX + 74, panelY, 71, 20).build()

        // --- MODE TOGGLE (Limit / Market) ---
        modeButton = ButtonWidget.builder(Text.literal("Limit")) { button ->
            isMarketOrder = !isMarketOrder
            button.message = Text.literal(if (isMarketOrder) "Market" else "Limit")

            if (isMarketOrder) {
                priceField.text = "Auto"
                priceField.setEditable(false)
            } else {
                priceField.setEditable(true)
                updateOptimalPrice()
            }
        }.dimensions(panelX + 85, panelY + 27, 60, 16).build()

        // --- TEXT FIELDS ---
        priceField = TextFieldWidget(this.textRenderer, panelX, panelY + 45, panelWidth, 16, Text.empty())
        priceField.setTextPredicate { text -> text.isEmpty() || text == "Auto" || text.matches(Regex("^\\d+$")) }

        sharesField = TextFieldWidget(this.textRenderer, panelX, panelY + 79, panelWidth, 16, Text.empty())
        sharesField.setTextPredicate { text -> text.isEmpty() || text.matches(Regex("^\\d+$")) }

        // --- TRADE BUTTON ---
        val tradeButton = ButtonWidget.builder(Text.literal("Trade")) { _ ->
            val price = if (isMarketOrder) 0L else (priceField.text.toLongOrNull() ?: 0L)
            val shares = sharesField.text.toIntOrNull() ?: 0

            if ((isMarketOrder || price > 0) && shares > 0) {
                val modeStr = if (isMarketOrder) "MARKET" else "LIMIT"
                val typeStr = if (isBuyTab) "BUY" else "SELL"
                // TODO: Send Custom Payload to Server to place order
                println("Sending Order -> Type: $typeStr, Mode: $modeStr, Price: $price, Shares: $shares")
            }
        }.dimensions(panelX, panelY + 120, panelWidth, 20).build()

        this.addDrawableChild(buyTab)
        this.addDrawableChild(sellTab)
        this.addDrawableChild(modeButton)
        this.addDrawableChild(priceField)
        this.addDrawableChild(sharesField)
        this.addDrawableChild(tradeButton)

        // Initialize with optimal limit price
        updateOptimalPrice()
    }

    // Automatically fills the price field crossing the spread
    private fun updateOptimalPrice() {
        if (isMarketOrder) return
        val bestPrice = if (isBuyTab) {
            data.asks.minByOrNull { it.price }?.price // Lowest Ask
        } else {
            data.bids.maxByOrNull { it.price }?.price // Highest Bid
        }
        priceField.text = bestPrice?.toString() ?: ""
    }

    // Calculates total cost, walking the order book if it's a Market order
    private fun getEstimatedTotal(): Long {
        val shares = sharesField.text.toLongOrNull() ?: 0L
        if (shares <= 0) return 0L

        if (!isMarketOrder) {
            val price = priceField.text.toLongOrNull() ?: 0L
            return price * shares
        }

        // Market Order: Walk the book
        var remaining = shares
        var total = 0L
        val book = if (isBuyTab) data.asks.sortedBy { it.price } else data.bids.sortedByDescending { it.price }

        for (order in book) {
            val take = minOf(remaining, order.amount.toLong())
            total += take * order.price
            remaining -= take
            if (remaining == 0L) break
        }

        // If not enough liquidity, estimate the rest using the last available price
        if (remaining > 0L && book.isNotEmpty()) {
            total += remaining * book.last().price
        }
        return total
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(context, mouseX, mouseY, delta)
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, -0xefeff0)

        val borderColor = -0x555556
        context.fill(x, y, x + backgroundWidth, y + 1, borderColor)
        context.fill(x, y + backgroundHeight - 1, x + backgroundWidth, y + backgroundHeight, borderColor)
        context.fill(x, y, x + 1, y + backgroundHeight, borderColor)
        context.fill(x + backgroundWidth - 1, y, x + backgroundWidth, y + backgroundHeight, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        // Header
        val itemIdentifier = Identifier.of(data.assetId)
        val item = Registries.ITEM.get(itemIdentifier)
        context.drawItem(ItemStack(item), x + 10, y + 6)

        val assetNameText = data.assetId.substringAfter(':')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
        context.drawText(this.textRenderer, assetNameText, x + 30, y + 11, 0xFFFFFFFF.toInt(), false)

        val balanceText = "Balance: ${data.playerBalance} Coins"
        context.drawText(this.textRenderer, balanceText, x + backgroundWidth - this.textRenderer.getWidth(balanceText) - 10, y + 11, 0xFF55FF55.toInt(), false)

        context.fill(x + 10, y + 25, x + backgroundWidth - 10, y + 26, 0xFF555556.toInt())

        // Order Book
        context.drawText(this.textRenderer, "Order Book", x + 15, y + 32, 0xFFAAAAAA.toInt(), false)
        context.drawText(this.textRenderer, "PRICE", x + 15, y + 46, 0xFF777777.toInt(), false)
        context.drawText(this.textRenderer, "SHARES", x + 145 - this.textRenderer.getWidth("SHARES"), y + 46, 0xFF777777.toInt(), false)
        orderBookWidget.render(context, mouseX, mouseY, delta)

        // Trade Panel
        val panelX = this.x + 160
        val panelY = this.y + 45

        // Active tab underline
        if (isBuyTab) {
            context.fill(panelX, panelY + 21, panelX + 71, panelY + 23, 0xFF55FF55.toInt())
        } else {
            context.fill(panelX + 74, panelY + 21, panelX + 145, panelY + 23, 0xFFFF5555.toInt())
        }

        // Labels
        val priceLabel = if (isMarketOrder) "Market Order" else "Limit Price (¢)"
        context.drawText(this.textRenderer, priceLabel, panelX, panelY + 31, 0xFFAAAAAA.toInt(), false)
        context.drawText(this.textRenderer, "Shares", panelX, panelY + 67, 0xFFAAAAAA.toInt(), false)
        context.drawText(this.textRenderer, "Total", panelX, panelY + 105, 0xFFAAAAAA.toInt(), false)

        // Total Cost Rendering
        val totalCost = getEstimatedTotal()
        val prefix = if (isMarketOrder && totalCost > 0) "~" else "" // Indicate estimation for market
        val totalText = "$prefix${totalCost}¢"
        val totalColor = if (isBuyTab) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
        context.drawText(this.textRenderer, totalText, panelX + 145 - this.textRenderer.getWidth(totalText), panelY + 105, totalColor, false)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        // We unpack the Click record to pass the raw numbers to our custom widget
        if (orderBookWidget.onMouseClicked(click.x(), click.y(), click.button())) return true
        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        if (orderBookWidget.onMouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY)) return true
        return super.mouseDragged(click, deltaX, deltaY)
    }

    override fun mouseReleased(click: Click): Boolean {
        if (orderBookWidget.onMouseReleased(click.x(), click.y(), click.button())) return true
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (orderBookWidget.onMouseScrolled(mouseX, mouseY, verticalAmount)) return true
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun shouldPause(): Boolean = false
}