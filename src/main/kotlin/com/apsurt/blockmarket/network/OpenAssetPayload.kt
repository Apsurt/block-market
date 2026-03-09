package com.apsurt.blockmarket.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class OpenAssetPayload(val assetId: String) : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID = CustomPayload.Id<OpenAssetPayload>(Identifier.of("blockmarket", "open_asset"))
        val CODEC: PacketCodec<RegistryByteBuf, OpenAssetPayload> = PacketCodec.tuple(
            PacketCodecs.STRING, OpenAssetPayload::assetId,
            ::OpenAssetPayload
        )
    }
}