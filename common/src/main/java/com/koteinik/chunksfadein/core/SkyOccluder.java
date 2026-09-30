package com.koteinik.chunksfadein.core;

import com.koteinik.chunksfadein.config.Config;
import com.koteinik.chunksfadein.hooks.CompatibilityHook;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class SkyOccluder {
	private static final float DEFAULT_START = -1.0F;
	private static final float DEFAULT_END = -8.0F;
	private static final float BELOW_MIN_Y_START = 15.0F;
	private static final float BELOW_MIN_Y_END = 10.0F;
	private static final float UNDERWATER_START = 40.0F;
	private static final float UNDERWATER_END = 20.0F;

	private static final float RADIUS = 64.0F;
	private static final int SEGMENTS = 48;
	private static final int RAMP_STEPS = 12;

	public static void render(PoseStack poseStack, float partialTick, Camera camera) {
		if (!Config.isModEnabled || !Config.skyOccluder)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null)
			return;

		if (level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL)
			return;

		if (CompatibilityHook.isIrisShaderPackInUse())
			return;

		float start;
		float end;
		if (minecraft.player.getEyePosition(partialTick).y < (double) level.getMinBuildHeight()) {
			start = BELOW_MIN_Y_START;
			end = BELOW_MIN_Y_END;
		} else if (camera.getFluidInCamera() == FogType.WATER) {
			start = UNDERWATER_START;
			end = UNDERWATER_END;
		} else {
			start = DEFAULT_START;
			end = DEFAULT_END;
		}

		float[] fogColor = RenderSystem.getShaderFogColor();

		draw(poseStack.last().pose(), fogColor[0], fogColor[1], fogColor[2], start, end);
	}

	private static void draw(Matrix4f pose, float red, float green, float blue, float start, float end) {
		float[] elevation = new float[RAMP_STEPS + 3];
		float[] alpha = new float[RAMP_STEPS + 3];
		elevation[0] = -90.0F;
		alpha[0] = 1.0F;
		elevation[1] = end;
		alpha[1] = 1.0F;
		for (int i = 1; i <= RAMP_STEPS; i++) {
			float angle = Mth.lerp(i / (float) RAMP_STEPS, end, start);
			elevation[i + 1] = angle;
			alpha[i + 1] = occlusionAlpha(angle, start, end);
		}

		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
			GlStateManager.SourceFactor.SRC_ALPHA,
			GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SourceFactor.ZERO,
			GlStateManager.DestFactor.ONE
		);
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);

		vertexCount = 0;
		for (int ring = 0; ring < elevation.length - 1; ring++) {
			if (elevation[ring + 1] <= elevation[ring])
				continue;

			for (int segment = 0; segment < SEGMENTS; segment++) {
				float azimuthA = segment * Mth.TWO_PI / SEGMENTS;
				float azimuthB = (segment + 1) * Mth.TWO_PI / SEGMENTS;
				vertex(elevation[ring], azimuthA, red, green, blue, alpha[ring]);
				vertex(elevation[ring], azimuthB, red, green, blue, alpha[ring]);
				vertex(elevation[ring + 1], azimuthB, red, green, blue, alpha[ring + 1]);
				vertex(elevation[ring + 1], azimuthA, red, green, blue, alpha[ring + 1]);
			}
		}
		flush(pose);

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	private static final int FLOATS_PER_VERTEX = 7;
	private static float[] vertices = new float[4 * SEGMENTS * (RAMP_STEPS + 2) * FLOATS_PER_VERTEX];
	private static int vertexCount;
	private static int program;
	private static int mvpLocation;
	private static int vao;
	private static int vbo;

	private static void vertex(float elevationDegrees, float azimuth, float red, float green, float blue, float alpha) {
		float elevation = elevationDegrees * Mth.DEG_TO_RAD;
		float horizontal = Mth.cos(elevation) * RADIUS;
		int i = vertexCount++ * FLOATS_PER_VERTEX;
		if (i + FLOATS_PER_VERTEX > vertices.length)
			vertices = java.util.Arrays.copyOf(vertices, vertices.length * 2);

		vertices[i] = horizontal * Mth.cos(azimuth);
		vertices[i + 1] = Mth.sin(elevation) * RADIUS;
		vertices[i + 2] = horizontal * Mth.sin(azimuth);
		vertices[i + 3] = red;
		vertices[i + 4] = green;
		vertices[i + 5] = blue;
		vertices[i + 6] = alpha;
	}

	private static void init() {
		int vs = compile(GL20.GL_VERTEX_SHADER,
			"#version 150\nin vec3 Position;\nin vec4 Color;\nuniform mat4 MVP;\nout vec4 vColor;\n"
				+ "void main() { gl_Position = MVP * vec4(Position, 1.0); vColor = Color; }\n");
		int fs = compile(GL20.GL_FRAGMENT_SHADER,
			"#version 150\nin vec4 vColor;\nout vec4 fragColor;\nvoid main() { fragColor = vColor; }\n");
		program = GL20.glCreateProgram();
		GL20.glAttachShader(program, vs);
		GL20.glAttachShader(program, fs);
		GL20.glBindAttribLocation(program, 0, "Position");
		GL20.glBindAttribLocation(program, 1, "Color");
		GL20.glLinkProgram(program);
		GL20.glDeleteShader(vs);
		GL20.glDeleteShader(fs);
		mvpLocation = GL20.glGetUniformLocation(program, "MVP");
		vao = GL30.glGenVertexArrays();
		vbo = GL15.glGenBuffers();
	}

	private static int compile(int type, String source) {
		int shader = GL20.glCreateShader(type);
		GL20.glShaderSource(shader, source);
		GL20.glCompileShader(shader);
		return shader;
	}

	private static void flush(Matrix4f pose) {
		if (vertexCount == 0)
			return;
		if (program == 0)
			init();

		int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		int previousBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

		int quads = vertexCount / 4;
		int triangleVertices = quads * 6;
		FloatBuffer data = MemoryUtil.memAllocFloat(triangleVertices * FLOATS_PER_VERTEX);
		try {
			for (int quad = 0; quad < quads; quad++) {
				int base = quad * 4;
				for (int corner : new int[]{0, 1, 2, 0, 2, 3})
					data.put(vertices, (base + corner) * FLOATS_PER_VERTEX, FLOATS_PER_VERTEX);
			}
			data.flip();

			Matrix4f mvp = new Matrix4f(RenderSystem.getProjectionMatrix()).mul(pose);
			FloatBuffer matrix = MemoryUtil.memAllocFloat(16);
			try {
				mvp.get(matrix);

				GL20.glUseProgram(program);
				GL20.glUniformMatrix4fv(mvpLocation, false, matrix);
			} finally {
				MemoryUtil.memFree((java.nio.Buffer) matrix);
			}

			GL30.glBindVertexArray(vao);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STREAM_DRAW);
			int stride = FLOATS_PER_VERTEX * Float.BYTES;
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
			GL20.glEnableVertexAttribArray(1);
			GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, triangleVertices);
		} finally {
			MemoryUtil.memFree((java.nio.Buffer) data);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
			GL30.glBindVertexArray(previousVao);
			GL20.glUseProgram(previousProgram);
		}
	}

	private static float occlusionAlpha(float angle, float start, float end) {
		float t = Mth.clamp((angle - start) / (end - start), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
