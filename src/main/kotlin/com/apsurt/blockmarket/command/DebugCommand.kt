package com.apsurt.blockmarket.command

import com.apsurt.blockmarket.BlockMarket
import com.apsurt.blockmarket.engine.Order
import com.apsurt.blockmarket.engine.OrderSide
import com.apsurt.blockmarket.engine.OrderType
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.registry.Registries
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
                .requires(MarketPermissions::isAdmin)

                .then(literal("populate")
                    .executes { context ->
                        // Fetch EVERY item in the game except air
                        val items = Registries.ITEM.ids
                            .filter { it.path != "air" }
                            .map { it.toString() }

                        val botUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")

                        // Give the bot unlimited resources to back the orders
                        BlockMarket.orchestrator.walletManager.addCoins(botUuid, 1_000_000_000L)

                        var totalOrders = 0

                        items.forEach { itemId ->
                            // Create Asks (Sellers) - Reduced to 10 per item to prevent server freeze
                            repeat(10) {
                                val price = Random.nextLong(200, 1000)
                                val amount = Random.nextInt(1, 10)

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
                                totalOrders++
                            }

                            // Create Bids (Buyers) - Reduced to 10 per item
                            repeat(10) {
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
                                totalOrders++
                            }
                        }

                        context.source.sendMessage(Text.literal("§d[Debug] Market populated with $totalOrders active orders across ${items.size} items!"))
                        1
                    }
                )
        )
    }
}