package com.smoothquartz.minecart.mixin;

import com.smoothquartz.minecart.ModPlayerManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.DefaultMinecartController;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DefaultMinecartController.class, ExperimentalMinecartController.class})
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
    private net.minecraft.entity.vehicle.AbstractMinecartEntity smoothquartz$getMinecart() {
        return ((MinecartControllerAccessor) this).smoothquartz$getMinecart();
    }

    @Unique
    private boolean isOnSmoothQuartzRail() {
        var minecart = smoothquartz$getMinecart();
        BlockPos railPos = minecart.getBlockPos();
        BlockState railBlock = minecart.getWorld().getBlockState(railPos);
        BlockState below = minecart.getWorld().getBlockState(railPos.down());
        return railBlock.isOf(Blocks.SMOOTH_QUARTZ) || below.isOf(Blocks.SMOOTH_QUARTZ);
    }

    @Unique
    private boolean hasHighSpeedPermission() {
        Entity passenger = smoothquartz$getMinecart().getFirstPassenger();
        if (!(passenger instanceof PlayerEntity player)) {
            return true;
        }

        // Keep server-side tracking support, but do not hard-block local/client behavior.
        if (smoothquartz$getMinecart().getWorld().isClient()) {
            return true;
        }
        return ModPlayerManager.hasMod(player.getUuid()) || smoothquartz$getMinecart().getWorld().getServer() == null;
    }

    @Unique
    private boolean shouldEnableHighSpeed() {
        return isOnSmoothQuartzRail() && hasHighSpeedPermission();
    }

    @Unique
    private double horizontalSpeed(Vec3d velocity) {
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetMaxSpeed(ServerWorld world, CallbackInfoReturnable<Double> cir) {
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

    @Inject(method = "moveOnRail", at = @At("TAIL"))
    private void onMoveOnRail(ServerWorld world, CallbackInfo ci) {
        var minecart = smoothquartz$getMinecart();
        Vec3d velocity = minecart.getVelocity();
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
            minecart.setVelocity(velocity.x * factor, velocity.y, velocity.z * factor);
        }

        if (smoothquartz$softCap <= NORMAL_SPEED_MAX + EPSILON) {
            smoothquartz$softDecelActive = false;
        }
    }
}
