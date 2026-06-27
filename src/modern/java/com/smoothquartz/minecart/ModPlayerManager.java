package com.smoothquartz.minecart;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModPlayerManager {

    private static final Set<UUID> HAS_MOD_PLAYERS = ConcurrentHashMap.newKeySet();

    private ModPlayerManager() {
    }

    public static void addPlayer(UUID playerUUID) {
        HAS_MOD_PLAYERS.add(playerUUID);
    }

    public static boolean hasMod(UUID playerUUID) {
        return HAS_MOD_PLAYERS.contains(playerUUID);
    }

    public static void onPlayerQuit(UUID playerUUID) {
        HAS_MOD_PLAYERS.remove(playerUUID);
    }

    public static void onServerShutdown() {
        HAS_MOD_PLAYERS.clear();
    }
}
