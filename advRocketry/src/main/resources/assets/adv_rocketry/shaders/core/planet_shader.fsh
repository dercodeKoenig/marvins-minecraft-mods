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
uniform float playerHeight;         // how high the player is to reduce atm tint for star
uniform float planetSkyHeight;

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


    // atmosphere modifies how the star appears
    float altitudeAtmThicknessMod = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
    float starUp = dot(upUniverseSpace, normalize(TargetVector));
    float atmThicknessMod = AtmDensity / (1.0 + AtmDensity);
    atmThicknessMod *= pow(1.0 - max(0.0, starUp), 2.0) * 0.5 + 0.4;
    atmThicknessMod *= altitudeAtmThicknessMod;

    // more atm should make the star less bright bc light scatters away
    float starBrightness = max(0.0, emissiveColor.a) * (1 - atmThicknessMod) ;

    vec3 starColorRGBLinear= gamma_reverse(emissiveColor.rgb);
    vec3 sunriseLinear = gamma_reverse(LocalSunriseColor);

    // i tint the star slightly in the sunrise color because this is the light that scatters away less
    vec3 sunRiseTintColor = sunriseLinear;

    // tint based on atm thickness
    vec3 atmAdjustedEmissiveColor = mix(starColorRGBLinear, sunRiseTintColor, atmThicknessMod);

    // also include the texture for emissive, well it will probably not matter because of bloom but i think this is correct so
    vec3 emitted = atmAdjustedEmissiveColor * gamma_reverse(baseSurfaceColor) * starBrightness;

    fragColor = vec4(totalReflectedLight + emitted, 1.0);
}
