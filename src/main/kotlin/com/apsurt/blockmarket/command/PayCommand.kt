package com.apsurt.blockmarket.command

import com.apsurt.blockmarket.BlockMarket
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object PayCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("pay")
                .then(argument("target", EntityArgumentType.player())
                    .then(argument("amount", LongArgumentType.longArg(1))
                        .executes { context ->
                            val sender = context.source.player ?: return@executes 0
                            val target = EntityArgumentType.getPlayer(context, "target")
                            val amount = LongArgumentType.getLong(context, "amount")

                            // 1. Try to take the money from the sender
                            val success = BlockMarket.orchestrator.walletManager.removeCoins(sender.uuid, amount)

                            if (success) {
                                // 2. If they had enough, give it to the target
                                BlockMarket.orchestrator.walletManager.addCoins(target.uuid, amount)

                                sender.sendMessage(Text.literal("§aPaid $amount coins to ${target.name.string}"), false)
                                target.sendMessage(Text.literal("§aReceived $amount coins from ${sender.name.string}"), false)
                            } else {
                                sender.sendMessage(Text.literal("§cInsufficient funds to pay $amount coins!"), false)
                            }
                            1
                        }
                    )
                )
        )
    }
}