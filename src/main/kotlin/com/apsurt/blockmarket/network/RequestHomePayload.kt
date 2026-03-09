package com.apsurt.blockmarket.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

object RequestHomePayload : CustomPayload {
    val ID = CustomPayload.Id<RequestHomePayload>(Identifier.of("blockmarket", "request_home"))

    // PacketCodecs.unit now points to the single 'this' instance
    val CODEC: PacketCodec<RegistryByteBuf, RequestHomePayload> = PacketCodec.unit(this)

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}