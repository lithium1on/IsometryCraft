package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private double xpos;
    @Shadow private double ypos;

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$keepCursorFree(CallbackInfo ci) {
        if (IsometryCraftClient.isIsometric) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$middleButtonPan(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (!IsometryCraftClient.isIsometric
                || buttonInfo.button() != InputConstants.MOUSE_BUTTON_MIDDLE) {
            return;
        }

        if (action == InputConstants.PRESS) {
            IsometryCraftClient.isMiddleMouseDown = true;
            IsometryCraftClient.lastMouseX = this.xpos;
            IsometryCraftClient.lastMouseY = this.ypos;
        } else if (action == InputConstants.RELEASE) {
            IsometryCraftClient.isMiddleMouseDown = false;
        }
        ci.cancel();
    }

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$panWorld(long handle, double newX, double newY, CallbackInfo ci) {
        if (!IsometryCraftClient.isIsometric
                || !IsometryCraftClient.isMiddleMouseDown
                || this.minecraft.player == null) {
            return;
        }

        ci.cancel();
        double dx = newX - IsometryCraftClient.lastMouseX;
        double dy = newY - IsometryCraftClient.lastMouseY;
        double sensitivity = 0.05;

        double yawRad = Math.toRadians(IsometryCraftClient.cameraYaw);
        double pitchRad = Math.toRadians(IsometryCraftClient.cameraPitch);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        double rightZ = -sinYaw;
        double upX = sinYaw * sinPitch;
        double upZ = cosYaw * sinPitch;

        double moveX = (dx * cosYaw + dy * upX) * sensitivity;
        double moveY = dy * cosPitch * sensitivity;
        double moveZ = (dx * rightZ + dy * upZ) * sensitivity;

        double playerX = this.minecraft.player.getX() - moveX;
        double playerY = IsometryCraftClient.isYLocked
                ? IsometryCraftClient.lockedYValue - this.minecraft.player.getEyeHeight()
                : this.minecraft.player.getY() - moveY;
        double playerZ = this.minecraft.player.getZ() - moveZ;
        this.minecraft.player.setPos(playerX, playerY, playerZ);

        this.xpos = newX;
        this.ypos = newY;
        IsometryCraftClient.lastMouseX = newX;
        IsometryCraftClient.lastMouseY = newY;
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$zoomWithScroll(
            long window, double horizontal, double vertical, CallbackInfo ci) {
        if (IsometryCraftClient.isIsometric && this.minecraft.gui.screen() == null) {
            IsometryCraftClient.isometricSize = Mth.clamp(
                    IsometryCraftClient.isometricSize - (float) vertical * 1.5F,
                    1.0F, 100.0F
            );
            IsometryCraftClient.saveConfig();
            ci.cancel();
        }
    }
}
