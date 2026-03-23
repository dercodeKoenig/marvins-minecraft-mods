#version 150

#moj_import "adv_rocketry:noise.glsl"
#moj_import "adv_rocketry:atmFilter.glsl"

uniform sampler2D Sampler0; // surface texture

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform float BrightnessMultiplier;
uniform vec4 emissiveColor;      // planet’s self-emission (rgb + intensity)
uniform float textureBrightness;        // float that scales only texture brightness, emissive color shades other planets, this value only shades this planet, for example to have a bright red star with bloom effect
uniform float LocalAtmDensity;        // observer planet atmosphere
uniform float TargetAtmDensity;  // target planet atmosphere (affects rim)
uniform vec3 LocalSunriseColor;  // tint for sunrise / sunset
uniform vec3 TargetSkyColor;       // target planets sky color
uniform vec3 TargetCloudColor;      // cloud color of the target planet
uniform float TargetCloudValue;       // how much clouds to render, 0 - 1
uniform int CloudSampleSteps;
uniform int CloudWarp;
uniform vec3 TargetReflectiveTextureTintColor; // tint the texture for diffuse light
uniform float playerHeight;         // how high the player is to reduce atm tint for star
uniform float planetSkyHeight;      // how high is considered out of atmosphere
uniform int isLocalPlanet;           // if this is my planet, some special rendering applies
uniform vec3 localTerrainFogColor;
uniform float time; // time in seconds for noise offset

in vec2 texcoord;
in vec3 normalUniverseSpace;
in vec3 viewDir;
in vec3 normalModelSpace;
in vec3 localUpUniverseSpace;

out vec4 fragColor;

void main() {

    vec3 V = normalize(viewDir);
    vec3 N = normalize(normalUniverseSpace);
    vec3 N_model = normalize(normalModelSpace);

    vec3 baseSurfaceColor = texture(Sampler0, texcoord).rgb;
    baseSurfaceColor = pow(baseSurfaceColor, vec3(2.2)); // gamma reverse
    baseSurfaceColor *= textureBrightness;

    if(isLocalPlanet == 1){
        float mixvalue = clamp((playerHeight-350) / planetSkyHeight * 5, 0, 1);
        baseSurfaceColor = mix(localTerrainFogColor, baseSurfaceColor, mixvalue);
    }

    vec3 totalReflectedLight = vec3(0.0);

    // calculate clouds first, the value is independent from lights
    // the noise is extremly expensive and crashes fps significantly on integrated graphics
    // i skip it on local planet where the fragment number is too high
    float cloudValue = 0;
    if(isLocalPlanet != 1 && CloudSampleSteps > 0){
        float amp = 0.8;
        float freq = 2;
        float noiseOffset = pow(TargetCloudValue, 0.5) * 2 - 1;
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
    vec3 cloudColorBase = pow(clamp(cloudValue, 0, 1), 2) * TargetCloudColor;

    vec3 surfaceLightBase = baseSurfaceColor * TargetReflectiveTextureTintColor;

    // for atmosphere
    // how much of the edge (horizon) we see
    float viewAngle = 1.0 - abs(dot(N, V));
    // rim intensity (thicker with higher TargetAtmDensity)
    // the thing that glows on the side
    float rim = pow(viewAngle, 3);  // the more at the side the more atmosphere we will see

    vec3 atmLightMix =
    2 * rim * TargetSkyColor // the atm glow around the planet
    + surfaceLightBase; // the light scatters through atm and hits terrain


    // distant planets like saturn have maybe 1% of earth sunlight
    // eyes would adapt slightly but i do not control the entire render pipeline
    // so i will accumulate the entire star brightness and modify the output brightness with it
    // (only modulate reflected light)
    float totalBrightness = 0;

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        float dist = length(LightVectors[i]);

        float brightness = LightColors[i].a / (dist * dist);
        totalBrightness += brightness;

        // the atm adds extra light after the normal falloff and uses the sky color/rim mix
        float atmLightFactor = max(0, dot(N, L) * 0.8 + 0.2);
        atmLightFactor = pow(atmLightFactor, 2); // with gamma correct the transition from black to less black is too aggressive
        vec3 atmLight = atmLightFactor * atmLightMix;

        // the reflected light without atmosphere consideration, just surface and n°l
        float surfaceLightFactor = max(0, dot(N, L));
        surfaceLightFactor = pow(surfaceLightFactor, 2);
        vec3 surfaceLight = surfaceLightFactor * surfaceLightBase;

        // clouds color
        vec3 cloudLight = cloudColorBase * atmLightFactor;

        // blend surface light and atm light and clouds
        vec3 finalLight = cloudLight + mix(surfaceLight, atmLight, TargetAtmDensity / (1+TargetAtmDensity));

        // final reflected light for this star
        vec3 reflected =
        finalLight
        * LightColors[i].rgb * brightness;


        totalReflectedLight += reflected;
    }

    if(totalBrightness < 1){
        // do not divide by entire brightness to keep the falloff somewhat correct visually ( or sun would be blue from a star 400AU away )
        totalReflectedLight *= 1 / pow(totalBrightness, 0.5);
    }

    vec3 emitted = vec3(0,0,0);
    if(emissiveColor.a > 0) {
        float starBrightness = emissiveColor.a;
        vec3 starColor = emissiveColor.rgb * baseSurfaceColor;
        emitted = starColor * starBrightness;
    }

    vec3 atmFilter = getAtmFilter(
        planetSkyHeight,
        playerHeight,
        normalize(localUpUniverseSpace),
        V,
        LocalAtmDensity,
        LocalSunriseColor
    );

    vec3 finalColor =
        (totalReflectedLight + emitted)
        * atmFilter
        * BrightnessMultiplier;

    fragColor = vec4(finalColor, 1.0);
}
