package com.apsurt.blockmarket.command

import net.minecraft.command.permission.*
import net.minecraft.server.command.ServerCommandSource

object MarketPermissions {
    fun isAdmin(source: ServerCommandSource): Boolean {
        return source.permissions.hasPermission(Permission.Level(PermissionLevel.ADMINS))
    }
}