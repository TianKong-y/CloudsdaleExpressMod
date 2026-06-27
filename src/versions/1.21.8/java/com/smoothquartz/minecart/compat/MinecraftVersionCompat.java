package com.smoothquartz.minecart.compat;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class MinecraftVersionCompat {

    private MinecraftVersionCompat() {
    }

    public static World getWorld(Entity entity) {
        return entity.getWorld();
    }
}
