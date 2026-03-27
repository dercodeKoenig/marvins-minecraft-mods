#version 150

#moj_import "adv_rocketry:atm_filter.glsl"

uniform vec3 SkyColor;
uniform vec3 SunriseColor;
uniform vec3 FogColor;
uniform float AtmDensity;
uniform float planetSkyHeight;

uniform float playerHeight;

in vec3 v_vertexDirUniverseSpace;
in vec3 v_localUpUniverseSpace;

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

out vec4 fragColor;

void main() {

    vec3 V = normalize(v_vertexDirUniverseSpace);
    vec3 U = normalize(v_localUpUniverseSpace);

    // how much the fragment is up, 1 = directly above me, 0 = at horizon
    float verticalDot = dot(U, V);

    // how much is the fragment at the horizon, used to scale sunrise color vertically
    float horizonFactor = pow(1 - max(0,verticalDot), 8);

    // how high relative to sky height is the player
    float playerHeight = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);

    // atm thickness for this fragment
    float atmThickness = getAtmThickness(playerHeight, U, V, AtmDensity);

    // how bright the sky should be, TODO: this should also depend on weather multiplier - add global uniform modifier, encode eclipse modifier in star intensity value
    float globalBrightnessModifier = atmThickness;

    vec3 SkyColorBase = SkyColor * globalBrightnessModifier;

    vec3 cumulativeSkyColor = vec3(0);

    vec3 cumulativeSunriseGlow = vec3(0);

    for (int i = 0; i < LightCount; i++) {

        vec3 starVector = vec3(LightVectors[i]);
        vec4 starColor = vec4(LightColors[i]);
        vec3 starDir = normalize(starVector);
        float starDistance = length(starVector);

        // how much is the sun aligned with my up vector (how much it is up in the sky)
        float sunUp = dot(starDir, U);
        // how much the sun is at horizon
        float sunAtHorizon = 1 - max(0,sunUp);
        // how much is the sun aligned with the fragments direction
        float sunDot = dot(starDir, V);

        // a fancy curve based on the height of the star above the horizon (dot product to up vector)
        // note that future gamma correction will make dark areas brighter, so i adjust the pow factor to compensate it
        // it is not about beeing physically correct, it just has to look good enough
        float perStarBrightnessMultiplier = pow(max(0, (sunUp+0.3)/1.3), 2);
        // apply intensity + distance modifier
        perStarBrightnessMultiplier *= starColor.a / (starDistance * starDistance);
        // if a fragment is closely aligned with the sun, make it more bright so that the area around the sun is brighter
        perStarBrightnessMultiplier *= 1 + pow(max(0,sunDot), 2) * AtmDensity;

        // glow stronger where the sun is
        float sunriseGlowMultiplier = (sunDot + 1) / 2;// transform -1 - 1 to 0 - 1

        // glowing sunrise color to be added to the base color
        vec3 sunriseGlow =
        SunriseColor *
        starColor.rgb *
        sunriseGlowMultiplier * // glow more where the sun is aligned with the fragment
        pow(sunAtHorizon, 10) * // glow more when sun is at horizon
        horizonFactor * // glow more when the fragment is at horizon (you dont want glow high above you)
        globalBrightnessModifier;

        // add
        cumulativeSkyColor += SkyColorBase * perStarBrightnessMultiplier;
        cumulativeSunriseGlow += sunriseGlow * perStarBrightnessMultiplier;
    }

    // Apply global extinction once after all lights are added
    // Lots of atmosphere makes it dark
    float extinction = exp(-atmThickness);
    cumulativeSkyColor *= extinction;
    cumulativeSunriseGlow *= extinction;

    // Add Fog
    // i want fog to blend in at the horizon and below
    // i also want it to be lower when the player is high up
    float fogFactor = clamp((-verticalDot+0.1) * 4 - 0.5 * (1 - playerHeight), 0, 1);
    // Apply the artistic curve
    fogFactor = pow(fogFactor, 2); // Adjust exponent for feel
    // the fog should not be modified in any way because it has to match the terrain fog color
    // simple blend will do

    // blend skycolor and fog color and add sunrise glow
    cumulativeSkyColor =
        cumulativeSkyColor * (1.1 - fogFactor)
        + FogColor * fogFactor
        + cumulativeSunriseGlow;

    // encode atmThickness in alpha channel to be potentially used in future shaders
    fragColor = vec4(cumulativeSkyColor, atmThickness);
}