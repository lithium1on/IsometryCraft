package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$disableViewBobbing(
            CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (IsometryCraftClient.isIsometric) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$disableHurtTilt(
            CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (IsometryCraftClient.isIsometric) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$hideFirstPersonHand(
            CameraRenderState cameraState, PlayerRenderState playerState,
            GpuTextureView depthTextureView, CallbackInfo ci) {
        if (IsometryCraftClient.isIsometric) {
            ci.cancel();
        }
    }
}
