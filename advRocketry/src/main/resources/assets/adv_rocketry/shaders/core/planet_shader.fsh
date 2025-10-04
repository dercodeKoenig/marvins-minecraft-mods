#version 150

uniform sampler2D Sampler0; // surface texture

// Light arrays
#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform int LightCount;

uniform float reflectivity;  // reflection factor
uniform vec4 emissiveColor;  // planet's self-emission (rgb + intensity)
uniform float AtmDensity; // atmosphere density
uniform vec3 LocalSunriseColor; // sunrise color on observer planet to blend star color with

in vec3 LightVectors_ViewSpace[MAX_LIGHTS];
in vec2 texcoord;
in vec3 normalViewSpace;
in vec3 upViewSpace;
in vec3 TargetVectorViewSpace; // from observer to target planet

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

    //// this following code is an approximation to tint the star during sunrise and sunset
    float starUp = dot(upViewSpace, normalize(TargetVectorViewSpace)); // how much the star is above me
    float atmthicknessModifier = pow(1-max(0,starUp), 3)*0.9+0.1; // create a base atm thickness when above and larger thickness towards horizon
    atmthicknessModifier = atmthicknessModifier * AtmDensity / (1+AtmDensity); // scale it with the planets atm density / thickness modifier
    vec3 starSunriseColor = pow(LocalSunriseColor * emissiveColor.rgb, vec3(3)); // during sunrise the star should be tinted in the atmosphere sunrise color, but significantly amplified
    vec3 atmAdjustedEmissiveColor = mix(emissiveColor.rgb, starSunriseColor, atmthicknessModifier); // mix between base color and the tinted color based on the atmosphere thickness

    float starBrightnessMultiplier = 1 - atmthicknessModifier; // also make the star a bit darker because light is scattered away in the atmosphere
    float starBrightness = max(0.0, emissiveColor.a) * starBrightnessMultiplier;

    // Emission: surface color * emissive strength
    vec4 emittedLight = vec4(atmAdjustedEmissiveColor * baseSurfaceColor.rgb *  starBrightness, 1.0);

    fragColor = reflectedLight + emittedLight;
}
