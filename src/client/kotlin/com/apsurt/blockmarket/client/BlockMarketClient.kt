package com.apsurt.blockmarket.client

import com.apsurt.blockmarket.client.ui.screen.AssetScreen
import com.apsurt.blockmarket.network.MarketSyncPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient

class BlockMarketClient : ClientModInitializer {

    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MarketSyncPayload.ID) { payload, context ->
            context.client().execute {
                MinecraftClient.getInstance().setScreen(AssetScreen(payload))
            }
        }
    }
}