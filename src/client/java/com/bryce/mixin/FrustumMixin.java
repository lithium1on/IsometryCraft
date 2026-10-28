package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$disableFrustumCull(AABB box, CallbackInfoReturnable<Boolean> cir) {
        if (IsometryCraftClient.isIsometric) {
            cir.setReturnValue(true);
        }
    }
}
