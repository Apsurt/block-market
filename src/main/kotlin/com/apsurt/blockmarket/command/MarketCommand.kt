package com.apsurt.blockmarket.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object MarketCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("market")
                // Base command: /market (Opens the UI)
                .executes { context ->
                    val player = context.source.player ?: return@executes 0

                    player.sendMessage(Text.literal("§d[Opening Market UI...]"), false)
                    // TODO: Send packet to client to open the visual screen

                    1
                }

                // Subcommand: /market inbox (or /market claim)
                .then(literal("inbox")
                    .executes { context ->
                        val player = context.source.player ?: return@executes 0

                        // TODO: Implement the logic to sweep EscrowManager.getInbox(player.uuid)
                        // TODO: Add coins to wallet, give physical items to player inventory
                        player.sendMessage(Text.literal("§7[TODO: Claim items and coins from inbox]"), false)

                        1
                    }
                )
        )
    }
}