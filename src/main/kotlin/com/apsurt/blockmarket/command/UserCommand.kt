package com.apsurt.blockmarket.command

import com.apsurt.blockmarket.BlockMarket
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

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

                // 3. /bm market
                .then(literal("market")
                    // Executing just /bm market
                    .executes { context ->
                        val player = context.source.player ?: return@executes 0

                        player.sendMessage(Text.literal("§d[Opening Market UI...]"), false)
                        // TODO: Send packet to client to open the visual screen

                        1
                    }
                    // Executing /bm market inbox
                    .then(literal("inbox")
                        .executes { context ->
                            val player = context.source.player ?: return@executes 0

                            // TODO: Implement the logic to sweep EscrowManager.getInbox(player.uuid)
                            player.sendMessage(Text.literal("§7[TODO: Claim items and coins from inbox]"), false)

                            1
                        }
                    )
                )
        )
    }
}