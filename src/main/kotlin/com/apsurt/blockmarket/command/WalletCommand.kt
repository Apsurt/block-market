package com.apsurt.blockmarket.command

import com.apsurt.blockmarket.BlockMarket
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object WalletCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("wallet").executes { context ->
                val player = context.source.player ?: return@executes 0

                // We access the orchestrator from your main BlockMarket class
                val balance = BlockMarket.orchestrator.walletManager.getBalance(player.uuid)

                player.sendMessage(Text.literal("§6Your balance: §e$balance coins"), false)
                1
            }
        )
    }
}