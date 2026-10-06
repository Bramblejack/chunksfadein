package com.koteinik.chunksfadein.compat.sodium.ext;

import com.koteinik.chunksfadein.core.DataBuffer;

public interface RenderSectionExt {
	boolean hasRenderedBefore();

	void setRenderedBefore();

	void dhMarkRendered();

	/** True once fade and animation have both completed and their final values are in the buffer. */
	boolean isSettled();

	/** Recomputes the settled flag; call after the buffer has been written for this frame. */
	void updateSettled();

	long calculateAndGetDelta();

	float[] getAnimationOffset();

	float getFadeCoeff();

	boolean incrementFadeCoeff(long delta, int sectionIndex, DataBuffer buffer);

	boolean incrementAnimationOffset(long delta, int sectionIndex, DataBuffer buffer);
}
