package com.apsurt.blockmarket.client.ui.widget

import com.apsurt.blockmarket.network.MarketSyncPayload
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.util.math.MathHelper
import kotlin.math.max

class OrderBookWidget(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    data: MarketSyncPayload, // Removed 'val' because it is only used during initialization
    private val textRenderer: TextRenderer
) : Drawable {

    private val rowHeight = 13
    private var scrollY = 0.0
    private val totalHeight: Int

    // --- 1. DATA AGGREGATION ---
    private val aggregatedAsks = data.asks.groupBy { it.price }
        .mapValues { it.value.sumOf { order -> order.amount } }
        .entries.sortedBy { it.key }

    private val aggregatedBids = data.bids.groupBy { it.price }
        .mapValues { it.value.sumOf { order -> order.amount } }
        .entries.sortedByDescending { it.key }

    private val askCumSum = mutableListOf<Int>()
    private val bidCumSum = mutableListOf<Int>()
    private val maxVolume: Int

    init {
        var askSum = 0
        for (ask in aggregatedAsks) {
            askSum += ask.value
            askCumSum.add(askSum)
        }

        var bidSum = 0
        for (bid in aggregatedBids) {
            bidSum += bid.value
            bidCumSum.add(bidSum)
        }

        maxVolume = max(askCumSum.lastOrNull() ?: 1, bidCumSum.lastOrNull() ?: 1).coerceAtLeast(1)
        totalHeight = (aggregatedAsks.size * rowHeight) + (aggregatedBids.size * rowHeight) + 18
    }

    // Catches the scroll wheel from the screen
    fun onMouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        // Check if mouse is hovering over the widget
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            val maxScroll = max(0.0, (totalHeight - height).toDouble())
            // verticalAmount is usually 1.0 or -1.0. We multiply by rowHeight so one click = one row
            scrollY = MathHelper.clamp(scrollY - verticalAmount * rowHeight, 0.0, maxScroll)
            return true
        }
        return false
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 1. Enable Scissor Test (This acts as a mask, hiding anything drawn outside the box)
        context.enableScissor(x, y, x + width, y + height)

        // 2. Offset the starting Y coordinate based on how far down we have scrolled
        var currentY = this.y - scrollY.toInt()

        // --- DRAW ASKS (Top Half, Red) ---
        val reversedAsks = aggregatedAsks.reversed()
        val reversedAskCumSum = askCumSum.reversed()

        for (i in reversedAsks.indices) {
            val ask = reversedAsks[i]
            val cumVolume = reversedAskCumSum[i]

            val barWidth = ((cumVolume.toFloat() / maxVolume) * width).toInt()
            context.fill(this.x + width - barWidth, currentY, this.x + width, currentY + rowHeight, 0x33FF5555)

            context.drawText(textRenderer, "${ask.key}¢", this.x + 2, currentY + 3, 0xFFFF5555.toInt(), false)
            val amountStr = ask.value.toString()
            context.drawText(textRenderer, amountStr, this.x + width - textRenderer.getWidth(amountStr) - 2, currentY + 3, 0xFFFFFFFF.toInt(), false)

            currentY += rowHeight
        }

        // --- THE SPREAD (Center Divider) ---
        val spreadText = if (aggregatedAsks.isNotEmpty() && aggregatedBids.isNotEmpty()) {
            "${aggregatedAsks.first().key - aggregatedBids.first().key}¢ Spread"
        } else {
            "No Spread"
        }
        context.drawText(textRenderer, spreadText, this.x + (width / 2) - (textRenderer.getWidth(spreadText) / 2), currentY + 4, 0xFFAAAAAA.toInt(), false)

        context.fill(this.x, currentY + 1, this.x + width, currentY + 2, 0xFF444444.toInt())
        context.fill(this.x, currentY + 15, this.x + width, currentY + 16, 0xFF444444.toInt())

        currentY += 18

        // --- DRAW BIDS (Bottom Half, Green) ---
        for (i in aggregatedBids.indices) {
            val bid = aggregatedBids[i]
            val cumVolume = bidCumSum[i]

            val barWidth = ((cumVolume.toFloat() / maxVolume) * width).toInt()
            context.fill(this.x + width - barWidth, currentY, this.x + width, currentY + rowHeight, 0x3355FF55)

            context.drawText(textRenderer, "${bid.key}¢", this.x + 2, currentY + 3, 0xFF55FF55.toInt(), false)
            val amountStr = bid.value.toString()
            context.drawText(textRenderer, amountStr, this.x + width - textRenderer.getWidth(amountStr) - 2, currentY + 3, 0xFFFFFFFF.toInt(), false)

            currentY += rowHeight
        }

        // 3. Disable the Scissor mask so the rest of the UI draws normally
        context.disableScissor()
    }
}