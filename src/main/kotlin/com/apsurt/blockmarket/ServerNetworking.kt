package com.apsurt.blockmarket

import com.apsurt.blockmarket.network.*
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

object ServerNetworking {

    fun registerPayloads() {
        // Server-To-Client (S2C)
        PayloadTypeRegistry.playS2C().register(MarketSyncPayload.ID, MarketSyncPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(MarketOverviewPayload.ID, MarketOverviewPayload.CODEC)

        // Client-To-Server (C2S)
        PayloadTypeRegistry.playC2S().register(OpenAssetPayload.ID, OpenAssetPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(RequestHomePayload.ID, RequestHomePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(PlaceOrderPayload.ID, PlaceOrderPayload.CODEC)
    }

    fun registerReceivers() {
        // Route to Open Asset logic
        ServerPlayNetworking.registerGlobalReceiver(OpenAssetPayload.ID) { payload, context ->
            TradeService.handleOpenAsset(context.player(), payload.assetId)
        }

        // Route to Home Screen logic
        ServerPlayNetworking.registerGlobalReceiver(RequestHomePayload.ID) { _, context ->
            TradeService.handleRequestHome(context.player())
        }

        // Route to Order Execution logic
        ServerPlayNetworking.registerGlobalReceiver(PlaceOrderPayload.ID) { payload, context ->
            TradeService.handlePlaceOrder(context.player(), payload)
        }
    }
}