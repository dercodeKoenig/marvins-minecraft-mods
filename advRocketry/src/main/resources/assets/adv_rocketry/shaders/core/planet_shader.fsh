#version 150

#moj_import "adv_rocketry:noise.glsl"

uniform sampler2D Sampler0; // surface texture

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform float BrightnessMultiplier;
uniform vec4 emissiveColor;      // planet’s self-emission (rgb + intensity)
uniform float LocalAtmDensity;        // observer planet atmosphere
uniform float TargetAtmDensity;  // target planet atmosphere (affects rim)
uniform vec3 LocalSunriseColor;  // tint for sunrise / sunset
uniform vec3 TargetSkyColor;       // target planets sky color
uniform vec3 TargetCloudColor;      // cloud color of the target planet
uniform float TargetCloudValue;       // how much clouds to render, 0 - 1
uniform vec3 TargetReflectiveTextureTintColor; // tint the texture for diffuse light
uniform float playerHeight;         // how high the player is to reduce atm tint for star
uniform float planetSkyHeight;      // how high is considered out of atmosphere
uniform int isLocalPlanet;           // if this is my planet, some special rendering applies
uniform vec3 localTerrainFogColor;
uniform float time; // time in seconds for noise offset

in vec2 texcoord;
in vec3 normalUniverseSpace;
in vec3 localUpUniverseSpace;
in vec3 viewDir;
in vec3 normalModelSpace;

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

    // calculate clouds first, the value is independent from lights
    // the noise is extremly expensive and crashes fps significantly on integrated graphics
    // i skip it on local planet where the fragment number is too high
    float cloudValue = 0;
    if(isLocalPlanet != 1){
        float amp = 1;
        float freq = 2;
        float noiseOffset = pow(TargetCloudValue, 0.5) * 2 - 1;
        cloudValue = noiseOffset;
        if (noiseOffset > - 0.9){
            vec3 warp = fbm_vec3(normalModelSpace, 1, time * 0.002);
            for (int i = 0; i < 5; i++) {
                float noiseVal = cnoise(warp * freq);
                cloudValue +=noiseVal * amp;
                amp *= 0.5;
                freq *= 2;
            }
        }
    }


    // for atmosphere
    // how much of the edge (horizon) we see
    float viewAngle = 1.0 - abs(dot(N, viewDir));
    // rim intensity (thicker with higher TargetAtmDensity)
    // the thing that glows on the side
    float rim = pow(viewAngle, 3);  // the more at the side the more atmosphere we will see

    vec3 atmLightMix =
    2 * rim * TargetSkyColor // the atm glow around the planet
    + baseSurfaceColor; // the light scatters through atm and hits terrain. not for cloudy atmosphere, but this is what the texture is for!


    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        float dist = length(LightVectors[i]);

        // the atm adds extra light after the normal falloff and uses the sky color/rim mix
        float atmLightFactor = clamp(dot(N, L) * 0.8 + 0.2, 0.0, 1.0);
        atmLightFactor = pow(atmLightFactor, 2); // with gamma correct the transition from black to less black is too aggressive
        vec3 atmLight = atmLightMix * atmLightFactor;

        // the reflected light without atmosphere consideration, just surface and n°l
        float NdotL = max(0,dot(N, L));
        NdotL = pow(NdotL, 2);
        vec3 surfaceLight = NdotL * baseSurfaceColor;

        // clouds color
        vec3 cloudLight = pow(clamp(cloudValue, 0, 1),2) * TargetCloudColor * atmLightFactor;

        // blend surface light and atm light and clouds
        vec3 finalLight = cloudLight + mix(surfaceLight, atmLight, TargetAtmDensity / (1+TargetAtmDensity));

        // final reflected light for this star
        vec3 reflected =
        finalLight
        * TargetReflectiveTextureTintColor
        * LightColors[i].rgb * LightColors[i].a
        / (dist * dist);


        totalReflectedLight += reflected * BrightnessMultiplier;
    }

    vec3 emitted = vec3(0,0,0);
    if(emissiveColor.a > 0) {
        // atmosphere modifies how the star appears
        float altitudeAtmThicknessMod = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
        float starUp = dot(localUpUniverseSpace, viewDir);
        float atmThicknessMod = LocalAtmDensity / (1.0 + LocalAtmDensity);
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
