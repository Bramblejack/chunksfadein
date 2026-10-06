package com.koteinik.chunksfadein.compat.sodium.mixin;

import com.koteinik.chunksfadein.MathUtils;
import com.koteinik.chunksfadein.compat.dh.LodMaskTexture;
import com.koteinik.chunksfadein.compat.sodium.ext.RenderSectionExt;
import com.koteinik.chunksfadein.config.Config;
import com.koteinik.chunksfadein.core.DataBuffer;
import com.koteinik.chunksfadein.core.Fader;
import com.koteinik.chunksfadein.core.Utils;
import com.koteinik.chunksfadein.hooks.CompatibilityHook;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSection.class, remap = false)
public class RenderSectionMixin implements RenderSectionExt {
	@Shadow
	@Final
	private int chunkX;

	@Shadow
	@Final
	private int chunkY;

	@Shadow
	@Final
	private int chunkZ;

	private Fader fader;
	private boolean completedFade = false;
	private boolean completedAnimation = false;
	private boolean settled = false;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void modifyInit(RenderRegion region, int chunkX, int chunkY, int chunkZ, CallbackInfo ci) {
		fader = new Fader(chunkX, chunkZ);
	}

	@Override
	public boolean hasRenderedBefore() {
		return fader.hasRenderedBefore();
	}

	@Override
	public void setRenderedBefore() {
		fader.setRenderedBefore();
	}

	@Override
	public void dhMarkRendered() {
		if ((completedFade || !Config.isFadeEnabled) && (completedAnimation || !Config.isAnimationEnabled))
			LodMaskTexture.markRendered(chunkX, chunkY, chunkZ);
	}

	@Override
	public boolean isSettled() {
		return settled;
	}

	@Override
	public void updateSettled() {
		settled = completedFade && completedAnimation;
	}

	@Override
	public boolean incrementFadeCoeff(long delta, int sectionIndex, DataBuffer buffer) {
		if (completedFade)
			return false;

		float fadeCoeff = fader.incrementFadeCoeff(delta, isNearPlayer());
		buffer.put(sectionIndex, 3, fadeCoeff);

		completedFade |= fadeCoeff == 1f;
		return true;
	}

	@Override
	public boolean incrementAnimationOffset(long delta, int sectionIndex, DataBuffer buffer) {
		if (completedAnimation)
			return false;

		if (!Config.animateWithDH && CompatibilityHook.isDHRenderingEnabled())
			return completedAnimation = true;

		float[] offset = fader.incrementAnimationOffset(delta, isNearPlayer());
		for (int i = 0; i < 3; i++)
			buffer.put(sectionIndex, i, offset[i]);

		completedAnimation |= offset[0] == 0f && offset[1] == 0f && offset[2] == 0f;
		return true;
	}

	@Override
	public long calculateAndGetDelta() {
		return fader.calculateAndGetDelta();
	}

	@Override
	public float[] getAnimationOffset() {
		return fader.getAnimationOffset();
	}

	@Override
	public float getFadeCoeff() {
		return fader.getFadeCoeff();
	}

	private boolean isNearPlayer() {
		net.minecraft.world.phys.Vec3 cam = Utils.cameraPosition();

		final int camChunkX = SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(cam.x));
		final int camChunkZ = SectionPos.blockToSectionCoord(net.minecraft.util.Mth.floor(cam.z));

		return MathUtils.chunkInRange(chunkX, chunkZ, camChunkX, camChunkZ, 1);
	}
}
