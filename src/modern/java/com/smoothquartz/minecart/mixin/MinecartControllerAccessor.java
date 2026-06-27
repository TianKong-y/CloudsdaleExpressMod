package com.smoothquartz.minecart.mixin;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecartBehavior.class)
public interface MinecartControllerAccessor {

    @Accessor("minecart")
    AbstractMinecart smoothquartz$getMinecart();
}
