package com.apsurt.blockmarket

import com.apsurt.blockmarket.command.*
import com.apsurt.blockmarket.data.MarketState
import com.apsurt.blockmarket.engine.MarketOrchestrator
import com.apsurt.blockmarket.network.MarketSyncPayload
import com.apsurt.blockmarket.network.OpenAssetPayload
import com.apsurt.blockmarket.network.MarketOverviewPayload
import com.apsurt.blockmarket.network.RequestHomePayload

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