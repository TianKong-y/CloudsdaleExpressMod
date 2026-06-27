package com.smoothquartz.minecart.mixin;

import com.smoothquartz.minecart.ModPlayerManager;
import com.smoothquartz.minecart.compat.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({OldMinecartBehavior.class, NewMinecartBehavior.class})
public abstract class MinecartPhysicsMixin {

    @Unique
    private static final double HIGH_SPEED_MAX = 1.75D;

    @Unique
    private static final double NORMAL_SPEED_MAX = 0.4D;

    @Unique
    private static final double SOFT_DECEL_PER_TICK = 0.055D;

    @Unique
    private static final double EPSILON = 1.0E-6D;

    @Unique
    private double smoothquartz$softCap = NORMAL_SPEED_MAX;

    @Unique
    private boolean smoothquartz$softDecelActive = false;

    @Unique
    private net.minecraft.world.entity.vehicle.minecart.AbstractMinecart smoothquartz$getMinecart() {
        return ((MinecartControllerAccessor) this).smoothquartz$getMinecart();
    }

    @Unique
    private boolean isOnSmoothQuartzRail() {
        var minecart = smoothquartz$getMinecart();
        BlockPos railPos = minecart.blockPosition();
        var world = MinecraftVersionCompat.getWorld(minecart);
        BlockState railBlock = world.getBlockState(railPos);
        BlockState below = world.getBlockState(railPos.below());
        return railBlock.is(Blocks.SMOOTH_QUARTZ) || below.is(Blocks.SMOOTH_QUARTZ);
    }

    @Unique
    private boolean hasHighSpeedPermission() {
        Entity passenger = smoothquartz$getMinecart().getFirstPassenger();
        if (!(passenger instanceof Player player)) {
            return true;
        }

        // Keep server-side tracking support, but do not hard-block local/client behavior.
        var world = MinecraftVersionCompat.getWorld(smoothquartz$getMinecart());
        if (world.isClientSide()) {
            return true;
        }
        return ModPlayerManager.hasMod(player.getUUID()) || world.getServer() == null;
    }

    @Unique
    private boolean shouldEnableHighSpeed() {
        return isOnSmoothQuartzRail() && hasHighSpeedPermission();
    }

    @Unique
    private double horizontalSpeed(Vec3 velocity) {
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetMaxSpeed(ServerLevel world, CallbackInfoReturnable<Double> cir) {
        if (shouldEnableHighSpeed()) {
            smoothquartz$softDecelActive = false;
            smoothquartz$softCap = HIGH_SPEED_MAX;
            cir.setReturnValue(HIGH_SPEED_MAX);
            return;
        }

        if (smoothquartz$softDecelActive && smoothquartz$softCap > NORMAL_SPEED_MAX + EPSILON) {
            cir.setReturnValue(Math.min(cir.getReturnValue(), smoothquartz$softCap));
            return;
        }

        smoothquartz$softDecelActive = false;
        smoothquartz$softCap = NORMAL_SPEED_MAX;
        cir.setReturnValue(Math.min(cir.getReturnValue(), NORMAL_SPEED_MAX));
    }

    @Inject(method = "moveAlongTrack", at = @At("TAIL"))
    private void onMoveOnRail(ServerLevel world, CallbackInfo ci) {
        var minecart = smoothquartz$getMinecart();
        Vec3 velocity = minecart.getDeltaMovement();
        double horizontal = horizontalSpeed(velocity);
        if (horizontal <= 0.0D) {
            return;
        }

        if (shouldEnableHighSpeed()) {
            smoothquartz$softDecelActive = false;
            smoothquartz$softCap = HIGH_SPEED_MAX;
            return;
        }

        if (horizontal <= NORMAL_SPEED_MAX + EPSILON) {
            smoothquartz$softDecelActive = false;
            smoothquartz$softCap = NORMAL_SPEED_MAX;
            return;
        }

        if (!smoothquartz$softDecelActive) {
            smoothquartz$softDecelActive = true;
            smoothquartz$softCap = Math.min(HIGH_SPEED_MAX, horizontal);
        }

        smoothquartz$softCap = Math.max(NORMAL_SPEED_MAX, smoothquartz$softCap - SOFT_DECEL_PER_TICK);

        if (horizontal > smoothquartz$softCap) {
            double factor = smoothquartz$softCap / horizontal;
            minecart.setDeltaMovement(velocity.x * factor, velocity.y, velocity.z * factor);
        }

        if (smoothquartz$softCap <= NORMAL_SPEED_MAX + EPSILON) {
            smoothquartz$softDecelActive = false;
        }
    }
}
