package com.apsurt.blockmarket.command

import com.apsurt.blockmarket.BlockMarket
import com.apsurt.blockmarket.engine.Order
import com.apsurt.blockmarket.engine.OrderSide
import com.apsurt.blockmarket.engine.OrderType
import com.apsurt.blockmarket.network.MarketSyncPayload
import com.apsurt.blockmarket.network.OrderEntry

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType

import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking


object UserCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("bm")
                // 1. /bm balance
                .then(literal("balance")
                    .executes { context ->
                        val player = context.source.player ?: return@executes 0
                        val balance = BlockMarket.orchestrator.walletManager.getBalance(player.uuid)

                        player.sendMessage(Text.literal("§6Your balance: §e$balance coins"), false)
                        1
                    }
                )

                // 2. /bm transfer <target> <amount>
                .then(literal("transfer")
                    .then(argument("target", EntityArgumentType.player())
                        .then(argument("amount", LongArgumentType.longArg(1))
                            .executes { context ->
                                val sender = context.source.player ?: return@executes 0
                                val target = EntityArgumentType.getPlayer(context, "target")
                                val amount = LongArgumentType.getLong(context, "amount")

                                val success = BlockMarket.orchestrator.walletManager.removeCoins(sender.uuid, amount)

                                if (success) {
                                    BlockMarket.orchestrator.walletManager.addCoins(target.uuid, amount)
                                    sender.sendMessage(Text.literal("§aTransferred $amount coins to ${target.name.string}"), false)
                                    target.sendMessage(Text.literal("§aReceived $amount coins from ${sender.name.string}"), false)
                                } else {
                                    sender.sendMessage(Text.literal("§cInsufficient funds to transfer $amount coins!"), false)
                                }
                                1
                            }
                        )
                    )
                )

                // 3. /bm buy <item_id> <amount> (Market Buy)
                .then(literal("buy")
                    .then(argument("item", StringArgumentType.word())
                        .then(argument("amount", IntegerArgumentType.integer(1))
                            .executes { context ->
                                val player = context.source.player ?: return@executes 0
                                val itemId = StringArgumentType.getString(context, "item")
                                val amount = IntegerArgumentType.getInteger(context, "amount")

                                val order = Order(
                                    ownerId = player.uuid,
                                    assetId = itemId,
                                    side = OrderSide.BUY,
                                    type = OrderType.MARKET,
                                    price = 0L, // Price is resolved by the OrderBook for Market orders
                                    initialAmount = amount
                                )

                                try {
                                    val trades = BlockMarket.orchestrator.placeOrder(order)
                                    player.sendMessage(Text.literal("§aMarket Buy placed! Resulted in ${trades.size} trades."), false)
                                } catch (e: Exception) {
                                    player.sendMessage(Text.literal("§cFailed: ${e.message}"), false)
                                }
                                1
                            }
                        )
                    )
                )

                // 4. /bm sell <item_id> <amount> (Market Sell)
                .then(literal("sell")
                    .then(argument("item", StringArgumentType.word())
                        .then(argument("amount", IntegerArgumentType.integer(1))
                            .executes { context ->
                                val player = context.source.player ?: return@executes 0
                                val itemId = StringArgumentType.getString(context, "item")
                                val amount = IntegerArgumentType.getInteger(context, "amount")

                                // TODO: Verify player actually has the items in their Minecraft inventory!
                                // TODO: Remove the items from their physical inventory before placing the order.

                                val order = Order(
                                    ownerId = player.uuid,
                                    assetId = itemId,
                                    side = OrderSide.SELL,
                                    type = OrderType.MARKET,
                                    price = 0L,
                                    initialAmount = amount
                                )

                                try {
                                    val trades = BlockMarket.orchestrator.placeOrder(order)
                                    player.sendMessage(Text.literal("§aMarket Sell placed! Resulted in ${trades.size} trades."), false)
                                } catch (e: Exception) {
                                    player.sendMessage(Text.literal("§cFailed: ${e.message}"), false)
                                }
                                1
                            }
                        )
                    )
                )

                // 5. /bm bid <item_id> <amount> <price> (Limit Buy)
                .then(literal("bid")
                    .then(argument("item", StringArgumentType.word())
                        .then(argument("amount", IntegerArgumentType.integer(1))
                            .then(argument("price", LongArgumentType.longArg(1))
                                .executes { context ->
                                    val player = context.source.player ?: return@executes 0
                                    val itemId = StringArgumentType.getString(context, "item")
                                    val amount = IntegerArgumentType.getInteger(context, "amount")
                                    val price = LongArgumentType.getLong(context, "price")

                                    val order = Order(
                                        ownerId = player.uuid,
                                        assetId = itemId,
                                        side = OrderSide.BUY,
                                        type = OrderType.LIMIT,
                                        price = price,
                                        initialAmount = amount
                                    )

                                    try {
                                        val trades = BlockMarket.orchestrator.placeOrder(order)
                                        player.sendMessage(Text.literal("§aBid placed for $amount $itemId at $price coins each!"), false)
                                    } catch (e: Exception) {
                                        player.sendMessage(Text.literal("§cFailed: ${e.message}"), false)
                                    }
                                    1
                                }
                            )
                        )
                    )
                )

                // 6. /bm ask <item_id> <amount> <price> (Limit Sell)
                .then(literal("ask")
                    .then(argument("item", StringArgumentType.word())
                        .then(argument("amount", IntegerArgumentType.integer(1))
                            .then(argument("price", LongArgumentType.longArg(1))
                                .executes { context ->
                                    val player = context.source.player ?: return@executes 0
                                    val itemId = StringArgumentType.getString(context, "item")
                                    val amount = IntegerArgumentType.getInteger(context, "amount")
                                    val price = LongArgumentType.getLong(context, "price")

                                    // TODO: Verify & remove physical items from inventory before placing!

                                    val order = Order(
                                        ownerId = player.uuid,
                                        assetId = itemId,
                                        side = OrderSide.SELL,
                                        type = OrderType.LIMIT,
                                        price = price,
                                        initialAmount = amount
                                    )

                                    try {
                                        val trades = BlockMarket.orchestrator.placeOrder(order)
                                        player.sendMessage(Text.literal("§aAsk placed for $amount $itemId at $price coins each!"), false)
                                    } catch (e: Exception) {
                                        player.sendMessage(Text.literal("§cFailed: ${e.message}"), false)
                                    }
                                    1
                                }
                            )
                        )
                    )
                )

                // 7. /bm market (UI and Inbox placeholders)
                .then(literal("market")
                    .executes { context ->
                        val player = context.source.player ?: return@executes 0
                        // TODO: Remove hardcoded assetId and allow players to pass it as a command argument (e.g., /bm market minecraft:iron_ingot)
                        val assetId = "minecraft:diamond" // Hardcoded for now as requested

                        val balance = BlockMarket.orchestrator.walletManager.getBalance(player.uuid)

                        // TODO: Implement MarketOrchestrator.getTopOrders(assetId, limit = 50)
                        val payload = MarketSyncPayload(
                            assetId = assetId,
                            playerBalance = balance,
                            bids = listOf(OrderEntry(100L, 5), OrderEntry(95L, 10)), // Dummy data for UI testing
                            asks = listOf(OrderEntry(105L, 2), OrderEntry(110L, 8))  // Dummy data for UI testing
                        )

                        ServerPlayNetworking.send(player, payload)
                        1
                    }
                    .then(literal("inbox")
                        .executes { context ->
                            val player = context.source.player ?: return@executes 0
                            player.sendMessage(Text.literal("§7[TODO: Claim items and coins from inbox]"), false)
                            1
                        }
                    )
                )
        )
    }
}