package com.apsurt.blockmarket

import com.apsurt.blockmarket.command.*
import com.apsurt.blockmarket.engine.Order
import com.apsurt.blockmarket.data.MarketState
import com.apsurt.blockmarket.engine.MarketOrchestrator
import com.apsurt.blockmarket.engine.OrderSide
import com.apsurt.blockmarket.engine.OrderType
import com.apsurt.blockmarket.network.MarketSyncPayload
import com.apsurt.blockmarket.network.OpenAssetPayload
import com.apsurt.blockmarket.network.MarketOverviewPayload
import com.apsurt.blockmarket.network.RequestHomePayload
import com.apsurt.blockmarket.network.PlaceOrderPayload

import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.text.Text

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

import org.slf4j.LoggerFactory

object BlockMarket : ModInitializer {
    private val logger = LoggerFactory.getLogger("block-market")

    val orchestrator = MarketOrchestrator()

    override fun onInitialize() {
        logger.info("Initializing the Block Market")

        // 1. Register payloads
        PayloadTypeRegistry.playS2C().register(MarketSyncPayload.ID, MarketSyncPayload.CODEC)

        PayloadTypeRegistry.playS2C().register(MarketOverviewPayload.ID, MarketOverviewPayload.CODEC)

        PayloadTypeRegistry.playC2S().register(OpenAssetPayload.ID, OpenAssetPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(OpenAssetPayload.ID) { payload, context ->
            val player = context.player()
            val assetId = payload.assetId

            // Fetch the specific asset data
            val balance = orchestrator.walletManager.getBalance(player.uuid)
            val (liveBids, liveAsks) = orchestrator.getTopOrders(assetId, 50)

            val syncPayload = MarketSyncPayload(
                assetId = assetId,
                playerBalance = balance,
                bids = liveBids,
                asks = liveAsks
            )

            // Send the detailed AssetScreen payload back to the player
            ServerPlayNetworking.send(player, syncPayload)
        }

        PayloadTypeRegistry.playC2S().register(RequestHomePayload.ID, RequestHomePayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(RequestHomePayload.ID) { _, context ->
            val player = context.player()
            val overview = orchestrator.getMarketOverview(player.uuid)
            ServerPlayNetworking.send(player, overview)
        }

        PayloadTypeRegistry.playC2S().register(PlaceOrderPayload.ID, PlaceOrderPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(PlaceOrderPayload.ID) { payload, context ->
            val player = context.player()
            val uuid = player.uuid

            val assetId = payload.assetId
            val isBuy = payload.isBuy
            val isMarket = payload.isMarket
            val price = payload.price
            val shares = payload.shares

            val itemIdentifier = Identifier.of(assetId)
            val item = Registries.ITEM.get(itemIdentifier)

            var passesSecurityCheck = true

            if (isBuy) {
                // Check Wallet Balance
                val balance = orchestrator.walletManager.getBalance(uuid)
                val estimatedCost = if (isMarket) 0L else price * shares

                // For Limit orders, we know exactly how much they need.
                // (For Market orders, the orchestrator handles checking balance during the book sweep)
                if (!isMarket && balance < estimatedCost) {
                    passesSecurityCheck = false
                    player.sendMessage(Text.literal("§cYou don't have enough coins for this limit order!"), false)
                }
            } else {
                // Check Minecraft Inventory for Sellers
                val itemCount = player.inventory.count(item)
                if (itemCount < shares) {
                    passesSecurityCheck = false
                    player.sendMessage(Text.literal("§cYou don't have enough $assetId in your inventory!"), false)
                }
            }

            // --- EXECUTION & UI REFRESH ---
            if (passesSecurityCheck) {

                // ADD THIS TO DEDUCT THE ITEMS:
                if (!isBuy && !player.isCreative) {
                    // This tells Minecraft to search the inventory for this specific item
                    // and physically delete 'shares' amount of it!
                    player.inventory.remove({ it.isOf(item) }, shares, player.inventory)
                }

                // Map the booleans to your domain Enums
                val orderSide = if (isBuy) OrderSide.BUY else OrderSide.SELL
                val orderType = if (isMarket) OrderType.MARKET else OrderType.LIMIT

                // Construct the Order exactly as your data class expects
                val newOrder = Order(
                    ownerId = player.uuid,
                    assetId = assetId, // Assuming AssetId is a typealias for String, otherwise wrap it: AssetId(assetId)
                    side = orderSide,
                    type = orderType,
                    price = price,     // Assuming Coins is a typealias for Long
                    initialAmount = shares
                )

                // Pass the correctly formatted Order object to your Orchestrator
                val trades = orchestrator.placeOrder(newOrder)

                // Fetch the newly updated order book and wallet balance
                val newBalance = orchestrator.walletManager.getBalance(uuid)
                val (liveBids, liveAsks) = orchestrator.getTopOrders(assetId, 50)

                // Fire the Sync Packet back to the player to instantly refresh their UI!
                val syncPayload = MarketSyncPayload(
                    assetId = assetId,
                    playerBalance = newBalance,
                    bids = liveBids,
                    asks = liveAsks
                )
                ServerPlayNetworking.send(player, syncPayload)
            }
        }

        // 2. Load the Persistent Data when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            val state = MarketState.getServerState(server)
            orchestrator.walletManager.loadState(state)
            logger.info("Block Market balances loaded successfully!")
        }

        // 3. Register all commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            AdminCommand.register(dispatcher)
            UserCommand.register(dispatcher)
            // TODO: REMOVE BEFORE PRODUCTION
            DebugCommand.register(dispatcher)
        }
    }
}