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
    private val width: Int, // We'll reserve the rightmost 4 pixels for the scrollbar
    private val height: Int,
    data: MarketSyncPayload,
    private val textRenderer: TextRenderer
) : Drawable {

    private val rowHeight = 13
    private var scrollY = 0.0
    private val totalHeight: Int
    private var isDraggingScrollbar = false

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

        // --- NEW: INITIALIZE SCROLL TO THE SPREAD ---
        val maxScroll = max(0.0, (totalHeight - height).toDouble())
        val asksHeight = aggregatedAsks.size * rowHeight
        // Target Y is the bottom of the asks, minus half the widget height to center it
        val targetScroll = asksHeight - (height / 2.0) + 9.0 // 9 is half the spread height
        scrollY = MathHelper.clamp(targetScroll, 0.0, maxScroll)
    }

    private fun getMaxScroll() = max(0.0, (totalHeight - height).toDouble())

    // --- SCROLLBAR INTERACTION METHODS ---
    fun onMouseScrolled(mouseX: Double, mouseY: Double, verticalAmount: Double): Boolean {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            scrollY = MathHelper.clamp(scrollY - verticalAmount * rowHeight, 0.0, getMaxScroll())
            return true
        }
        return false
    }

    fun onMouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && getMaxScroll() > 0) {
            val trackX = x + width - 4 // Scrollbar is 4px wide on the right edge
            if (mouseX >= trackX && mouseX <= trackX + 4 && mouseY >= y && mouseY <= y + height) {
                isDraggingScrollbar = true
                return true
            }
        }
        return false
    }

    fun onMouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (isDraggingScrollbar && getMaxScroll() > 0) {
            val thumbHeight = max(20.0, (height.toDouble() / totalHeight.toDouble()) * height)
            val scrollPerPixel = getMaxScroll() / (height - thumbHeight)
            scrollY = MathHelper.clamp(scrollY + deltaY * scrollPerPixel, 0.0, getMaxScroll())
            return true
        }
        return false
    }

    fun onMouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false
            return true
        }
        return false
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val listWidth = width - 6 // Leave gap for the scrollbar

        context.enableScissor(x, y, x + listWidth, y + height)
        var currentY = this.y - scrollY.toInt()

        // --- DRAW ASKS (Top Half, Red) ---
        val reversedAsks = aggregatedAsks.reversed()
        val reversedAskCumSum = askCumSum.reversed()

        for (i in reversedAsks.indices) {
            val ask = reversedAsks[i]
            val cumVolume = reversedAskCumSum[i]

            val barWidth = ((cumVolume.toFloat() / maxVolume) * listWidth).toInt()
            context.fill(this.x + listWidth - barWidth, currentY, this.x + listWidth, currentY + rowHeight, 0x33FF5555)

            context.drawText(textRenderer, "${ask.key}¢", this.x + 2, currentY + 3, 0xFFFF5555.toInt(), false)
            val amountStr = ask.value.toString()
            context.drawText(textRenderer, amountStr, this.x + listWidth - textRenderer.getWidth(amountStr) - 2, currentY + 3, 0xFFFFFFFF.toInt(), false)

            currentY += rowHeight
        }

        // --- THE SPREAD (Center Divider) ---
        val spreadText = if (aggregatedAsks.isNotEmpty() && aggregatedBids.isNotEmpty()) {
            "${aggregatedAsks.first().key - aggregatedBids.first().key}¢ Spread"
        } else {
            "No Spread"
        }
        context.drawText(textRenderer, spreadText, this.x + (listWidth / 2) - (textRenderer.getWidth(spreadText) / 2), currentY + 4, 0xFFAAAAAA.toInt(), false)

        context.fill(this.x, currentY + 1, this.x + listWidth, currentY + 2, 0xFF444444.toInt())
        context.fill(this.x, currentY + 15, this.x + listWidth, currentY + 16, 0xFF444444.toInt())

        currentY += 18

        // --- DRAW BIDS (Bottom Half, Green) ---
        for (i in aggregatedBids.indices) {
            val bid = aggregatedBids[i]
            val cumVolume = bidCumSum[i]

            val barWidth = ((cumVolume.toFloat() / maxVolume) * listWidth).toInt()
            context.fill(this.x + listWidth - barWidth, currentY, this.x + listWidth, currentY + rowHeight, 0x3355FF55)

            context.drawText(textRenderer, "${bid.key}¢", this.x + 2, currentY + 3, 0xFF55FF55.toInt(), false)
            val amountStr = bid.value.toString()
            context.drawText(textRenderer, amountStr, this.x + listWidth - textRenderer.getWidth(amountStr) - 2, currentY + 3, 0xFFFFFFFF.toInt(), false)

            currentY += rowHeight
        }

        context.disableScissor()

        // --- DRAW SCROLLBAR ---
        val maxScroll = getMaxScroll()
        if (maxScroll > 0) {
            val trackX = x + width - 4
            // Scrollbar Track (Darker background)
            context.fill(trackX, y, trackX + 4, y + height, 0xFF222222.toInt())

            // Scrollbar Thumb (Lighter draggable part)
            val thumbHeight = max(20.0, (height.toDouble() / totalHeight.toDouble()) * height)
            val thumbY = y + (scrollY / maxScroll) * (height - thumbHeight)
            val thumbColor = if (isDraggingScrollbar) 0xFFAAAAAA.toInt() else 0xFF666666.toInt()
            context.fill(trackX, thumbY.toInt(), trackX + 4, (thumbY + thumbHeight).toInt(), thumbColor)
        }
    }
}