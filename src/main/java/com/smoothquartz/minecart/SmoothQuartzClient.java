package com.smoothquartz.minecart;

import com.smoothquartz.minecart.network.HandshakePayload;
import com.smoothquartz.minecart.network.SmoothQuartzNetworking;
import java.lang.reflect.Method;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmoothQuartzClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("SmoothQuartz-Client");
    private static final int HANDSHAKE_DELAY_TICKS = 20;

    private static boolean needsHandshake;
    private static int delayTicks;

    @Override
    public void onInitializeClient() {
        SmoothQuartzNetworking.initClientNetworking();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            needsHandshake = !ClientPlayNetworking.canSend(HandshakePayload.ID);
            delayTicks = HANDSHAKE_DELAY_TICKS;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            needsHandshake = false;
            delayTicks = 0;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!needsHandshake || client.player == null) {
                return;
            }

            if (delayTicks > 0) {
                delayTicks--;
                return;
            }

            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (networkHandler == null) {
                return;
            }

            sendHandshakeCommand(networkHandler);
            needsHandshake = false;
        });

        LOGGER.info("SmoothQuartz client initialized");
    }

    private void sendHandshakeCommand(ClientPlayNetworkHandler handler) {
        if (tryInvokeSendCommand(handler, "sqm handshake")) {
            return;
        }

        // Yarn mapping on this environment exposes sendChatCommand instead of sendCommand.
        handler.sendChatCommand("sqm handshake");
    }

    private boolean tryInvokeSendCommand(ClientPlayNetworkHandler handler, String command) {
        try {
            Method method = handler.getClass().getMethod("sendCommand", String.class);
            method.invoke(handler, command);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
