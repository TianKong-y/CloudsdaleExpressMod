package com.smoothquartz.minecart.network;

import com.smoothquartz.minecart.ModPlayerManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmoothQuartzNetworking {

    private static final Logger LOGGER = LoggerFactory.getLogger("SmoothQuartz-Network");
    private static boolean payloadTypesRegistered = false;

    private SmoothQuartzNetworking() {
    }

    public static synchronized void registerPayloadTypes() {
        if (payloadTypesRegistered) {
            return;
        }

        PayloadTypeRegistry.playC2S().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HandshakePayload.ID, HandshakePayload.CODEC);
        payloadTypesRegistered = true;
    }

    public static void initServerNetworking() {
        registerPayloadTypes();

        ServerPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, context) -> {
            if (!"HELLO".equals(payload.message())) {
                return;
            }

            ModPlayerManager.addPlayer(context.player().getUuid());
            ServerPlayNetworking.send(context.player(), new HandshakePayload("ACK"));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            ModPlayerManager.onPlayerQuit(handler.player.getUuid())
        );
    }

    public static void initClientNetworking() {
        registerPayloadTypes();

        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, context) -> {
            if ("ACK".equals(payload.message())) {
                LOGGER.info("Handshake ACK received from server");
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(HandshakePayload.ID)) {
                ClientPlayNetworking.send(new HandshakePayload("HELLO"));
            }
        });
    }
}
