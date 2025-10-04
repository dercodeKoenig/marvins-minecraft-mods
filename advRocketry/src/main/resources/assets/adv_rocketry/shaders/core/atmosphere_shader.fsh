#version 150

uniform vec3 SkyColor;
uniform vec3 SunriseColor;
uniform vec3 FogColor;

uniform float playerHeight;
uniform float renderDistance;

in vec3 normalViewSpace;
in vec3 upViewSpace;

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform int LightCount;

in vec3 LightVectors_ViewSpace[MAX_LIGHTS];

out vec4 fragColor;

void main() {

    vec3 SkyColorHeightAdjusted = SkyColor * max(0,(10000-playerHeight) / 10000);

    // 1. Get the base vertical factor
    float verticalDot = dot(normalize(upViewSpace), -normalize(normalViewSpace));

    // 2. Calculate the horizon dip based on planet curvature
    // This is the more precise formula.
    float h = playerHeight;
    float R = renderDistance;
    float R_plus_h = R + h;

    float horizonDip = sqrt(2.0 * R * h + h * h) / R_plus_h;

    // 3. Remap the vertical factor to account for the new horizon
    // The logic remains the same: shift and rescale the range.
    float fogFactor = (verticalDot + horizonDip) / (1.0 + horizonDip);
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    // 4. Apply the artistic curve
    fogFactor = pow(fogFactor, 1.0); // Adjust exponent for feel





    vec3 cumulativeSkyColor = vec3(0);

    for (int i = 0; i < LightCount; i++) {

        vec3 starVector = LightVectors_ViewSpace[i];
        vec4 starColor = LightColors[i];
        vec3 starDir = normalize(starVector);
        float starDistance = length(starVector);

        // how much is the sun aligned with my up vector (how much it is up in the sky)
        float sunUp = dot(starDir, upViewSpace);

        // how bright the sky should be
        float brightness = (max(0, sunUp)+0.05)/1.05 * starColor.a / (starDistance*starDistance);

        // if the sun is below 0 (below the horizon) the sunset should fade out quick to bring darkness
        //float sunBelowHorizon = max(0,-sunUp);
        //float sunsetFactor = 1-pow(sunBelowHorizon, 0.1);

        // how much the sun is at horizon
        float sunAtHorizon = 1 - abs(sunUp);
        float sunAtHorizon_adjusted = pow(sunAtHorizon, 4);

        // how much is the sun aligned with the fragments normal, note that the skybox normals point towards inside
        float sunDot = -dot(starDir, normalViewSpace);
        float sunDot_adjusted = (sunDot+1) / 2;// transform -1 - 1 to 0 - 1
        sunDot_adjusted = (sunDot_adjusted+0.1)/1.1;// add some base value

        // how much is the fragment at the horizon, can be used to scale sunrise color vertically
        float upDot = dot(normalViewSpace, upViewSpace);
        float horizonFactor = pow(1-abs(upDot), 6);

        vec3 foggedSkyColor = mix(FogColor, SkyColorHeightAdjusted, fogFactor);

        vec3 skyColorOut = mix(foggedSkyColor * brightness, SunriseColor * brightness, sunDot_adjusted * sunAtHorizon_adjusted * horizonFactor);

        cumulativeSkyColor = cumulativeSkyColor + skyColorOut;
    }

    fragColor = vec4(cumulativeSkyColor,1);

}