#version 150

uniform sampler2D Sampler0; // surface texture

// Light arrays
#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform float reflectivity;  // reflection factor
uniform vec4 emissiveColor;  // planet's self-emission (rgb + intensity)
uniform float AtmDensity; // atmosphere density
uniform vec3 LocalSunriseColor; // sunrise color on observer planet to blend star color with

uniform vec3 TargetVector; // from observer to target planet

in vec2 texcoord;
in vec3 normalUniverseSpace;
in vec3 upUniverseSpace;

out vec4 fragColor;

void main() {
    vec3 baseSurfaceColor = texture(Sampler0, texcoord).rgb;

    vec3 totalReflectedLight = vec3(0.0);

    for (int i = 0; i < LightCount; i++) {
        float NdotL = max(0.0, dot(normalize(normalUniverseSpace), normalize(LightVectors[i])));
        float distance = length(LightVectors[i]);

        vec3 reflected = NdotL * reflectivity * baseSurfaceColor
        * LightColors[i].rgb * LightColors[i].a
        / (distance * distance);

        totalReflectedLight += reflected;
    }

    vec4 reflectedLight = vec4(totalReflectedLight, 1.0);

    //// this following code is an approximation to tint the star during sunrise and sunset
    float starUp = dot(upUniverseSpace, normalize(TargetVector)); // how much the star is above me
    float atmthicknessModifier = pow(1-max(0,starUp), 2)*0.9+0.1; // create a base atm thickness when above and larger thickness towards horizon
    atmthicknessModifier = atmthicknessModifier * AtmDensity / (1+AtmDensity); // scale it with the planets atm density / thickness modifier
    vec3 starSunriseColor = pow(LocalSunriseColor * emissiveColor.rgb, vec3(3)); // during sunrise the star should be tinted in the atmosphere sunrise color, but significantly amplified
    vec3 atmAdjustedEmissiveColor = mix(emissiveColor.rgb, starSunriseColor, atmthicknessModifier); // mix between base color and the tinted color based on the atmosphere thickness

    float starBrightnessMultiplier = 1 - atmthicknessModifier; // also make the star a bit darker because light is scattered away in the atmosphere
    float starBrightness = max(0.0, emissiveColor.a) * starBrightnessMultiplier;

    // Emission: surface color * emissive strength
    vec4 emittedLight = vec4(atmAdjustedEmissiveColor * baseSurfaceColor.rgb *  starBrightness, 1.0);

    fragColor = reflectedLight + emittedLight;
}
