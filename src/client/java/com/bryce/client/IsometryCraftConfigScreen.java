package com.bryce.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Builds the configuration screen with Cloth Config; Mod Menu supplies the entry point.
 */
public final class IsometryCraftConfigScreen {
    private IsometryCraftConfigScreen() {
    }

    public static Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("IsometryCraft Options"))
                .setSavingRunnable(IsometryCraftClient::saveConfig);

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigCategory camera = builder.getOrCreateCategory(Component.literal("Camera"));

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Enable isometric view"), IsometryCraftClient.isIsometric)
                .setDefaultValue(false)
                .setSaveConsumer(enabled -> {
                    Minecraft client = Minecraft.getInstance();
                    IsometryCraftClient.setIsometric(enabled, client);
                })
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Show isometric HUD"), IsometryCraftClient.showHud)
                .setDefaultValue(true)
                .setSaveConsumer(value -> IsometryCraftClient.showHud = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Lock camera height"), IsometryCraftClient.isYLocked)
                .setDefaultValue(false)
                .setSaveConsumer(value -> IsometryCraftClient.isYLocked = value)
                .build());

        general.addEntry(entries.startIntField(
                        Component.literal("Locked camera height (blocks)"),
                        (int) Math.round(IsometryCraftClient.lockedYValue))
                .setDefaultValue(70)
                .setMin(-64)
                .setMax(320)
                .setSaveConsumer(value -> IsometryCraftClient.lockedYValue = value.doubleValue())
                .build());

        camera.addEntry(entries.startIntSlider(
                        Component.literal("Camera zoom"), Math.round(IsometryCraftClient.isometricSize), 1, 100)
                .setDefaultValue(15)
                .setSaveConsumer(value -> IsometryCraftClient.isometricSize = value.floatValue())
                .build());

        int yawStep = Math.round((Mth.wrapDegrees(IsometryCraftClient.targetYaw) + 360.0F) % 360.0F / 45.0F) % 8;
        camera.addEntry(entries.startIntSlider(
                        Component.literal("Camera yaw (45° steps)"), yawStep, 0, 7)
                .setDefaultValue(1)
                .setSaveConsumer(value -> IsometryCraftClient.targetYaw = value * 45.0F)
                .build());

        camera.addEntry(entries.startIntSlider(
                        Component.literal("Camera pitch"), IsometryCraftClient.targetPitchIndex, 0, 2)
                .setDefaultValue(1)
                .setSaveConsumer(value -> {
                    IsometryCraftClient.targetPitchIndex = Mth.clamp(value, 0, 2);
                    IsometryCraftClient.targetPitch =
                            IsometryCraftClient.PITCH_VALUES[IsometryCraftClient.targetPitchIndex];
                })
                .build());

        return builder.build();
    }
}
