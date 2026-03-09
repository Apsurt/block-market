package com.apsurt.blockmarket.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

// A simple DTO for a single line in the order book
data class OrderEntry(val price: Long, val amount: Int)

data class MarketSyncPayload(
    val assetId: String,
    val playerBalance: Long,
    val bids: List<OrderEntry>,
    val asks: List<OrderEntry>
) : CustomPayload {

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID = CustomPayload.Id<MarketSyncPayload>(Identifier.of("blockmarket", "market_sync"))

        // Codec for a single OrderEntry
        private val ENTRY_CODEC: PacketCodec<RegistryByteBuf, OrderEntry> = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, OrderEntry::price,
            PacketCodecs.VAR_INT, OrderEntry::amount,
            ::OrderEntry
        )

        // Codec for the entire payload
        val CODEC: PacketCodec<RegistryByteBuf, MarketSyncPayload> = PacketCodec.tuple(
            PacketCodecs.STRING, MarketSyncPayload::assetId,
            PacketCodecs.VAR_LONG, MarketSyncPayload::playerBalance,
            ENTRY_CODEC.collect(PacketCodecs.toList()), MarketSyncPayload::bids,
            ENTRY_CODEC.collect(PacketCodecs.toList()), MarketSyncPayload::asks,
            ::MarketSyncPayload
        )
    }
}