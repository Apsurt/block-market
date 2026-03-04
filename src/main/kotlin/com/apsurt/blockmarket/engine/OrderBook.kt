package com.apsurt.blockmarket.engine

import java.util.PriorityQueue
import kotlin.math.min

class OrderBook(val assetId: AssetId) {

    // BIDS (Buy Orders): Highest price wins. If prices tie, oldest timestamp wins.
    val bids = PriorityQueue(
        compareByDescending<Order> { it.price }
            .thenBy { it.timestamp }
    )

    // ASKS (Sell Orders): Lowest price wins. If prices tie, oldest timestamp wins.
    val asks = PriorityQueue(
        compareBy<Order> { it.price }
            .thenBy { it.timestamp }
    )

    fun processOrder(incomingOrder: Order, availableFunds: Coins = Long.MAX_VALUE): List<Trade> {
        val trades = mutableListOf<Trade>()
        var currentFunds = availableFunds
        var lastTradedPrice: Coins? = null

        if (incomingOrder.side == OrderSide.BUY) {
            // Sweeping the ASKS queue
            while (incomingOrder.amountRemaining > 0 && asks.isNotEmpty()) {
                val bestAsk = asks.peek()

                if (incomingOrder.type == OrderType.LIMIT && incomingOrder.price < bestAsk.price) break

                val tradePrice = bestAsk.price
                val desiredAmount = min(incomingOrder.amountRemaining, bestAsk.amountRemaining)

                val affordableAmount = if (incomingOrder.type == OrderType.MARKET) {
                    (currentFunds / tradePrice).toInt()
                } else {
                    desiredAmount
                }

                val actualTradeAmount = min(desiredAmount, affordableAmount)
                if (actualTradeAmount <= 0) break

                incomingOrder.filledAmount += actualTradeAmount
                bestAsk.filledAmount += actualTradeAmount
                currentFunds -= (actualTradeAmount * tradePrice)
                lastTradedPrice = tradePrice

                trades.add(Trade(assetId, incomingOrder.ownerId, bestAsk.ownerId, actualTradeAmount, tradePrice))

                if (bestAsk.isFullyFilled) asks.poll()
                if (actualTradeAmount < desiredAmount) break
            }

            // Handle the remainder
            enqueueRemainingOrder(incomingOrder, lastTradedPrice, bids)

        } else { // OrderSide.SELL
            // Sweeping the BIDS queue
            while (incomingOrder.amountRemaining > 0 && bids.isNotEmpty()) {
                val bestBid = bids.peek()

                if (incomingOrder.type == OrderType.LIMIT && incomingOrder.price > bestBid.price) break

                val tradePrice = bestBid.price
                val actualTradeAmount = min(incomingOrder.amountRemaining, bestBid.amountRemaining)

                incomingOrder.filledAmount += actualTradeAmount
                bestBid.filledAmount += actualTradeAmount
                lastTradedPrice = tradePrice

                trades.add(Trade(assetId, bestBid.ownerId, incomingOrder.ownerId, actualTradeAmount, tradePrice))

                if (bestBid.isFullyFilled) bids.poll()
            }

            // Handle the remainder
            enqueueRemainingOrder(incomingOrder, lastTradedPrice, asks)
        }

        return trades
    }

    /**
     * Helper function to handle partial fills and leftover amounts.
     * Converts exhausted MARKET orders to LIMIT orders at the last traded price.
     */
    private fun enqueueRemainingOrder(order: Order, lastTradedPrice: Coins?, targetQueue: PriorityQueue<Order>) {
        if (order.amountRemaining > 0) {
            if (order.type == OrderType.LIMIT) {
                targetQueue.add(order)
            } else if (order.type == OrderType.MARKET && lastTradedPrice != null) {
                // PARTIAL FILL CONVERSION
                order.type = OrderType.LIMIT
                order.price = lastTradedPrice
                targetQueue.add(order)
            }
        }
    }
}