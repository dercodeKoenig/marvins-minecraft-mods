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

    vec4 cumulativeSkyColor = vec4(0);

    for (int i = 0; i < LightCount; i++) {

        vec3 starVector = LightVectors_ViewSpace[i];
        vec4 starColor = LightColors[i];
        vec3 starDir = normalize(starVector);
        float starDistance = length(starVector);

        // how much is the sun aligned with my up vector (how much it is up in the sky)
        float sunUp = dot(starDir, upViewSpace);

        // how bright the sky should be

        float brightness = max(0, sunUp) * starColor.a / (starDistance*starDistance);
        vec3 skyColorOut = SkyColor * brightness;

        // if the sun is below 0 (below the horizon) the sunset should fade out quick to bring darkness
        //float sunBelowHorizon = max(0,-sunUp);
        //float sunsetFactor = 1-pow(sunBelowHorizon, 0.1);

        // how much the sun is at horizon
        float sunAtHorizon = 1 - abs(sunUp);

        // how much is the sun aligned with the fragments normal, note that the skybox normals point towards inside
        float sunDot = -dot(starDir, normalViewSpace);
        float sunDot_adjusted = (sunDot+1) / 2;// transform -1 - 1 to 0 - 1
        sunDot_adjusted = (sunDot_adjusted+0.1)/1.1;// add some base value

        // how much is the fragment at the horizon, can be used to scale sunrise color vertically
        float upDot = dot(normalViewSpace, upViewSpace);
        float horizonFactor = pow(1-abs(upDot), 5);

        skyColorOut = mix(skyColorOut, SunriseColor, sunDot_adjusted * sunAtHorizon * horizonFactor);

        cumulativeSkyColor = cumulativeSkyColor + vec4(skyColorOut, 1);
    }

    fragColor = cumulativeSkyColor / (vec4(1) + cumulativeSkyColor);

}