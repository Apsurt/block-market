package com.apsurt.blockmarket

import com.apsurt.blockmarket.command.*
import com.apsurt.blockmarket.data.MarketState
import com.apsurt.blockmarket.engine.MarketOrchestrator
import com.apsurt.blockmarket.network.MarketSyncPayload
import com.apsurt.blockmarket.network.MarketOverviewPayload

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

import org.slf4j.LoggerFactory

object BlockMarket : ModInitializer {
    private val logger = LoggerFactory.getLogger("block-market")

    val orchestrator = MarketOrchestrator()

    override fun onInitialize() {
        logger.info("Initializing the Block Market")

        PayloadTypeRegistry.playS2C().register(MarketSyncPayload.ID, MarketSyncPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(MarketOverviewPayload.ID, MarketOverviewPayload.CODEC)

        // 1. Load the Persistent Data when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            val state = MarketState.getServerState(server)
            orchestrator.walletManager.loadState(state)
            logger.info("Block Market balances loaded successfully!")
        }

        // 2. Register all commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            AdminCommand.register(dispatcher)
            UserCommand.register(dispatcher)
            // TODO: REMOVE BEFORE PRODUCTION
            DebugCommand.register(dispatcher)
        }
    }
}