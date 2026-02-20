#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// NEW: Add the uniform here. vec2 allows you to shift both horizontally (U) and vertically (V).
uniform vec2 UVOffset;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // NEW: Apply the offset to the texture coordinates
    vec2 offsetCoord = texCoord0 + UVOffset;
    vec4 color = texture(Sampler0, offsetCoord) * vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}