package com.apsurt.blockmarket.command

import com.apsurt.blockmarket.BlockMarket
import com.apsurt.blockmarket.engine.Order
import com.apsurt.blockmarket.engine.OrderSide
import com.apsurt.blockmarket.engine.OrderType
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.util.UUID
import kotlin.random.Random

// TODO: REMOVE THIS ENTIRE FILE BEFORE PRODUCTION
object DebugCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("bmdebug")
                .requires(MarketPermissions::isAdmin) // Still protect it behind admin rights

                .then(literal("populate")
                    .executes { context ->
                        val items = listOf("minecraft:diamond", "minecraft:emerald")
                        val botUuid = UUID.fromString("00000000-0000-0000-0000-000000000000") // A static "Bot" UUID

                        // Give the bot unlimited resources to back the orders
                        BlockMarket.orchestrator.walletManager.addCoins(botUuid, 1_000_000L)

                        items.forEach { itemId ->
                            // Create Asks (Sellers)
                            repeat(500) {
                                val price = Random.nextLong(200, 1000)
                                val amount = Random.nextInt(1, 10)

                                // Manually lock items for the bot so the Orchestrator doesn't fail
                                BlockMarket.orchestrator.escrowManager.lockItems(botUuid, itemId, amount)

                                BlockMarket.orchestrator.placeOrder(
                                    Order(
                                        ownerId = botUuid,
                                        assetId = itemId,
                                        side = OrderSide.SELL,
                                        type = OrderType.LIMIT,
                                        price = price,
                                        initialAmount = amount
                                    )
                                )
                            }

                            // Create Bids (Buyers)
                            repeat(500) {
                                val price = Random.nextLong(10, 250)
                                val amount = Random.nextInt(1, 10)

                                BlockMarket.orchestrator.placeOrder(
                                    Order(
                                        ownerId = botUuid,
                                        assetId = itemId,
                                        side = OrderSide.BUY,
                                        type = OrderType.LIMIT,
                                        price = price,
                                        initialAmount = amount
                                    )
                                )
                            }
                        }

                        context.source.sendMessage(Text.literal("§d[Debug] Market populated with 1000 orders per item!"))
                        1
                    }
                )
        )
    }
}