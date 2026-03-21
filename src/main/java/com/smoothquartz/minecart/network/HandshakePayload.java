package com.smoothquartz.minecart.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HandshakePayload(String message) implements CustomPayload {

    public static final Identifier CHANNEL = Identifier.of("smoothquartz", "handshake");
    public static final CustomPayload.Id<HandshakePayload> ID = new CustomPayload.Id<>(CHANNEL);
    public static final PacketCodec<RegistryByteBuf, HandshakePayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING,
        HandshakePayload::message,
        HandshakePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
