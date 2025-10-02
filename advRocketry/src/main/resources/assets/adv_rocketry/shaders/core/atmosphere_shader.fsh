#version 150

uniform sampler2D planetTexture; // rendered planets / stars

uniform vec4 Color;
uniform vec4 FogColor;
uniform int screenWidth;
uniform int screenHeight;

in vec3 rotatedNormal;

out vec4 fragColor;

vec4 linear_fog(vec4 inColor, float vertexHeight, vec4 fogColor) {
    float fogValue = 1-vertexHeight;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

void main() {
    //fragColor = linear_fog(Color, clamp(vertexHeight, 0,1), FogColor);

    vec2 uv = gl_FragCoord.xy / vec2(screenWidth, screenHeight);
    vec4 planetframe = texture(planetTexture, uv);

    vec4 color = Color + planetframe;
    fragColor = color / (1+color);
}
