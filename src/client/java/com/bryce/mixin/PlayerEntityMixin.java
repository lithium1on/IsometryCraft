package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$blockInteractionRange(CallbackInfoReturnable<Double> cir) {
        Player player = (Player) (Object) this;
        if (IsometryCraftClient.isIsometric && player.getAbilities().instabuild) {
            cir.setReturnValue(128.0);
        }
    }

    @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$entityInteractionRange(CallbackInfoReturnable<Double> cir) {
        Player player = (Player) (Object) this;
        if (IsometryCraftClient.isIsometric && player.getAbilities().instabuild) {
            cir.setReturnValue(128.0);
        }
    }
}
