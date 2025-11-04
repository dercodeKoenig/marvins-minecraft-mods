#version 150

uniform vec3 SkyColor;
uniform vec3 SunriseColor;
uniform vec3 FogColor;
uniform float AtmDensity;
uniform float planetSkyHeight;

uniform float playerHeight;

in vec3 normalUniverseSpace;
in vec3 localUpUniverseSpace;

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

out vec4 fragColor;

//TODO: to render rings on planets use a sphere with dot(normal, ringnormal) and pow to render rings on local planet - but monitor fps and use low framebuffer size

void main() {
    // how bright the sky should be, TODO: this should also depend on weather multiplier - add global uniform modifier, encode eclipse modifier in star intensity value
    float brightnessModifierPlayerAltitude = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
    float globalBrightnessModifier = brightnessModifierPlayerAltitude * (AtmDensity/(1+AtmDensity));

    // how much the fragment is up, 1 = directly above me, 0 = at horizon
    float verticalDot = dot(localUpUniverseSpace, -normalUniverseSpace);

    // i want fog to blend in at the horizon and below
    // i also want it to be lower when the player is high up
    float fogFactor = max(0, (-verticalDot+0.1)/1.1 - 0.5 * (1 - brightnessModifierPlayerAltitude));
    // 4. Apply the artistic curve
    fogFactor = pow(fogFactor, 0.7); // Adjust exponent for feel

    vec3 finalFogColor = FogColor * fogFactor * globalBrightnessModifier;

    vec3 SkyColorBase = globalBrightnessModifier * SkyColor;



    vec3 cumulativeSkyColor = vec3(0);

    for (int i = 0; i < LightCount; i++) {

        vec3 starVector = vec3(LightVectors[i]);
        vec4 starColor = vec4(LightColors[i]);
        vec3 starDir = normalize(starVector);
        float starDistance = length(starVector);

        // how much is the sun aligned with my up vector (how much it is up in the sky)
        float sunUp = dot(starDir, localUpUniverseSpace);
        // how much the sun is at horizon
        float sunAtHorizon = 1 - max(0,sunUp);
        // how much is the sun aligned with the fragments normal, note that the skybox normals point towards inside
        float sunDot = -dot(starDir, normalUniverseSpace);

        // a fancy curve based on the height of the star above the horizon (dot product to up vector)
        float perStarBrightnessMultiplier = pow(max(0, (sunUp+0.4)/1.4), 0.8);
        // apply intensity + distance modifier
        perStarBrightnessMultiplier *= starColor.a / (starDistance * starDistance);
        // if a fragment is closely aligned with the sun, make it more bright so that the area around the sun is brighter
        perStarBrightnessMultiplier *= 1 + pow(max(0,sunDot), 5) * 2 * AtmDensity;

        // how much is the fragment at the horizon, can be used to scale sunrise color vertically
        float horizonFactor = pow(1 - max(0,verticalDot), 8);

        // glow stronger where the sun is
        float sunriseGlowMultiplier = (sunDot + 1) / 2;// transform -1 - 1 to 0 - 1
        sunriseGlowMultiplier = (sunriseGlowMultiplier + 0.1) / 1.1;// add some base value for sunset glow all around the horizon

        // glowing sunrise color to be added to the base color
        vec3 sunriseGlow =
        SunriseColor *
        starColor.rgb *
        sunriseGlowMultiplier * // glow more where the sun is aligned with the fragment
        pow(sunAtHorizon, 3) * // glow more when sun is at horizon
        horizonFactor * // glow more when the fragment is at horizon (you dont want glow high above you)
        globalBrightnessModifier;

        // brightness adjustment, add fog, sky color and sunrise glow
        vec3 skyColorOut = (finalFogColor + SkyColorBase + sunriseGlow) * perStarBrightnessMultiplier;

        // add
        cumulativeSkyColor = cumulativeSkyColor + skyColorOut;
    }

    fragColor = vec4(cumulativeSkyColor, 1);

}