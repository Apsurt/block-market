package com.apsurt.blockmarket.client

import com.apsurt.blockmarket.client.ui.screen.AssetScreen
import com.apsurt.blockmarket.client.ui.screen.HomeScreen
import com.apsurt.blockmarket.network.MarketSyncPayload
import com.apsurt.blockmarket.network.MarketOverviewPayload
import com.apsurt.blockmarket.network.RequestHomePayload

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

class BlockMarketClient : ClientModInitializer {

    override fun onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(MarketSyncPayload.ID) { payload, context ->
            context.client().execute {
                MinecraftClient.getInstance().setScreen(AssetScreen(payload))
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(MarketOverviewPayload.ID) { payload, context ->
            context.client().execute {
                MinecraftClient.getInstance().setScreen(HomeScreen(payload))
            }
        }

        val openMarketKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.blockmarket.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KeyBinding.Category.create(Identifier.of("blockmarket", "main"))
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openMarketKey.wasPressed()) {
                if (client.currentScreen == null) {
                    ClientPlayNetworking.send(RequestHomePayload)
                }
            }
        }
    }
}