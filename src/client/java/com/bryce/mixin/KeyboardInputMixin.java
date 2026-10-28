package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Shadow protected Vec2 moveVector;

    @Inject(method = "tick", at = @At("TAIL"))
    private void isometrycraft$cameraRelativeMovement(CallbackInfo ci) {
        if (!IsometryCraftClient.isIsometric || this.moveVector == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        float forward = this.moveVector.y;
        float sideways = this.moveVector.x;
        if (forward == 0.0F && sideways == 0.0F) {
            return;
        }

        float inputAngle = (float) Math.toDegrees(Math.atan2(-sideways, forward));
        float yaw = Mth.wrapDegrees(IsometryCraftClient.cameraYaw + inputAngle);

        client.player.setYRot(yaw);
        client.player.setXRot(0.0F);
        client.player.setYHeadRot(yaw);

        float magnitude = (float) Math.hypot(forward, sideways);
        this.moveVector = new Vec2(0.0F, magnitude);
    }
}
