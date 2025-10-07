#version 150

uniform sampler2D Sampler0; // surface texture

// Light arrays
#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform float TargetAtmDensity; // atmosphere density on observers planet
uniform vec3 TargetSkyColor;

in vec3 normalUniverseSpace;

out vec4 fragColor;

void main() {

    vec3 totalColor = vec3(0.0);

    float atmDotOffset = TargetAtmDensity / (1+TargetAtmDensity) * 0.1; // this makes slightly more than 180° of the planet bright when there it atmosphere

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        vec3 N = normalize(normalUniverseSpace);

        float NdotL = dot(N, L);
        NdotL = smoothstep(-atmDotOffset, 1.0, NdotL);

        float dist = length(LightVectors[i]);
        vec3 baseLight = LightColors[i].rgb * LightColors[i].a / (dist * dist);

        // Base diffuse reflection
        vec3 diffuse = NdotL * (vec3(1.0) * 0.8 + TargetSkyColor * 0.2)
        * TargetAtmDensity / (1.0 + TargetAtmDensity)
        * baseLight;

        // Rim scattering
        float rim = pow(1.0 - abs(dot(N, L)), 3.0);
        vec3 rimColor = TargetSkyColor * rim * 0.3 * TargetAtmDensity;

        // Combine
        totalColor += diffuse + rimColor;
    }

    // Add faint ambient scattering
    totalColor += TargetSkyColor * 0.03 * TargetAtmDensity;

    fragColor = vec4(totalColor, 1.0);

}
