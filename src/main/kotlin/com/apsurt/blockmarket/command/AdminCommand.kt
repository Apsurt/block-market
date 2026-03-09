package com.apsurt.blockmarket.command

import com.apsurt.blockmarket.BlockMarket
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object AdminCommand {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("bmadmin")
                .requires(MarketPermissions::isAdmin)

                // 1. MINT: Adds coins to the target's existing balance
                .then(literal("mint")
                    .then(argument("target", EntityArgumentType.player())
                        .then(argument("amount", LongArgumentType.longArg(1))
                            .executes { context ->
                                val target = EntityArgumentType.getPlayer(context, "target")
                                val amount = LongArgumentType.getLong(context, "amount")

                                BlockMarket.orchestrator.walletManager.addCoins(target.uuid, amount)
                                BlockMarket.logger.info("[AUDIT] Admin ${context.source.name} minted $amount coins for ${target.name.string} (${target.uuid})")
                                context.source.sendMessage(Text.literal("§bMinted $amount coins for ${target.name.string}"))
                                1
                            }
                        )
                    )
                )

                // 2. BURN: Safely removes coins (fails if they don't have enough)
                .then(literal("burn")
                    .then(argument("target", EntityArgumentType.player())
                        .then(argument("amount", LongArgumentType.longArg(1))
                            .executes { context ->
                                val target = EntityArgumentType.getPlayer(context, "target")
                                val amount = LongArgumentType.getLong(context, "amount")

                                val success = BlockMarket.orchestrator.walletManager.removeCoins(target.uuid, amount)

                                if (success) {
                                    BlockMarket.logger.info("[AUDIT] Admin ${context.source.name} burned $amount coins from ${target.name.string} (${target.uuid})")
                                    context.source.sendMessage(Text.literal("§cBurned $amount coins from ${target.name.string}"))
                                } else {
                                    context.source.sendMessage(Text.literal("§4Failed! ${target.name.string} does not have enough coins to burn $amount."))
                                }
                                1
                            }
                        )
                    )
                )

                // 3. SETBALANCE: Forcefully overrides their current balance
                .then(literal("setbalance")
                    .then(argument("target", EntityArgumentType.player())
                        .then(argument("amount", LongArgumentType.longArg(0))
                            .executes { context ->
                                val target = EntityArgumentType.getPlayer(context, "target")
                                val amount = LongArgumentType.getLong(context, "amount")

                                BlockMarket.orchestrator.walletManager.setBalance(target.uuid, amount)
                                BlockMarket.logger.info("[AUDIT] Admin ${context.source.name} set balance of ${target.name.string} (${target.uuid}) to $amount")
                                context.source.sendMessage(Text.literal("§eSet ${target.name.string}'s balance to $amount coins"))
                                1
                            }
                        )
                    )
                )
        )
    }
}