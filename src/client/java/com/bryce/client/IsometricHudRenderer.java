package com.bryce.client;

import com.bryce.IsometryCraft;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class IsometricHudRenderer {
    private IsometricHudRenderer() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                IsometryCraft.id("isometric_hud"),
                IsometricHudRenderer::extract
        );
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!IsometryCraftClient.isIsometric || !IsometryCraftClient.showHud) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        // Avoid drawing over other screens such as the inventory or pause menu.
        if (client.player == null || client.gui.screen() != null) {
            return;
        }

        int x = 10;
        int y = 10;
        int line = 12;
        graphics.text(client.font, Component.literal("IsometryCraft"), x, y, 0xFFFFC857);
        y += line;
        graphics.text(client.font,
                Component.literal(String.format("Yaw: %.1f° (target %.1f°)",
                        IsometryCraftClient.cameraYaw, IsometryCraftClient.targetYaw)),
                x, y, 0xFFE0E0E0);
        y += line;
        graphics.text(client.font,
                Component.literal(String.format("Pitch: %.1f°", IsometryCraftClient.cameraPitch)),
                x, y, 0xFFE0E0E0);
        y += line;
        graphics.text(client.font,
                Component.literal(String.format("Zoom: %.1f", IsometryCraftClient.isometricSize)),
                x, y, 0xFFE0E0E0);
        y += line;
        graphics.text(client.font,
                Component.literal(IsometryCraftClient.isYLocked
                        ? String.format("Y-lock: %.1f", IsometryCraftClient.lockedYValue)
                        : "Y-lock: off"),
                x, y, 0xFFE0E0E0);
    }
}
