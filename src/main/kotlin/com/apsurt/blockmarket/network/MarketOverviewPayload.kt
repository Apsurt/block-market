package com.apsurt.blockmarket.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

// A simple DTO for the summary of a single item on the home page
data class AssetSummary(
    val assetId: String,
    val bestBid: Long, // We'll use 0L to represent "No bids" to keep the packet simple
    val bestAsk: Long, // We'll use 0L to represent "No asks"
    val volume24h: Long,
    val changePercent: Double
)

data class MarketOverviewPayload(
    val playerBalance: Long,
    val assets: List<AssetSummary>
) : CustomPayload {

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID = CustomPayload.Id<MarketOverviewPayload>(Identifier.of("blockmarket", "market_overview"))

        // Codec for a single AssetSummary
        private val SUMMARY_CODEC: PacketCodec<RegistryByteBuf, AssetSummary> = PacketCodec.tuple(
            PacketCodecs.STRING, AssetSummary::assetId,
            PacketCodecs.VAR_LONG, AssetSummary::bestBid,
            PacketCodecs.VAR_LONG, AssetSummary::bestAsk,
            PacketCodecs.VAR_LONG, AssetSummary::volume24h,
            PacketCodecs.DOUBLE, AssetSummary::changePercent,
            ::AssetSummary
        )

        // Codec for the entire payload
        val CODEC: PacketCodec<RegistryByteBuf, MarketOverviewPayload> = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, MarketOverviewPayload::playerBalance,
            SUMMARY_CODEC.collect(PacketCodecs.toList()), MarketOverviewPayload::assets,
            ::MarketOverviewPayload
        )
    }
}