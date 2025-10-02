#version 150

uniform sampler2D planetTexture; // rendered planets / stars

uniform vec4 Color;
uniform vec4 FogColor;
uniform int screenWidth;
uniform int screenHeight;

in vec3 normalViewSpace;
in vec3 upViewSpace;
out vec4 fragColor;

vec4 linear_fog(vec4 inColor, float vertexHeight, vec4 fogColor) {
    float fogValue = 1-vertexHeight;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

void main() {

    vec2 uv = gl_FragCoord.xy / vec2(screenWidth, screenHeight);
    vec4 planetframe = texture(planetTexture, uv);

    vec4 color = Color + planetframe;

    vec4 toneMap = color / (1+color);
    vec4 gammaCorrected = pow(toneMap, vec4(1.0/2.2));

    float distanceFactor = max(0,dot(normalize(upViewSpace), -normalize(normalViewSpace)));
    distanceFactor = pow(distanceFactor, 0.5);
    vec4 fogBlended = gammaCorrected * (distanceFactor) + FogColor * (1-distanceFactor);

    fragColor = fogBlended;
}
