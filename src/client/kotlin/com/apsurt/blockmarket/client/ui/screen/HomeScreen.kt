package com.apsurt.blockmarket.client.ui.screen

import com.apsurt.blockmarket.network.AssetSummary
import com.apsurt.blockmarket.network.MarketOverviewPayload
import com.apsurt.blockmarket.network.OpenAssetPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import kotlin.math.max

class HomeScreen(private val data: MarketOverviewPayload) : Screen(Text.literal("BlockMarket Home")) {

    private val bgWidth = 350
    private val bgHeight = 220
    private var x: Int = 0
    private var y: Int = 0

    private lateinit var searchField: TextFieldWidget
    private var filteredAssets: MutableList<AssetSummary> = data.assets.toMutableList()

    // --- SORTING & SCROLLING STATE ---
    private var sortColumn = "name"
    private var sortAscending = true
    private var scrollY = 0.0
    private val rowHeight = 22

    override fun init() {
        super.init()
        this.x = (this.width - bgWidth) / 2
        this.y = (this.height - bgHeight) / 2

        // Search Bar
        searchField = TextFieldWidget(textRenderer, x + 15, y + 35, 120, 16, Text.literal("Search..."))
        searchField.setChangedListener { query ->
            filteredAssets = data.assets.filter {
                it.assetId.substringAfter(':').contains(query, ignoreCase = true)
            }.toMutableList()
            scrollY = 0.0 // Reset scroll when searching
            applySort()
        }
        this.addDrawableChild(searchField)

        applySort()
    }

    private fun applySort() {
        val comparator = when (sortColumn) {
            "name" -> compareBy<AssetSummary> { it.assetId }
            "bid" -> compareBy { it.bestBid }
            "ask" -> compareBy { it.bestAsk }
            "change" -> compareBy { it.changePercent }
            "volume" -> compareBy { it.volume24h }
            else -> compareBy { it.assetId }
        }
        filteredAssets.sortWith(if (sortAscending) comparator else comparator.reversed())
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(context, mouseX, mouseY, delta)
        context.fill(x, y, x + bgWidth, y + bgHeight, -0xefeff0)

        val borderColor = -0x555556
        context.fill(x, y, x + bgWidth, y + 1, borderColor)
        context.fill(x, y + bgHeight - 1, x + bgWidth, y + bgHeight, borderColor)
        context.fill(x, y, x + 1, y + bgHeight, borderColor)
        context.fill(x + bgWidth - 1, y, x + bgWidth, y + bgHeight, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        // 1. Header: "Hello, {PlayerName}"
        val playerName = client?.player?.name?.string ?: "Trader"
        context.drawText(textRenderer, "Hello, $playerName", x + 15, y + 15, 0xFFFFFFFF.toInt(), false)

        val balanceText = "Balance: ${data.playerBalance} Coins"
        context.drawText(textRenderer, balanceText, x + bgWidth - textRenderer.getWidth(balanceText) - 15, y + 15, 0xFF55FF55.toInt(), false)

        context.fill(x + 10, y + 60, x + bgWidth - 10, y + 61, 0xFF555556.toInt())

        // 2. Clickable Column Headers
        val headerY = y + 67
        drawHeader(context, "ASSET", x + 40, headerY, "name", mouseX, mouseY)
        drawHeader(context, "BID", x + 140, headerY, "bid", mouseX, mouseY)
        drawHeader(context, "ASK", x + 190, headerY, "ask", mouseX, mouseY)
        drawHeader(context, "1D %", x + 245, headerY, "change", mouseX, mouseY)
        drawHeader(context, "VOL", x + 295, headerY, "volume", mouseX, mouseY)

        context.fill(x + 10, y + 80, x + bgWidth - 10, y + 81, 0xFF444444.toInt())

        // 3. Scrollable Asset List
        val listY = y + 82
        val listHeight = bgHeight - 90

        context.enableScissor(x + 10, listY, x + bgWidth - 10, listY + listHeight)

        var currentY = listY - scrollY.toInt()

        filteredAssets.forEach { asset ->
            renderAssetRow(context, asset, x + 10, currentY, mouseX, mouseY, listY, listY + listHeight)
            currentY += rowHeight
        }

        context.disableScissor()
    }

    private fun drawHeader(context: DrawContext, label: String, hX: Int, hY: Int, colId: String, mX: Int, mY: Int) {
        val isHovered = mX >= hX && mX <= hX + textRenderer.getWidth(label) + 10 && mY >= hY && mY <= hY + 10
        val baseColor = if (isHovered) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
        val color = if (sortColumn == colId) 0xFF55FF55.toInt() else baseColor

        val suffix = if (sortColumn == colId) (if (sortAscending) " ↑" else " ↓") else ""
        context.drawText(textRenderer, label + suffix, hX, hY, color, false)
    }

    private fun renderAssetRow(context: DrawContext, asset: AssetSummary, rX: Int, rY: Int, mX: Int, mY: Int, clipTop: Int, clipBottom: Int) {
        // Only calculate hover if the mouse is actually inside the visible scissor box
        val isHovered = mX >= rX && mX <= rX + bgWidth - 20 && mY >= rY && mY <= rY + rowHeight && mY >= clipTop && mY <= clipBottom
        if (isHovered) {
            context.fill(rX, rY, rX + bgWidth - 20, rY + rowHeight, 0x33FFFFFF)
        }

        // Icon
        val stack = ItemStack(Registries.ITEM.get(Identifier.of(asset.assetId)))
        context.drawItem(stack, rX + 5, rY + 3)

        // Asset Name
        val name = asset.assetId.substringAfter(':')
            .split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        context.drawText(textRenderer, name, rX + 30, rY + 7, 0xFFFFFFFF.toInt(), false)

        // Bid & Ask
        val bidText = if (asset.bestBid > 0) "${asset.bestBid}¢" else "-"
        val askText = if (asset.bestAsk > 0) "${asset.bestAsk}¢" else "-"
        context.drawText(textRenderer, bidText, rX + 130, rY + 7, 0xFF55FF55.toInt(), false)
        context.drawText(textRenderer, askText, rX + 180, rY + 7, 0xFFFF5555.toInt(), false)

        // 1D Change %
        val changeColor = if (asset.changePercent > 0) 0xFF55FF55.toInt() else if (asset.changePercent < 0) 0xFFFF5555.toInt() else 0xFFAAAAAA.toInt()
        val sign = if (asset.changePercent > 0) "+" else ""
        context.drawText(textRenderer, "$sign${asset.changePercent}%", rX + 235, rY + 7, changeColor, false)

        // Volume
        context.drawText(textRenderer, asset.volume24h.toString(), rX + 285, rY + 7, 0xFFAAAAAA.toInt(), false)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled)
        }

