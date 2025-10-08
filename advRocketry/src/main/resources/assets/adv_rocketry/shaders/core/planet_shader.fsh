#version 150

uniform sampler2D Sampler0; // surface texture

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform vec4 emissiveColor;      // planet’s self-emission (rgb + intensity)
uniform float AtmDensity;        // observer planet atmosphere
uniform float TargetAtmDensity;  // target planet atmosphere (affects rim)
uniform vec3 LocalSunriseColor;  // tint for sunrise / sunset
uniform vec3 TargetVector;       // from observer to target planet
uniform vec3 TargetSkyColor;       // target planets sky color

in vec2 texcoord;
in vec3 normalUniverseSpace;
in vec3 upUniverseSpace;

out vec4 fragColor;
vec3 gamma_reverse(vec3 color){
    return pow(color, vec3(2.2));
}
void main() {
    vec3 baseSurfaceColor = texture(Sampler0, texcoord).rgb;
    vec3 totalReflectedLight = vec3(0.0);

    vec3 N = normalize(normalUniverseSpace);

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        float dist = length(LightVectors[i]);

        // the reflected light without atmosphere consideration is normal dot light
        float NdotL = max(0,dot(normalize(normalUniverseSpace), normalize(LightVectors[i])));


        // now consider atmosphere
        // how much of the edge (horizon) we see
        float viewAngle = 1.0 - clamp(dot(N, normalize(TargetVector)), 0.0, 1.0);
        // forward/back scattering relative to light direction
        float lightAngle = clamp(dot(N, L) * 0.7 + 0.3, 0.0, 1.0);

        // rim intensity (thicker with higher TargetAtmDensity)
        float rim = pow(viewAngle, 4)  // the more at the side the more atmosphere we will see
        * lightAngle  // more away from the sun = darker.
        * TargetAtmDensity; // less atmosphere = less light by atmosphere

        vec3 reflected =
        (NdotL * gamma_reverse(baseSurfaceColor) + rim * mix(gamma_reverse(baseSurfaceColor),gamma_reverse(TargetSkyColor),TargetAtmDensity/(1+TargetAtmDensity)))
        * gamma_reverse(LightColors[i].rgb) * LightColors[i].a
        / (dist * dist);


        totalReflectedLight += reflected;
    }

    //// --- existing emissive logic preserved ---

    float starUp = dot(upUniverseSpace, normalize(TargetVector));
    float atmThicknessMod = pow(1.0 - max(0.0, starUp), 2.0) * 0.9 + 0.1;
    atmThicknessMod *= AtmDensity / (1.0 + AtmDensity);

    vec3 starSunriseColor = pow(gamma_reverse(LocalSunriseColor) * gamma_reverse(emissiveColor.rgb), vec3(3.0));
    vec3 atmAdjustedEmissiveColor = mix(gamma_reverse(emissiveColor.rgb), starSunriseColor, atmThicknessMod);

    float starBrightnessMult = 1.0 - atmThicknessMod;
    float starBrightness = max(0.0, emissiveColor.a) * starBrightnessMult;

    vec3 emitted = atmAdjustedEmissiveColor * gamma_reverse(baseSurfaceColor) * starBrightness;

    fragColor = vec4(totalReflectedLight + emitted, 1.0);
}
