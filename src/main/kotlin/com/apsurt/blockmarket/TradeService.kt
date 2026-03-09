package com.apsurt.blockmarket

import com.apsurt.blockmarket.engine.Order
import com.apsurt.blockmarket.engine.OrderSide
import com.apsurt.blockmarket.engine.OrderType
import com.apsurt.blockmarket.network.MarketSyncPayload
import com.apsurt.blockmarket.network.PlaceOrderPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object TradeService {

    fun handleOpenAsset(player: ServerPlayerEntity, assetId: String) {
        val balance = BlockMarket.orchestrator.walletManager.getBalance(player.uuid)
        val (liveBids, liveAsks) = BlockMarket.orchestrator.getTopOrders(assetId, 50)

        val syncPayload = MarketSyncPayload(
            assetId = assetId,
            playerBalance = balance,
            bids = liveBids,
            asks = liveAsks
        )
        ServerPlayNetworking.send(player, syncPayload)
    }

    fun handleRequestHome(player: ServerPlayerEntity) {
        val overview = BlockMarket.orchestrator.getMarketOverview(player.uuid)
        ServerPlayNetworking.send(player, overview)
    }

    fun handlePlaceOrder(player: ServerPlayerEntity, payload: PlaceOrderPayload) {
        val uuid = player.uuid
        val itemIdentifier = Identifier.of(payload.assetId)
        val item = Registries.ITEM.get(itemIdentifier)
        var passesSecurityCheck = true

        // --- 1. SECURITY CHECKS ---
        if (payload.isBuy) {
            val balance = BlockMarket.orchestrator.walletManager.getBalance(uuid)
            val estimatedCost = if (payload.isMarket) 0L else payload.price * payload.shares

            if (!payload.isMarket && balance < estimatedCost) {
                passesSecurityCheck = false
                player.sendMessage(Text.literal("§cYou don't have enough coins for this limit order!"), false)
            }
        } else {
            val itemCount = player.inventory.count(item)
            if (itemCount < payload.shares) {
                passesSecurityCheck = false
                player.sendMessage(Text.literal("§cYou don't have enough ${payload.assetId} in your inventory!"), false)
            }
        }

        // --- 2. EXECUTION & ESCROW ---
        if (passesSecurityCheck) {

            // Deduct the items physically from inventory if selling
            if (!payload.isBuy && !player.isCreative) {
                player.inventory.remove({ it.isOf(item) }, payload.shares, player.inventory)
            }

            // Map the booleans to your domain Enums
            val orderSide = if (payload.isBuy) OrderSide.BUY else OrderSide.SELL
            val orderType = if (payload.isMarket) OrderType.MARKET else OrderType.LIMIT

            // Construct the Order
            val newOrder = Order(
                ownerId = uuid,
                assetId = payload.assetId,
                side = orderSide,
                type = orderType,
                price = payload.price,
                initialAmount = payload.shares
            )

            // Execute the trade in the engine
            val trades = BlockMarket.orchestrator.placeOrder(newOrder)

            // --- 3. UI REFRESH ---
            val newBalance = BlockMarket.orchestrator.walletManager.getBalance(uuid)
            val (liveBids, liveAsks) = BlockMarket.orchestrator.getTopOrders(payload.assetId, 50)

            val syncPayload = MarketSyncPayload(
                assetId = payload.assetId,
                playerBalance = newBalance,
                bids = liveBids,
                asks = liveAsks
            )
            ServerPlayNetworking.send(player, syncPayload)
        }
    }
}