        // 1. Check Header Clicks for Sorting
        val headerY = y + 67
        if (click.y >= headerY && click.y <= headerY + 12) {
            val clickXInt = click.x.toInt() // Convert the Double to an Int

            val newCol = when {
                clickXInt in (x + 40)..(x + 100) -> "name"
                clickXInt in (x + 140)..(x + 180) -> "bid"
                clickXInt in (x + 190)..(x + 230) -> "ask"
                clickXInt in (x + 245)..(x + 285) -> "change"
                clickXInt in (x + 295)..(x + 335) -> "volume"
                else -> null
            }

            if (newCol != null) {
                if (sortColumn == newCol) sortAscending = !sortAscending else sortAscending = true
                sortColumn = newCol
                applySort()
                return true
            }
        }

        // 2. Check Row Clicks
        val listY = y + 82
        val listHeight = bgHeight - 90

        // Only allow clicks if the mouse is inside the list boundaries
        if (click.y >= listY && click.y <= listY + listHeight && click.x >= x + 10 && click.x <= x + bgWidth - 10) {
            val clickYOffset = click.y - listY + scrollY
            val clickedIndex = (clickYOffset / rowHeight).toInt()

            if (clickedIndex in filteredAssets.indices) {
                val clickedAsset = filteredAssets[clickedIndex]

                // Send the request to the server
                ClientPlayNetworking.send(OpenAssetPayload(clickedAsset.assetId))
                return true
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val listY = y + 82
        val listHeight = bgHeight - 90

        // Only scroll if hovering over the list
        if (mouseX >= x + 10 && mouseX <= x + bgWidth - 10 && mouseY >= listY && mouseY <= listY + listHeight) {
            val totalHeight = filteredAssets.size * rowHeight
            val maxScroll = max(0.0, (totalHeight - listHeight).toDouble())
            scrollY = MathHelper.clamp(scrollY - verticalAmount * (rowHeight / 2), 0.0, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun shouldPause(): Boolean = false
}