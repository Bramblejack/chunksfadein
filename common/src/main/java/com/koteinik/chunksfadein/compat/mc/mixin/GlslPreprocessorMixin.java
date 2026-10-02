package com.koteinik.chunksfadein.compat.mc.mixin;

import com.koteinik.chunksfadein.core.CoreShaderFog;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(GlslPreprocessor.class)
public class GlslPreprocessorMixin {
	@ModifyReturnValue(method = "process", at = @At("RETURN"))
	private List<String> cfi_patchFog(List<String> original) {
		return CoreShaderFog.patch(original);
	}
}
