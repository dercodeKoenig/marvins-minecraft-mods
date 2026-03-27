#version 150

uniform float playerHeight;         // how high the player is
uniform float planetSkyHeight;      // how high is considered out of atmosphere
uniform float LocalAtmDensity;      // atm density of current planet

#moj_import "adv_rocketry:atm_filter.glsl"

in vec4 vColor;
in vec3 localUpUniverseSpace;
in vec3 viewDir;

out vec4 fragColor;

void main() {
    vec3 U = normalize(localUpUniverseSpace);
    vec3 V = normalize(viewDir);

    float relativeHeight = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
    float atmThickness = getAtmThickness(relativeHeight, U, V, LocalAtmDensity);
    // star background is very dark, our eyes are not color sensitive so do not use atm tint
    vec4 color = vColor * exp(-atmThickness * 4);

    fragColor = color;
}
