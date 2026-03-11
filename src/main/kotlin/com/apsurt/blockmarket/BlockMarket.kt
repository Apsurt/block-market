package com.apsurt.blockmarket

import com.apsurt.blockmarket.command.*
import com.apsurt.blockmarket.config.MarketConfig
import com.apsurt.blockmarket.engine.MarketOrchestrator

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.LoggerFactory

object BlockMarket : ModInitializer {
    val logger = LoggerFactory.getLogger("BlockMarket")
    val orchestrator = MarketOrchestrator()

    override fun onInitialize() {
        logger.info("Initializing the Block Market")
        // 1. Load mod config
        MarketConfig.load()

        // 2. Networking (Register packets and where they go)
        ServerNetworking.registerPayloads()
        ServerNetworking.registerReceivers()

        // 3. Persistence (Load/Save data)
        MarketPersistence.registerEvents()

        // 4. Commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            AdminCommand.register(dispatcher)
            UserCommand.register(dispatcher)
            // TODO: REMOVE BEFORE PRODUCTION
            DebugCommand.register(dispatcher)
        }
    }
}