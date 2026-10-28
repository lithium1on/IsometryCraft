package com.bryce.mixin;

import com.bryce.client.IsometryCraftClient;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftPickMixin {
    @Inject(method = "pick", at = @At("HEAD"), cancellable = true)
    private void isometrycraft$pickFromCursor(float partialTicks, CallbackInfo ci) {
        if (!IsometryCraftClient.isIsometric) {
            return;
        }

        Minecraft client = (Minecraft) (Object) this;
        if (client.level == null || client.player == null) {
            return;
        }

        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity == null) {
            return;
        }

        Camera camera = client.gameRenderer.mainCamera();
        Window window = client.getWindow();
        int width = window.getScreenWidth();
        int height = window.getScreenHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        MouseHandler mouse = client.mouseHandler;
        double ndcX = (2.0 * mouse.getScaledXPos(window) / width) - 1.0;
        double ndcY = 1.0 - (2.0 * mouse.getScaledYPos(window) / height);

        float size = IsometryCraftClient.isometricSize;
        float aspect = (float) window.getWidth() / Math.max(1, window.getHeight());
        Quaternionf rotation = camera.rotation();
        Vector3f screenOffset = new Vector3f(
                (float) (ndcX * size * aspect),
                (float) (ndcY * size),
                0.0F
        ).rotate(rotation);
        Vec3 rayStart = camera.position().add(screenOffset.x, screenOffset.y, screenOffset.z);

        Vector3f directionVector = new Vector3f(0.0F, 0.0F, -1.0F).rotate(rotation);
        Vec3 direction = new Vec3(directionVector.x, directionVector.y, directionVector.z).normalize();
        Vec3 farEnd = rayStart.add(direction.scale(150.0));

        BlockHitResult blockHit = client.level.clip(new ClipContext(
                rayStart, farEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cameraEntity
        ));
        double maxDistance = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceTo(rayStart)
                : 150.0;
        Vec3 rayEnd = rayStart.add(direction.scale(maxDistance));
        AABB searchBox = new AABB(rayStart, rayEnd).inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                cameraEntity,
                rayStart,
                rayEnd,
                searchBox,
                entity -> !entity.isSpectator() && entity.isPickable(),
                maxDistance * maxDistance
        );

        client.hitResult = entityHit != null ? entityHit : blockHit;
        client.crosshairPickEntity = entityHit != null ? entityHit.getEntity() : null;
        ci.cancel();
    }
}
