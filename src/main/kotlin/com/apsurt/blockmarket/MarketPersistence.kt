package com.apsurt.blockmarket

import com.apsurt.blockmarket.data.MarketState
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

object MarketPersistence {

    fun registerEvents() {
        // Load data on startup
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            val state = MarketState.getServerState(server)
            BlockMarket.orchestrator.walletManager.loadState(state)
            BlockMarket.logger.info("Block Market loaded successfully with ${state.balances.size} player balances.")
        }

        // Save data on shutdown (Crucial for persistence!)
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            val state = MarketState.getServerState(server)
            state.markDirty() // Ensure Minecraft knows the state needs to be written to disk
            BlockMarket.logger.info("Block Market shutting down. Saved ${state.balances.size} player balances.")
        }
    }
}