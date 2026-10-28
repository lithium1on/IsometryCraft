package com.bryce.client;

import com.bryce.IsometryCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class IsometryCraftClient implements ClientModInitializer {
    public static boolean isIsometric = false;
    public static float targetYaw = 45.0F;
    public static float cameraYaw = 45.0F;
    public static float prevCameraYaw = 45.0F;
    public static final float[] PITCH_VALUES = {
            0.0F,
            (float) Math.toDegrees(Math.atan(1.0 / Math.sqrt(2.0))),
            90.0F
    };
    public static int targetPitchIndex = 1;
    public static float targetPitch = PITCH_VALUES[1];
    public static float cameraPitch = PITCH_VALUES[1];
    public static float prevCameraPitch = PITCH_VALUES[1];
    public static float isometricSize = 15.0F;
    public static boolean isMiddleMouseDown = false;
    public static double lastMouseX = 0.0;
    public static double lastMouseY = 0.0;
    public static boolean isYLocked = false;
    public static double lockedYValue = 70.0;
    public static boolean showHud = true;

    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(IsometryCraft.id("general"));
    private static KeyMapping toggleKey;
    private static KeyMapping lockYKey;
    private static KeyMapping rotateLeftKey;
    private static KeyMapping rotateRightKey;
    private static KeyMapping pitchUpKey;
    private static KeyMapping pitchDownKey;
    private static KeyMapping openConfigKey;
    private static CloudStatus previousCloudStatus;
    private static final File CONFIG_FILE = Path.of(
            FabricLoader.getInstance().getConfigDir().toString(),
            "isometrycraft_client.json"
    ).toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean firstTick = true;

    @Override
    public void onInitializeClient() {
        loadConfig();

        toggleKey = registerKey("key.isometrycraft.toggle_iso", InputConstants.KEY_I);
        lockYKey = registerKey("key.isometrycraft.lock_y", InputConstants.KEY_Y);
        rotateLeftKey = registerKey("key.isometrycraft.rotate_left", InputConstants.KEY_LEFT);
        rotateRightKey = registerKey("key.isometrycraft.rotate_right", InputConstants.KEY_RIGHT);
        pitchUpKey = registerKey("key.isometrycraft.pitch_up", InputConstants.KEY_UP);
        pitchDownKey = registerKey("key.isometrycraft.pitch_down", InputConstants.KEY_DOWN);
        openConfigKey = registerKey("key.isometrycraft.open_config", InputConstants.KEY_O);

        IsometricHudRenderer.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (firstTick) {
                firstTick = false;
                if (isIsometric) {
                    enterIsometricMode(client);
                }
            }

            if (isIsometric) {
                prevCameraYaw = cameraYaw;
                float deltaYaw = Mth.wrapDegrees(targetYaw - cameraYaw);
                cameraYaw = Math.abs(deltaYaw) > 0.01F ? cameraYaw + deltaYaw * 0.25F : targetYaw;

                prevCameraPitch = cameraPitch;
                float deltaPitch = targetPitch - cameraPitch;
                cameraPitch = Math.abs(deltaPitch) > 0.01F ? cameraPitch + deltaPitch * 0.25F : targetPitch;
            }

            while (openConfigKey.consumeClick()) {
                client.gui.setScreen(IsometryCraftConfigScreen.createScreen(client.gui.screen()));
            }

            while (toggleKey.consumeClick()) {
                setIsometric(!isIsometric, client);
                if (client.player != null) {
                    client.gui.hud.setOverlayMessage(
                            Component.literal("View Mode: " + (isIsometric ? "Isometric" : "Normal")), true);
                }
                saveConfig();
            }

            while (lockYKey.consumeClick()) {
                if (isIsometric && client.player != null) {
                    isYLocked = !isYLocked;
                    if (isYLocked) {
                        lockedYValue = client.player.getY() + client.player.getEyeHeight();
                    }
                    String status = isYLocked
                            ? String.format("Camera Y-Lock: Enabled (%.2f)", lockedYValue)
                            : "Camera Y-Lock: Disabled";
                    client.gui.hud.setOverlayMessage(Component.literal(status), true);
                    saveConfig();
                }
            }

            while (rotateLeftKey.consumeClick()) {
                if (isIsometric) {
                    targetYaw = Mth.wrapDegrees(targetYaw + 45.0F);
                    saveConfig();
                }
            }
            while (rotateRightKey.consumeClick()) {
                if (isIsometric) {
                    targetYaw = Mth.wrapDegrees(targetYaw - 45.0F);
                    saveConfig();
                }
            }
            while (pitchUpKey.consumeClick()) {
                if (isIsometric && targetPitchIndex < PITCH_VALUES.length - 1) {
                    targetPitchIndex++;
                    targetPitch = PITCH_VALUES[targetPitchIndex];
                    saveConfig();
                }
            }
            while (pitchDownKey.consumeClick()) {
                if (isIsometric && targetPitchIndex > 0) {
                    targetPitchIndex--;
                    targetPitch = PITCH_VALUES[targetPitchIndex];
                    saveConfig();
                }
            }
        });
    }

    private static KeyMapping registerKey(String translationKey, int keyCode) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.Type.KEYBOARD, keyCode, KEY_CATEGORY));
    }

    public static void setIsometric(boolean enabled, Minecraft client) {
        if (isIsometric == enabled) {
            return;
        }

        if (enabled) {
            isIsometric = true;
            enterIsometricMode(client);
        } else {
            isIsometric = false;
            isYLocked = false;
            isMiddleMouseDown = false;
            client.mouseHandler.grabMouse();
            if (previousCloudStatus != null) {
                client.options.cloudStatus().set(previousCloudStatus);
                previousCloudStatus = null;
            }
        }
        saveConfig();
    }

    private static void enterIsometricMode(Minecraft client) {
        client.mouseHandler.releaseMouse();
        previousCloudStatus = client.options.cloudStatus().get();
        client.options.cloudStatus().set(CloudStatus.OFF);
    }

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            saveConfig();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            IsometryCraftConfig config = GSON.fromJson(reader, IsometryCraftConfig.class);
            if (config != null) {
                isIsometric = config.isIsometric;
                targetYaw = config.targetYaw;
                cameraYaw = config.cameraYaw;
                prevCameraYaw = config.prevCameraYaw;
                targetPitchIndex = Mth.clamp((int) config.targetPitchIndex, 0, PITCH_VALUES.length - 1);
                targetPitch = PITCH_VALUES[targetPitchIndex];
                cameraPitch = config.cameraPitch;
                prevCameraPitch = config.prevCameraPitch;
                isometricSize = Mth.clamp(config.isometricSize, 1.0F, 100.0F);
                isYLocked = config.isYLocked;
                lockedYValue = config.lockedYValue;
                showHud = config.showHud;
                try {
                    previousCloudStatus = config.previousCloudMode != null
                            ? CloudStatus.valueOf(config.previousCloudMode)
                            : CloudStatus.FANCY;
                } catch (IllegalArgumentException e) {
                    previousCloudStatus = CloudStatus.FANCY;
                }
            }
        } catch (Exception e) {
            IsometryCraft.LOGGER.error("Could not load IsometryCraft config", e);
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            IsometryCraftConfig config = new IsometryCraftConfig();
            config.isIsometric = isIsometric;
            config.targetYaw = targetYaw;
            config.cameraYaw = cameraYaw;
            config.prevCameraYaw = prevCameraYaw;
            config.targetPitchIndex = targetPitchIndex;
            config.targetPitch = targetPitch;
            config.cameraPitch = cameraPitch;
            config.prevCameraPitch = prevCameraPitch;
            config.isometricSize = isometricSize;
            config.isYLocked = isYLocked;
            config.lockedYValue = lockedYValue;
            config.showHud = showHud;
            config.previousCloudMode = previousCloudStatus != null ? previousCloudStatus.name() : "FANCY";
            GSON.toJson(config, writer);
        } catch (Exception e) {
            IsometryCraft.LOGGER.error("Could not save IsometryCraft config", e);
        }
    }
}
