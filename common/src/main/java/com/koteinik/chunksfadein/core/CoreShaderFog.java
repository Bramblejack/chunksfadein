package com.koteinik.chunksfadein.core;

import java.util.Collections;
import java.util.List;

public class CoreShaderFog {
	private static final String FOG_SIGNATURE = "vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor)";

	private static final String HELPER = String.join("\n",
		"uniform sampler2D cfi_sky;",
		"uniform sampler2D cfi_skyClean;",
		"uniform int cfi_skyVis = -1;",
		"vec4 cfi_linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {",
		"if (cfi_skyVis >= 0 && vertexDistance > fogStart) {",
		"vec2 uv = gl_FragCoord.xy / vec2(textureSize(cfi_sky, 0));",
		"fogColor.rgb = mix(textureLod(cfi_sky, uv, 0.0).rgb, textureLod(cfi_skyClean, uv, 0.0).rgb, float(cfi_skyVis) / 255.0);",
		"}",
		"return linear_fog(inColor, vertexDistance, fogStart, fogEnd, fogColor);",
		"}",
		""
	);

	public static List<String> patch(List<String> source) {
		String joined = String.join("", source);

		int main = joined.indexOf("void main()");
		if (main < 0 || !joined.contains(FOG_SIGNATURE) || joined.contains("cfi_linear_fog"))
			return source;

		String head = joined.substring(0, main);
		String body = joined.substring(main);
		if (!body.contains("linear_fog("))
			return source;

		return Collections.singletonList(head + HELPER + body.replace("linear_fog(", "cfi_linear_fog("));
	}
}
