package com.apsurt.blockmarket.engine

import com.apsurt.blockmarket.BlockMarket
import com.apsurt.blockmarket.network.OrderEntry
import com.apsurt.blockmarket.network.MarketOverviewPayload
import com.apsurt.blockmarket.network.AssetSummary

import net.minecraft.registry.Registries
import java.util.UUID

class MarketOrchestrator {
    val walletManager = WalletManager()
    val escrowManager = EscrowManager()

    // One OrderBook for every unique item (AssetId)
    private val orderBooks = mutableMapOf<AssetId, OrderBook>()

    private fun getOrderBook(assetId: AssetId): OrderBook {
        return orderBooks.getOrPut(assetId) { OrderBook(assetId) }
    }

    fun getTopOrders(assetId: AssetId, limit: Int = 5): Pair<List<OrderEntry>, List<OrderEntry>> {
        val book = orderBooks[assetId] ?: return Pair(emptyList(), emptyList())

        // PriorityQueues don't guarantee strict iteration order, so we convert to a list and sort
        val topBids = book.bids.toList()
            .groupBy { it.price }
            .map { OrderEntry(it.key, it.value.sumOf { order -> order.amountRemaining }) }
            .sortedByDescending { it.price } // Highest buyers first
            .take(limit)

        val topAsks = book.asks.toList()
            .groupBy { it.price }
            .map { OrderEntry(it.key, it.value.sumOf { order -> order.amountRemaining }) }
            .sortedBy { it.price } // Lowest sellers first
            .take(limit)

        return Pair(topBids, topAsks)
    }

    fun getMarketOverview(playerUuid: UUID): MarketOverviewPayload {
        val balance = walletManager.getBalance(playerUuid)

        // Iterate over EVERY item registered in Minecraft
        val allAssets = Registries.ITEM.ids
            .filter { it.path != "air" } // Nobody needs to buy air!
            .map { identifier ->
                val assetId = identifier.toString()

                // Fetch the live book if it exists, otherwise it will just be null
                val book = orderBooks[assetId]

                val bestBid = book?.bids?.peek()?.price ?: 0L
                val bestAsk = book?.asks?.peek()?.price ?: 0L

                // TODO: Track trade history
                val volume24h = 0L
                val changePercent = 0.0

                AssetSummary(
                    assetId = assetId,
                    bestBid = bestBid,
                    bestAsk = bestAsk,
                    volume24h = volume24h,
                    changePercent = changePercent
                )
            }.sortedBy { it.assetId }

        return MarketOverviewPayload(balance, allAssets)
    }

    /**
     * The main entry point for a player wanting to buy or sell.
     */
    fun placeOrder(order: Order): List<Trade> {
        val book = getOrderBook(order.assetId)

        // 1. Pre-Transaction: Secure the resources (Locking)
        if (order.side == OrderSide.BUY) {
            if (order.type == OrderType.LIMIT) {
                val totalCost = order.price * order.initialAmount
                if (!walletManager.removeCoins(order.ownerId, totalCost)) {
                    throw IllegalStateException("Insufficient funds to place limit buy order.")
                }
                escrowManager.lockCoins(order.ownerId, totalCost)
            }
            // MARKET orders are "Blank Checks", so we don't lock anything upfront.
        } else {
            // SELL orders always lock the items.
            // (The Orchestrator assumes the caller verified the items exist in inventory/warehouse)
            escrowManager.lockItems(order.ownerId, order.assetId, order.initialAmount)
        }

        // 2. Matching: Run the engine logic
        val buyerFunds = if (order.side == OrderSide.BUY && order.type == OrderType.MARKET) {
            walletManager.getBalance(order.ownerId)
        } else {
            Long.MAX_VALUE
        }

        val trades = book.processOrder(order, buyerFunds)

        // 3. Settlement: Move the results to Inboxes
        for (trade in trades) {
            finalizeTrade(trade, order)
        }

        if (trades.isNotEmpty()) {
            BlockMarket.logger.info("Engine matched ${trades.size} trades for ${order.assetId}. Total volume moved: ${trades.sumOf { it.amount }} items.")
        } else {
            BlockMarket.logger.debug(
                "Order placed to book without immediate matches: {} {} for {}x {}",
                order.type,
                order.side,
                order.initialAmount,
                order.assetId
            )
        }

        return trades
    }

    private fun finalizeTrade(trade: Trade, incomingOrder: Order) {
        // Update Wallets/Escrows based on the trade results
        if (incomingOrder.side == OrderSide.BUY) {
            // Buyer is the one who initiated.
            // If it was a Limit Buy, unlock the spent portion from Escrow.
            if (incomingOrder.type == OrderType.LIMIT) {
                escrowManager.unlockCoins(trade.buyerId, trade.totalValue)
            } else {
                // If it was Market, take the money from their wallet right now.
                walletManager.removeCoins(trade.buyerId, trade.totalValue)
            }

            // Give seller their coins and buyer their items in their Inboxes
            escrowManager.depositToInbox(trade.sellerId, coinAmount = trade.totalValue)
            escrowManager.depositToInbox(trade.buyerId, assetId = trade.assetId, itemAmount = trade.amount)

            // Remove the sold items from the seller's locked vault
            escrowManager.unlockItems(trade.sellerId, trade.assetId, trade.amount)

        } else {
            // Seller is the one who initiated.
            // Items were already locked in escrow.
            escrowManager.unlockItems(trade.sellerId, trade.assetId, trade.amount)

            // If the buyer was a Limit order, their coins were already locked.
            escrowManager.unlockCoins(trade.buyerId, trade.totalValue)

            // Distribute the loot
            escrowManager.depositToInbox(trade.sellerId, coinAmount = trade.totalValue)
            escrowManager.depositToInbox(trade.buyerId, assetId = trade.assetId, itemAmount = trade.amount)
        }
    }
}