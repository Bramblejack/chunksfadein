package com.koteinik.chunksfadein.core;

import com.koteinik.chunksfadein.config.Config;
import com.koteinik.chunksfadein.hooks.CompatibilityHook;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

public class SkyVisibility {
	private static final float SMOOTHING_SECONDS = 0.4F;

	private static float value = 1.0F;
	private static long lastUpdate;

	public static void update(Camera camera, float partialTick) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;

		float target = FogLooksCompat.skyCoverVisibility(partialTick);
		if (Float.isNaN(target)) {
			BlockPos pos = camera.getBlockPosition();
			int light = Math.max(level.getBrightness(LightLayer.SKY, pos), level.getBrightness(LightLayer.SKY, pos.above()));
			target = light > 0 ? 1.0F : 0.0F;
		}

		long now = Util.getMillis();
		float delta = Math.min((now - lastUpdate) / 1000.0F, 0.25F);
		lastUpdate = now;

		value += (target - value) * (1.0F - (float) Math.exp(-delta / SMOOTHING_SECONDS));
	}

	public static int shaderValue() {
		if (!Config.isModEnabled || !Config.isFadeEnabled || Config.fogOverrideMode != FogOverrideMode.CYLINDRICAL)
			return -1;

		if (!RenderPhase.renderingLevel || RenderPhase.fogSetups < 2 || CompatibilityHook.isIrisShaderPackInUse())
			return -1;

		return asByte();
	}

	public static int asByte() {
		return Math.round(Mth.clamp(value, 0.0F, 1.0F) * 255.0F);
	}
}
