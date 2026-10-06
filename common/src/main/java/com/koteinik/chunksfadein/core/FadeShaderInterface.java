package com.koteinik.chunksfadein.core;

import com.koteinik.chunksfadein.compat.sodium.ext.*;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;

public class FadeShaderInterface {
	private GlUniformBlockExt uniformFadeDatas;
	private GlUniformFloat2vExt screenSize;
	private GlUniformMatrix3fExt worldInView;
	private GlUniformIntExt cullDist;
	private GlUniformIntExt skyVis;
	private GlUniformIntExt fogCull;

	public FadeShaderInterface(ShaderBindingContextExt context) {
		this.uniformFadeDatas = context.bindUniformBlock("cfi_ubo_ChunkFadeDatas");
		this.screenSize = context.bindUniformFloat2v("cfi_screenSize");
		this.worldInView = context.bindUniformMat3f("cfi_worldInView");
		this.cullDist = context.bindUniformInt("cfi_cullDist");
		this.skyVis = context.bindUniformInt("cfi_skyVis");
		this.fogCull = context.bindUniformInt("cfi_fogCull");
	}

	public void bindUniforms(GlMutableBufferExt fadeDataBuffer) {
		if (uniformFadeDatas != null)
			uniformFadeDatas.bindBuffer(fadeDataBuffer);

		if (screenSize != null)
			screenSize.set(Utils.mainTargetWidth(), Utils.mainTargetHeight());

		if (cullDist != null)
			cullDist.set((int) Minecraft.getInstance().gameRenderer.getRenderDistance());

		if (fogCull != null)
			fogCull.set(FogCulling.enabled() ? 1 : 0);

		if (skyVis != null)
			skyVis.set(SkyVisibility.asByte());

		if (worldInView != null)
			worldInView.set(new Matrix3f().rotation(Utils.cameraViewRot()));
	}
}
