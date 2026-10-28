package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean initialized;
    @Shadow private Level level;
    @Shadow private Entity entity;
    @Shadow private boolean detached;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "alignWithEntity", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$alignIsometricCamera(float partialTicks, CallbackInfo ci) {
        if (!IsometryCraftClient.isIsometric || this.level == null || this.entity == null) {
            return;
        }

        if (!Float.isFinite(partialTicks)) {
            return;
        }

        this.initialized = true;
        this.detached = true;

        float yaw = IsometryCraftClient.cameraYaw;
        float pitch = IsometryCraftClient.cameraPitch;
        this.setRotation(yaw, pitch);

        double x = Mth.lerp((double) partialTicks, this.entity.xo, this.entity.getX());
        double y = IsometryCraftClient.isYLocked
                ? IsometryCraftClient.lockedYValue
                : Mth.lerp((double) partialTicks, this.entity.yo, this.entity.getY())
                    + this.entity.getEyeHeight();
        double z = Mth.lerp((double) partialTicks, this.entity.zo, this.entity.getZ());

        double yawRadians = Math.toRadians(-yaw);
        double pitchRadians = Math.toRadians(pitch);
        double lookX = Math.sin(yawRadians) * Math.cos(pitchRadians);
        double lookY = -Math.sin(pitchRadians);
        double lookZ = Math.cos(yawRadians) * Math.cos(pitchRadians);
        double distance = 20.0;

        this.setPosition(
                x - lookX * distance,
                y - lookY * distance,
                z - lookZ * distance
        );
        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void isometrycraft$setOrthographicProjection(
            CameraRenderState cameraState, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!IsometryCraftClient.isIsometric) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getWidth();
        int height = client.getWindow().getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float size = IsometryCraftClient.isometricSize;
        float halfWidth = size * (float) width / (float) height;
        float halfHeight = size;
        Matrix4f projection = cameraState.projectionMatrix;
        projection.identity().ortho(
                -halfWidth, halfWidth,
                -halfHeight, halfHeight,
                -1000.0F, 1000.0F
        );
    }
}
