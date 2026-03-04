package com.apsurt.blockmarket.engine

import java.util.UUID

typealias AssetId = String
typealias Coins = Long

enum class OrderSide {
    BUY,
    SELL
}

enum class OrderType {
    LIMIT,
    MARKET
}

/**
 * Represents a single request to buy or sell an asset.
 */
data class Order(
    val id: UUID = UUID.randomUUID(),
    val ownerId: UUID,
    val assetId: AssetId,
    val side: OrderSide,
    var type: OrderType,
    var price: Coins,
    val initialAmount: Int,
    var filledAmount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val amountRemaining: Int
        get() = initialAmount - filledAmount

    val isFullyFilled: Boolean
        get() = filledAmount >= initialAmount
}

/**
 * Represents a successful transaction between a buyer and a seller.
 * The OrderBook generates these when it finds a match.
 */
data class Trade(
    val assetId: AssetId,
    val buyerId: UUID,
    val sellerId: UUID,
    val amount: Int,
    val price: Coins,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalValue: Coins
        get() = amount * price
}