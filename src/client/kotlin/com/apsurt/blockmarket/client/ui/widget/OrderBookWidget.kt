package com.apsurt.blockmarket.client.ui.widget

import com.apsurt.blockmarket.network.MarketSyncPayload
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import kotlin.math.max

class OrderBookWidget(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val data: MarketSyncPayload,
    private val textRenderer: TextRenderer
) : Drawable {

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Draw Column Headers
        context.drawText(textRenderer, "PRICE", x, y, 0xFFAAAAAA.toInt(), false)
        context.drawText(textRenderer, "SHARES", x + width - textRenderer.getWidth("SHARES"), y, 0xFFAAAAAA.toInt(), false)

        val rowHeight = 13
        val maxRowsPerSide = 5 // TODO: Implement a scrollable ElementListWidget if orders exceed this limit
        val currentY = y + 15

        // --- 1. DATA AGGREGATION ---
        // Group orders by price and sum up the amounts
        val aggregatedAsks = data.asks.groupBy { it.price }
            .mapValues { it.value.sumOf { order -> order.amount } }
            .entries.sortedBy { it.key } // Lowest asks first (closest to spread)
            .take(maxRowsPerSide)

        val aggregatedBids = data.bids.groupBy { it.price }
            .mapValues { it.value.sumOf { order -> order.amount } }
            .entries.sortedByDescending { it.key } // Highest bids first (closest to spread)
            .take(maxRowsPerSide)

        // Calculate Cumulative Volumes (The math for the depth bars)
        val askCumSum = mutableListOf<Int>()
        var askSum = 0
        for (ask in aggregatedAsks) {
            askSum += ask.value
            askCumSum.add(askSum)
        }

        val bidCumSum = mutableListOf<Int>()
        var bidSum = 0
        for (bid in aggregatedBids) {
            bidSum += bid.value
            bidCumSum.add(bidSum)
        }

        // Find the absolute maximum volume to scale the bars correctly to the widget width
        val maxVolume = max(askCumSum.lastOrNull() ?: 1, bidCumSum.lastOrNull() ?: 1).coerceAtLeast(1)


        // --- 2. DRAW ASKS (Top Half, Red) ---
        // We draw from top to bottom, meaning we draw the highest price first (furthest from spread)
        val reversedAsks = aggregatedAsks.reversed()
        val reversedAskCumSum = askCumSum.reversed()

        // Push the asks down so they always hug the center spread, even if there are less than 5 orders
        val askStartY = currentY + ((maxRowsPerSide - reversedAsks.size) * rowHeight)

        for (i in reversedAsks.indices) {
            val ask = reversedAsks[i]
            val cumVolume = reversedAskCumSum[i]
            val drawY = askStartY + (i * rowHeight)

            // Draw Depth Bar (0x33 is ~20% opacity alpha)
            val barWidth = ((cumVolume.toFloat() / maxVolume) * width).toInt()
            context.fill(x + width - barWidth, drawY, x + width, drawY + rowHeight, 0x33FF5555)

            // Draw Text
            context.drawText(textRenderer, "${ask.key}¢", x + 2, drawY + 3, 0xFFFF5555.toInt(), false)
            val amountStr = ask.value.toString()
            context.drawText(textRenderer, amountStr, x + width - textRenderer.getWidth(amountStr) - 2, drawY + 3, 0xFFFFFFFF.toInt(), false)
        }


        // --- 3. THE SPREAD (Center Divider) ---
        val spreadY = currentY + (maxRowsPerSide * rowHeight) + 2
        val spreadText = if (aggregatedAsks.isNotEmpty() && aggregatedBids.isNotEmpty()) {
            "${aggregatedAsks.first().key - aggregatedBids.first().key}¢ Spread"
        } else {
            "No Spread"
        }
        context.drawText(textRenderer, spreadText, x + (width / 2) - (textRenderer.getWidth(spreadText) / 2), spreadY + 2, 0xFFAAAAAA.toInt(), false)

        // Two thin horizontal lines to frame the spread text
        context.fill(x, spreadY, x + width, spreadY + 1, 0xFF444444.toInt())
        context.fill(x, spreadY + 13, x + width, spreadY + 14, 0xFF444444.toInt())


        // --- 4. DRAW BIDS (Bottom Half, Green) ---
        // Bids are drawn top to bottom, highest price first (closest to spread)
        val bidStartY = spreadY + 16

        for (i in aggregatedBids.indices) {
            val bid = aggregatedBids[i]
            val cumVolume = bidCumSum[i]
            val drawY = bidStartY + (i * rowHeight)

            // Draw Depth Bar
            val barWidth = ((cumVolume.toFloat() / maxVolume) * width).toInt()
            context.fill(x + width - barWidth, drawY, x + width, drawY + rowHeight, 0x3355FF55)

            // Draw Text
            context.drawText(textRenderer, "${bid.key}¢", x + 2, drawY + 3, 0xFF55FF55.toInt(), false)
            val amountStr = bid.value.toString()
            context.drawText(textRenderer, amountStr, x + width - textRenderer.getWidth(amountStr) - 2, drawY + 3, 0xFFFFFFFF.toInt(), false)
        }
    }
}