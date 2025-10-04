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

    // this is just simple approximation, real value would have to be computated by actual atm density towards the target vector
    float starUp = dot(upViewSpace, normalize(TargetVectorViewSpace));
    float horizonFactor = pow(1-max(0,starUp), 6);
    float sunsetModifier = horizonFactor * AtmDensity / (1+AtmDensity);
    float starDarkenMultiplier = 1 - sunsetModifier;
    vec3 heightAdjustedEmissiveColor = mix(emissiveColor.rgb, LocalSunriseColor, sunsetModifier);

    // Emission: surface color * emissive strength
    vec4 emittedLight = vec4(heightAdjustedEmissiveColor * baseSurfaceColor.rgb *  max(0.0, emissiveColor.a)*starDarkenMultiplier, 1.0);

    fragColor = reflectedLight + emittedLight;
}
