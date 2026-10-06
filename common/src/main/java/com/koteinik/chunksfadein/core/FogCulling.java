package com.koteinik.chunksfadein.core;

import com.koteinik.chunksfadein.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FogCulling {
	private static Method options;
	private static Field performance;
	private static Field useFogOcclusion;
	private static boolean failed;

	public static boolean enabled() {
		if (failed)
			return true;

		try {
			if (options == null) {
				options = Class.forName("me.jellysquid.mods.sodium.client.SodiumClientMod").getMethod("options");

				Class<?> gameOptions = options.getReturnType();
				performance = gameOptions.getField("performance");
				useFogOcclusion = performance.getType().getField("useFogOcclusion");
			}

			Object performanceSettings = performance.get(options.invoke(null));
			return useFogOcclusion.getBoolean(performanceSettings);
		} catch (Throwable t) {
			Logger.warn("Could not read the fog occlusion option, assuming it is enabled: " + t);
			failed = true;
			return true;
		}
	}
}
