#version 150

#moj_import "adv_rocketry:noise.glsl"
#moj_import "adv_rocketry:utils.glsl"
#moj_import "adv_rocketry:atmFilter.glsl"

uniform sampler2D Sampler0; // surface texture
uniform sampler2D AtmTexture; // atmosphere frame

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform float BrightnessMultiplier;
uniform vec4 emissiveColor;      // planet’s self-emission (rgb + intensity)
uniform vec3 TargetTextureTintColor; // tint the texture
uniform float TargetAtmDensity;  // target planet atmosphere (affects rim)
uniform vec3 TargetSunriseColor;  // tint for clouds
uniform vec3 TargetSkyColor;       // target planets sky color
uniform vec3 TargetCloudColor;      // cloud color of the target planet
uniform float TargetCloudValue;       // how much clouds to render, 0 - 1
uniform int CloudSampleSteps;
uniform int CloudWarp;


// for local planet rendering / atmosphere shading
uniform float playerHeight;         // how high the player is
uniform float planetSkyHeight;      // how high is considered out of atmosphere
uniform int isLocalPlanet;           // if this is my planet
uniform vec3 localTerrainFogColor;
uniform float LocalAtmDensity;
uniform vec3 LocalSunriseColor;  // tint for sunrise / sunset

uniform float time; // time in seconds for noise offset

in vec2 texcoord;
in vec3 normalUniverseSpace;
in vec3 viewDir;
in vec3 normalModelSpace;
in vec3 localUpUniverseSpace;

out vec4 fragColor;

