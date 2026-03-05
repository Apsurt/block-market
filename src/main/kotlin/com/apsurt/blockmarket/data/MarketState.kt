package com.apsurt.blockmarket.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.datafixer.DataFixTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Uuids
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateType
import java.util.UUID

class MarketState(val balances: MutableMap<UUID, Long> = mutableMapOf()) : PersistentState() {

    companion object {
        val CODEC: Codec<MarketState> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.unboundedMap(Uuids.STRING_CODEC, Codec.LONG)
                    .optionalFieldOf("Balances", mutableMapOf())
                    .forGetter { state: MarketState -> state.balances }
            ).apply(instance) { balances -> MarketState(balances.toMutableMap()) }
        }

        val TYPE: PersistentStateType<MarketState> = PersistentStateType(
            "blockmarket_global",
            { MarketState() },
            CODEC,
            DataFixTypes.LEVEL
        )

        fun getServerState(server: MinecraftServer): MarketState {
            val persistentStateManager = server.overworld.persistentStateManager
            return persistentStateManager.getOrCreate(TYPE)
        }
    }
}