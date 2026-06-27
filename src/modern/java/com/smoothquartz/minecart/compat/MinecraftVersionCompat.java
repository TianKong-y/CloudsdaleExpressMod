package com.smoothquartz.minecart.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class MinecraftVersionCompat {

    private MinecraftVersionCompat() {
    }

    public static Level getWorld(Entity entity) {
        return entity.level();
    }
}
