package com.koteinik.chunksfadein.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FogLooksCompat {
	private static final String FOG_MANAGER = "net.shizotoaster.foglooksmodernnow.client.FogManager";

	private static boolean resolved;
	private static boolean available;

	private static Field densityManager;
	private static Method shouldRenderCaveFog;
	private static Method hasVanillaDarkness;
	private static Method getUndergroundFactor;
	private static Field darkness;
	private static Method interpolatedGet;

	public static float skyCoverVisibility(float partialTick) {
		if (!resolved)
			resolve();

		if (!available)
			return Float.NaN;

		try {
			Object manager = densityManager.get(null);
			if (manager == null)
				return Float.NaN;

			if (!(Boolean) shouldRenderCaveFog.invoke(null) || (Boolean) hasVanillaDarkness.invoke(null))
				return 1.0F;

			float underground = (Float) getUndergroundFactor.invoke(manager, partialTick);
			float darknessValue = (Float) interpolatedGet.invoke(darkness.get(manager), partialTick);

			float cover = 1.0F - (underground + (1.0F - underground) * darknessValue);
			cover *= cover;
			cover *= cover;

			return 1.0F - cover;
		} catch (Throwable t) {
			available = false;
			return Float.NaN;
		}
	}

	private static void resolve() {
		resolved = true;

		try {
			Class<?> manager = Class.forName(FOG_MANAGER);

			densityManager = manager.getField("densityManager");
			shouldRenderCaveFog = manager.getMethod("shouldRenderCaveFog");
			hasVanillaDarkness = manager.getMethod("hasVanillaDarkness");
			getUndergroundFactor = manager.getMethod("getUndergroundFactor", float.class);
			darkness = manager.getField("darkness");
			interpolatedGet = darkness.getType().getMethod("get", float.class);

			available = true;
		} catch (Throwable ignored) {
			available = false;
		}
	}
}
