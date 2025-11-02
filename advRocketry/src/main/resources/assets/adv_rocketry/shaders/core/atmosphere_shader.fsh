#version 150

uniform vec3 SkyColor;
uniform vec3 SunriseColor;
uniform vec3 FogColor;
uniform float AtmDensity;
uniform float planetSkyHeight;

uniform float playerHeight;

in vec3 normalUniverseSpace;
in vec3 upUniverseSpace;

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

out vec4 fragColor;

//TODO: to render rings on planets use a sphere with dot(normal, ringnormal) and pow to render rings
vec3 gamma_reverse(vec3 color){
    return pow(color, vec3(2.2));
}
void main() {
    // how bright the sky should be, TODO: this should also depend on weather multiplier - add global uniform modifier, encode eclipse modifier in star intensity value
    float brightnessModifierPlayerAltitude = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
    float globalBrightnessModifiew = brightnessModifierPlayerAltitude * (AtmDensity/(1+AtmDensity));

    float verticalDot = dot(upUniverseSpace, -normalUniverseSpace);

    // i want fog to blend in at the horizon and below
    // i also want it to be lower when the player is high up
    float fogFactor = max(0, (-verticalDot+0.1)/1.1 - 0.5 * (1 - brightnessModifierPlayerAltitude));
    // 4. Apply the artistic curve
    fogFactor = pow(fogFactor, 0.7); // Adjust exponent for feel

    vec3 finalFogColor = gamma_reverse(FogColor) * fogFactor * globalBrightnessModifiew;

    vec3 SkyColorBase = globalBrightnessModifiew * gamma_reverse(SkyColor);



    vec3 cumulativeSkyColor = vec3(0);

    for (int i = 0; i < LightCount; i++) {

        vec3 starVector = vec3(LightVectors[i]);
        vec4 starColor = vec4(LightColors[i]);
        vec3 starDir = normalize(starVector);
        float starDistance = length(starVector);

        // how much is the sun aligned with my up vector (how much it is up in the sky)
        float sunUp = dot(starDir, upUniverseSpace);

        // a fancy curve based on the height of the star above the horizon (dot product to up vector)
        float perStarBrightnessMultiplier = (pow(max(0, (sunUp+0.4)/1.4), 1) + 0.0001) / 1.0001 * starColor.a / (starDistance * starDistance);

        // how much the sun is at horizon
        float sunAtHorizon = 1 - max(0,sunUp);
        float sunAtHorizon_adjusted = pow(sunAtHorizon, 4);

        // how much is the sun aligned with the fragments normal, note that the skybox normals point towards inside
        float sunDot = -dot(starDir, normalUniverseSpace);
        float sunDot_adjusted = (sunDot + 1) / 2;// transform -1 - 1 to 0 - 1
        sunDot_adjusted = (sunDot_adjusted + 0.1) / 1.1;// add some base value for sunset glow all around the horizon

        // if a fragment is closely aligned with the sun, make it more bright so that the area around the sun is brighter
        float extraBrightness = pow(max(0,sunDot), 10) * 2*AtmDensity + 1;

        // how much is the fragment at the horizon, can be used to scale sunrise color vertically
        float horizonFactor = pow(1 - max(0,verticalDot), 8);

        // glowing sunrise color to be added to the base color
        vec3 sunriseGlow =
        gamma_reverse(SunriseColor) *
        gamma_reverse(starColor.rgb) *
        sunDot_adjusted *
        sunAtHorizon_adjusted *
        horizonFactor *
        globalBrightnessModifiew;

        // brightness adjustment, add fog, sky color and sunrise glow
        vec3 skyColorOut = (finalFogColor + SkyColorBase + sunriseGlow) * perStarBrightnessMultiplier * extraBrightness;

        // add
        cumulativeSkyColor = cumulativeSkyColor + skyColorOut;
    }

    fragColor = vec4(cumulativeSkyColor, 1);

}