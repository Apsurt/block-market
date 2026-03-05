package com.apsurt.blockmarket

import com.apsurt.blockmarket.command.*
import com.apsurt.blockmarket.engine.MarketOrchestrator
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.LoggerFactory

object BlockMarket : ModInitializer {
    private val logger = LoggerFactory.getLogger("block-market")

    val orchestrator = MarketOrchestrator()

    override fun onInitialize() {
        logger.info("Initializing the Free Market!")

        // Register all commands cleanly
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            WalletCommand.register(dispatcher)
            AdminCommand.register(dispatcher)
            MarketCommand.register(dispatcher)
            PayCommand.register(dispatcher)
        }
    }
}