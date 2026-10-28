package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
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

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$keepCursorFree(CallbackInfo ci) {
        if (IsometryCraftClient.isIsometric) {
            ci.cancel();
        }
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
