#version 150

uniform sampler2D Sampler0; // surface texture

// Light arrays
#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform int LightCount;

uniform float reflectivity;  // reflection factor
uniform vec4 emissiveColor;  // planet's self-emission (rgb + intensity)

in vec3 LightVectors_ViewSpace[MAX_LIGHTS];
in vec2 texcoord;
in vec3 normalViewSpace;

out vec4 fragColor;

void main() {
    vec3 baseSurfaceColor = texture(Sampler0, texcoord).rgb;

    vec3 totalReflectedLight = vec3(0.0);

    for (int i = 0; i < LightCount; i++) {
        float NdotL = max(0.0, dot(normalize(normalViewSpace), normalize(LightVectors_ViewSpace[i])));
        float distance = length(LightVectors_ViewSpace[i]);

        vec3 reflected = NdotL * reflectivity * baseSurfaceColor
        * LightColors[i].rgb * LightColors[i].a
        / (distance * distance);

        totalReflectedLight += reflected;
    }

    vec4 reflectedLight = vec4(totalReflectedLight, 1.0);

    // Emission: surface color * emissive strength
    vec4 emittedLight = vec4(emissiveColor.rgb * baseSurfaceColor.rgb * max(0.0, emissiveColor.a), 1.0);

    fragColor = reflectedLight + emittedLight;
}
