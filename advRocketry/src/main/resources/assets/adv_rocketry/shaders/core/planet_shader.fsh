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
uniform float planetSkyHeight;      // how high is considered out of atmosphere

uniform int isLocalPlanet;           // if this is my planet, some special rendering applies
uniform vec3 localTerrainFogColor;

in vec2 texcoord;
in vec3 normalUniverseSpace;
in vec3 localUpUniverseSpace;
in vec3 relativeFragPosUniverse;

out vec4 fragColor;

void main() {

    vec3 baseSurfaceColor = texture(Sampler0, texcoord).rgb;
    baseSurfaceColor = pow(baseSurfaceColor, vec3(2.2)); // gamma reverse

    if(isLocalPlanet == 1){
        float mixvalue = clamp((playerHeight-350) / planetSkyHeight * 5, 0, 1);
        baseSurfaceColor = mix(localTerrainFogColor, baseSurfaceColor, mixvalue);
    }

    vec3 totalReflectedLight = vec3(0.0);

    vec3 N = normalize(normalUniverseSpace);

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        float dist = length(LightVectors[i]);


        // the atm adds extra light after the normal falloff
        float atmLightFactor = clamp(dot(N, L) * 0.6 + 0.4, 0.0, 1.0);
        atmLightFactor = pow(atmLightFactor, 2);


        // how much of the edge (horizon) we see
        vec3 viewDir = normalize(relativeFragPosUniverse);
        float viewAngle = 1.0 - abs(dot(N, viewDir));
        // rim intensity (thicker with higher TargetAtmDensity)
        // the thing that glows on the side
        float rim = pow(viewAngle, 2);  // the more at the side the more atmosphere we will see

        vec3 atmLight =
         2 * rim * atmLightFactor * TargetSkyColor // the atm glow around the planet
         + atmLightFactor * baseSurfaceColor; // the light scatters through atm and hits terrain. not for cloudy atmosphere, but this is what the texture is for!
        ;


        // the reflected light without atmosphere consideration is normal dot light
        float NdotL = pow(max(0,dot(N, L)),2);
        vec3 reflectedLight = NdotL * baseSurfaceColor;

        vec3 reflected =
        (
            mix(reflectedLight, atmLight, TargetAtmDensity / (1+TargetAtmDensity))
        )
        * LightColors[i].rgb * LightColors[i].a
        / (dist * dist);


        totalReflectedLight += reflected;
    }

    vec3 emitted = vec3(0,0,0);
    if(emissiveColor.a > 0) {
        // atmosphere modifies how the star appears
        float altitudeAtmThicknessMod = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
        float starUp = dot(localUpUniverseSpace, normalize(TargetVector));
        float atmThicknessMod = AtmDensity / (1.0 + AtmDensity);
        atmThicknessMod *= pow(1.0 - max(0.0, starUp), 2.0) * 0.5 + 0.4;
        atmThicknessMod *= altitudeAtmThicknessMod;

        // more atm should make the star less bright bc light scatters away
        float starBrightness = max(0.0, emissiveColor.a) * (1 - atmThicknessMod);


        // i tint the star slightly in the sunrise color because this is the light that scatters away less
        vec3 sunRiseTintColor = LocalSunriseColor;

        // tint based on atm thickness
        vec3 atmAdjustedEmissiveColor = mix(emissiveColor.rgb, sunRiseTintColor, atmThicknessMod);

        // also include the texture for emissive, well it will probably not matter because of bloom but i think this is correct so
        emitted = atmAdjustedEmissiveColor * baseSurfaceColor * starBrightness;
    }

    fragColor = vec4(totalReflectedLight + emitted, 1.0);
}
