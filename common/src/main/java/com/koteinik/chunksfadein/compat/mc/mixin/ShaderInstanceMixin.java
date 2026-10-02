package com.koteinik.chunksfadein.compat.mc.mixin;

import com.koteinik.chunksfadein.core.RenderPhase;
import com.koteinik.chunksfadein.core.SkyFBO;
import com.koteinik.chunksfadein.core.SkyVisibility;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {
	@Unique
	private static int cfi_boundFrame = -1;

	@Unique
	private int cfi_skyVisLocation = -2;
	@Unique
	private int cfi_skyLocation;
	@Unique
	private int cfi_skyCleanLocation;

	@Shadow
	public abstract int getId();

	@Inject(method = "apply", at = @At("TAIL"))
	private void cfi_applySky(CallbackInfo ci) {
		if (cfi_skyVisLocation == -2) {
			int program = getId();
			cfi_skyVisLocation = GL20.glGetUniformLocation(program, "cfi_skyVis");
			cfi_skyLocation = GL20.glGetUniformLocation(program, "cfi_sky");
			cfi_skyCleanLocation = GL20.glGetUniformLocation(program, "cfi_skyClean");
		}

		if (cfi_skyVisLocation < 0)
			return;

		int visibility = SkyVisibility.shaderValue();
		GL20.glUniform1i(cfi_skyVisLocation, visibility);
		if (visibility < 0)
			return;

		GL20.glUniform1i(cfi_skyLocation, 13);
		GL20.glUniform1i(cfi_skyCleanLocation, 12);

		if (cfi_boundFrame != RenderPhase.frame) {
			SkyFBO.bind(13);
			SkyFBO.bindClean(12);
			cfi_boundFrame = RenderPhase.frame;
		}
	}
}