void main() {

    vec3 U = normalize(localUpUniverseSpace);
    vec3 V = normalize(viewDir);
    vec3 N = normalize(normalUniverseSpace);
    vec3 N_model = normalize(normalModelSpace);

    vec3 baseSurfaceColor = texture(Sampler0, texcoord).rgb;
    baseSurfaceColor = pow(baseSurfaceColor, vec3(2.2)); // gamma reverse
    baseSurfaceColor *= TargetTextureTintColor;

    // if this is my current planet below, tint the surface in fog color for smooth transition
    if(isLocalPlanet == 1){
        float mixvalue = clamp((playerHeight-350) / planetSkyHeight * 5, 0, 1);
        baseSurfaceColor = mix(localTerrainFogColor, baseSurfaceColor, mixvalue);
    }

    // find a normalization for the target atmosphere density to keep the math stable
    float normalizedTargetAtmDensity = TargetAtmDensity / (1 + TargetAtmDensity);

    // normalize hdr colors to 0-1
    vec3 scaledSunsetTint = maxNormC(TargetSunriseColor);

    // calculate clouds first, the value is independent from lights
    // the noise is extremly expensive and crashes fps significantly on integrated graphics
    // i skip it on local planet where the fragment number is too high
    float cloudValue = 0;
    if(isLocalPlanet != 1 && CloudSampleSteps > 0){
        float amp = 0.8;
        float freq = 2;
        float noiseOffset = pow(max(0, TargetCloudValue), 0.5) * 2 - 1;
        cloudValue = noiseOffset;
        if (noiseOffset > - 0.9){
            vec3 warp;
            if (CloudWarp == 1){
                warp = fbm_vec3(N_model, 1, time * 0.002);
            }else{
                warp = N_model + vec3(time * 0.002);
            }

            for (int i = 0; i < CloudSampleSteps; i++) {
                float noiseVal = cnoise(warp * freq);
                cloudValue += noiseVal * amp;
                amp *= 0.5;
                freq *= 2;
            }
        }
    }
    float extraCloud = max(0,cloudValue - 1); // this avoids flat clouds by giving them some more color
    cloudValue = clamp(cloudValue, 0, 1);
    cloudValue = pow(cloudValue, 3);

    // for atmosphere
    // how much of the edge (horizon) we see
    float viewAngle = 1.0 - abs(dot(N, V));
    // rim intensity (thicker with higher TargetAtmDensity)
    // the thing that glows on the side
    // the more at the side the more atmosphere we will see
    float rim = pow(viewAngle, 3);
    // the atm glow around the planet to be scaled with starlight and added to the final color
    vec3 atmGlow = 2 * rim * TargetSkyColor * normalizedTargetAtmDensity;


    // distant planets like saturn have maybe 1% of earth sunlight
    // eyes would adapt slightly but i do not control the entire render pipeline
    // so i will accumulate the entire star brightness and modify the output brightness with it
    // (only modulate reflected light)
    float totalBrightness = 0;

    vec3 totalReflectedLight = vec3(0.0);

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        float dist = length(LightVectors[i]);

        float brightness = LightColors[i].a / (dist * dist);
        totalBrightness += brightness;

        float NdotL = dot(N, L);

        // the atm adds extra light after the normal falloff and uses the sky color/rim mix
        float atmLightFactor = max(0, NdotL * 0.8 + 0.2);
        atmLightFactor = pow(atmLightFactor, 2); // with gamma correct the transition from black to less black is too aggressive

        // the reflected light without atmosphere consideration, just surface and n°l
        float surfaceLightFactor = max(0, NdotL);
        surfaceLightFactor = pow(surfaceLightFactor, 2);

        // mix surface light
        // when no atmosphere use ndotl,
        // when atmosphere, extend the ndotl because atm scatters light past the 0 line
        float NdotLmix = mix(surfaceLightFactor, atmLightFactor, normalizedTargetAtmDensity);
        vec3 surfaceLight = baseSurfaceColor * NdotLmix;

        // clouds color are higher up and use the extended dot
        // the following logic aims to keep clouds nice and bright for the day side
        // but have a smooth falloff and sunset tint toward the dark side
        float cloudLightFactor = atmLightFactor;  // re-use this for clouds too
        float cloudHorizonFactor = pow(max(0, 1 - cloudLightFactor), 20); // how much clouds are at horizon for sunrise tint
        vec3 cloudLight = TargetCloudColor * (1+extraCloud) * cloudLightFactor *
                            (
                            (1-cloudHorizonFactor) + // normal cloud light if not at horizon
                            cloudHorizonFactor * scaledSunsetTint // tinted clouds at horizon
                            );

        // blend surface light and atm light and clouds
        // clouds cover surface, so when we add clouds, we need to remove surface color
        vec3 surfaceCloudMix = surfaceLight * (1-cloudValue) + cloudLight * cloudValue;

        // atmosphere glow
        vec3 atmLight = atmGlow * atmLightFactor;

        // add atmosphere glow to the surface mix
        vec3 finalLight = surfaceCloudMix + atmLight;

        // final reflected light computed using this star
        vec3 reflected =
        finalLight * LightColors[i].rgb * brightness;

        totalReflectedLight += reflected;
    }

    if(totalBrightness < 1){
        // restore some brightness to keep planets visible, or saturn would have only 1% of earth light
        totalReflectedLight *= 1 / pow(totalBrightness, 0.5);
    }

    vec3 emitted = vec3(0,0,0);
    if(emissiveColor.a > 0) {
        emitted = emissiveColor.rgb * baseSurfaceColor * emissiveColor.a;
    }

    // Extract the "Overbright" texture parts as self-emission
    // Anything that exceeds 1.0 after textureBrightness is applied becomes a light source and glows independent of star light
    // (TODO: this could use an extra emissive texture in future)
    vec3 textureEmission = max(0,(1 - cloudValue)) * max(vec3(0.0), baseSurfaceColor - vec3(1));

    // some ambient air glow
    vec3 airGlow1 = TargetSkyColor * normalizedTargetAtmDensity * (viewAngle * 0.2 + 0.8) * 0.03;
    vec3 surfaceGlow = (1.0 - cloudValue) * airGlow1 * baseSurfaceColor;
    vec3 cloudGlow = cloudValue * airGlow1 * TargetCloudColor;
    vec3 airglow = surfaceGlow + cloudGlow;


    // for atmosphere tint
    vec3 atmFilter = getAtmFilter(
        planetSkyHeight,
        playerHeight,
        U,
        V,
        LocalAtmDensity,
        LocalSunriseColor
    );

    vec3 finalColor =
        (totalReflectedLight + emitted + textureEmission + airglow)
        * atmFilter
        * BrightnessMultiplier;

    fragColor = vec4(finalColor, 1.0);
}
