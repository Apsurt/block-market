package com.apsurt.blockmarket.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class PlaceOrderPayload(
    val assetId: String,
    val isBuy: Boolean,
    val isMarket: Boolean,
    val price: Long,
    val shares: Int
) : CustomPayload {

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID = CustomPayload.Id<PlaceOrderPayload>(Identifier.of("blockmarket", "place_order"))

        // PacketCodec.tuple supports up to 6 parameters perfectly for this!
        val CODEC: PacketCodec<RegistryByteBuf, PlaceOrderPayload> = PacketCodec.tuple(
            PacketCodecs.STRING, PlaceOrderPayload::assetId,
            PacketCodecs.BOOLEAN, PlaceOrderPayload::isBuy,
            PacketCodecs.BOOLEAN, PlaceOrderPayload::isMarket,
            PacketCodecs.VAR_LONG, PlaceOrderPayload::price,
            PacketCodecs.VAR_INT, PlaceOrderPayload::shares,
            ::PlaceOrderPayload
        )
    }
}