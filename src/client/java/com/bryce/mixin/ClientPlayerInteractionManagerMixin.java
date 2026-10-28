package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Unique
    private void isometrycraft$lookAt(Vec3 targetPos) {
        if (!IsometryCraftClient.isIsometric || this.minecraft.player == null) {
            return;
        }

        LocalPlayer player = this.minecraft.player;
        double dx = targetPos.x - player.getX();
        double dy = targetPos.y - player.getEyeY();
        double dz = targetPos.z - player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        float pitch = Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(dy, horizontalDistance)));

        player.setYRot(yaw);
        player.setXRot(pitch);

        // The interaction packet can reach the server before the next regular player
        // movement update. Send the intended look first so server-side reach/LOS
        // validation uses the block or entity selected from the isometric camera.
        if (this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                    yaw, pitch, player.onGround(), player.horizontalCollision()));
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void isometrycraft$onUseBlock(
            LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        this.isometrycraft$lookAt(hitResult.getLocation());
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void isometrycraft$onInteractEntity(
            Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        this.isometrycraft$lookAt(hitResult.getLocation());
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void isometrycraft$onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        this.isometrycraft$lookAt(target.position());
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void isometrycraft$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        this.isometrycraft$lookAt(Vec3.atCenterOf(pos));
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void isometrycraft$onStartDestroyBlock(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        this.isometrycraft$lookAt(Vec3.atCenterOf(pos));
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void isometrycraft$onContinueDestroyBlock(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        this.isometrycraft$lookAt(Vec3.atCenterOf(pos));
    }
}
