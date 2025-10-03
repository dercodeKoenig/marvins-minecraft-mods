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

        ///////////// sunrise / sunset calculations
        // how much is the fragment at the horizon
        // float upDot = dot(normalViewSpace, upViewSpace);
        // float horizonFactor = pow(1-abs(upDot), 3);

        vec3 starVector = LightVectors_ViewSpace[i];
        vec4 starColor = LightColors[i];
        vec3 starDir = normalize(starVector);


        // how much is the sun aligned with my up vector (how much it is up in the sky)
        float sunUp = dot(starDir, upViewSpace);

        // how bright the sky should be
        float brightness = max(0, sunUp);
        vec3 skyColorOut = SkyColor * brightness;

        // if the sun is below 0 (below the horizon) the sunset should fade out quick to bring darkness
        //float sunBelowHorizon = max(0,-sunUp);
        //float sunsetFactor = 1-pow(sunBelowHorizon, 0.1);

        // how much the sun is at horizon
        float sunAtHorizon = 1 - abs(sunUp);

        // how much is the sun aligned with the fragments normal, note that the skybox normals point towards inside
        float sunDot = -dot(starDir, normalViewSpace);
        float sunDot_0_1 = (sunDot+1) / 2;// transform -1 - 1 to 0 - 1

        skyColorOut = mix(skyColorOut, SunriseColor, sunDot_0_1 * sunAtHorizon);

        cumulativeSkyColor = cumulativeSkyColor + vec4(skyColorOut, 1);
    }

    fragColor = cumulativeSkyColor / (vec4(1) + cumulativeSkyColor);

}