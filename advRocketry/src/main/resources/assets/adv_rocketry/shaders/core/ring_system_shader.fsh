#version 150

uniform sampler2D Sampler0; // surface texture

#moj_import "adv_rocketry:atmFilter.glsl"

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform float BrightnessMultiplier;

in vec2 texcoord;
in vec3 viewDir;
in vec3 normalUniverseSpace;

in vec3 position; // the position of the fragment
in vec3 planetCenter; // the position of the planet
uniform float planetGeometryScale; // the actual geometry radius that is used for render (in planetMatrix.scale())

// for atm shading modifier
in vec3 localUpUniverseSpace;
uniform float LocalAtmDensity;
uniform vec3 LocalSunriseColor;
uniform float playerHeight;
uniform float planetSkyHeight;

out vec4 fragColor;

vec3 fresnelSchlick(float normalDotViewdir, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - normalDotViewdir, 5.0);
}


// made by gemini - it should calculate a shadow assuming a directional light and applies some softness to it
// it is good enough
float getSoftShadowFactorApprox(
    vec3 fragPos,
    vec3 L,
    vec3 planetCenter,
    float planetRadius,
    float penumbraSize  // controls the softness/width of the penumbra
)
{
    // L is the direction FROM the planet TOWARD the light source (Light Direction)
    // D is the shadow ray direction: FROM the fragment TOWARD the light source
    vec3 D = L;

    vec3 oc = fragPos - planetCenter;

    // 1. Hard Shadow Check (Planet is behind the fragment relative to the light)

    // Ray: P(t) = fragPos + t*D. Fragment is at t=0.
    // t_c is the distance along D to the plane containing the planet center
    float t_c = -dot(oc, D);

    const float EPS = 1e-4;

    // If the planet center is behind the fragment (t_c < 0), it can't occlude.
    if (t_c < EPS) {
        return 1.0; // Fully Lit
    }

    // 2. Perpendicular Distance Check (How far off-center the ray hits)

    // d_perp is the minimum distance between the ray (P(t)) and the planet center.
    // This is the magnitude of the vector rejection of 'oc' onto 'D'.
    float d_perp = length(oc + t_c * D);

    // 3. Occlusion/Penumbra Calculation

    // dist is the distance from the planet's edge (R) to the point d_perp.
    float dist = d_perp - planetRadius;

    // Use a smooth transition (e.g., smoothstep) based on this distance.

    // If dist is highly negative (deep inside the planet), shadow factor is 0.
    // If dist is positive (outside the planet's hard edge), it enters the penumbra.

    // Penumbra starts at R_planet and ends at R_planet + penumbraSize.
    float start = 0.0;
    float end = penumbraSize;

    // Clamp the distance *relative to the edge* to the [start, end] range.
    float factor = clamp(dist / (end - start), 0.0, 1.0);

    // factor = 0.0 (dist <= 0)  -> Deep Shadow (Umbra)
    // factor = 1.0 (dist >= penumbraSize) -> Full Light

    // Optional: Use smoothstep for a nicer blend
    return smoothstep(0.0, 1.0, factor);
}


void main() {

    float specularPower = 5;

    float alphaMultiplier = 1;
    // TODO: tint color?, specular color?

    vec4 baseColor = texture(Sampler0, texcoord);

    float alpha = baseColor.a * alphaMultiplier;

    vec3 baseColorLinRGB = pow(baseColor.rgb, vec3(2.2));

    vec3 normalUniverseSpaceAdjusted = normalize(gl_FrontFacing ? normalUniverseSpace : -normalUniverseSpace);

    vec3 viewDirNormalized = normalize(viewDir);

    vec3 totalColor = vec3(0,0,0);

    for (int i = 0; i < LightCount; i++) {

        // Reconstruct light position and fragment->light direction
        vec3 planetToLight = LightVectors[i];

        float distance = length(planetToLight);
        vec3 L = normalize(LightVectors[i]);
        vec3 C = LightColors[i].rgb * LightColors[i].a;

        vec3 C1 = C  / (distance*distance) * BrightnessMultiplier;

        float shadowFactor = getSoftShadowFactorApprox(
            position,
            L,
            planetCenter,
            planetGeometryScale,
            planetGeometryScale * 0.5
        );

        if(shadowFactor <= 0)
        continue;

        C1 *= shadowFactor;

        vec3 F0 = vec3(0.04);
        vec3 fr = fresnelSchlick(abs(dot(normalUniverseSpace, viewDirNormalized)), F0);

        // specular - bright when starlight reflects into my view
        // TODO: this might not be perfect because it also reflects backside ?
        vec3 halfway = L - viewDir;
        if(length(halfway) > 0){
            halfway = normalize(halfway);
            float reflectionMultiplier = pow(max(0,dot(halfway, normalUniverseSpaceAdjusted)), specularPower);
            totalColor += reflectionMultiplier * C1 * fr ;
        }

        // diffuse - brignt when face is facing the star
        float diffuse = max(0,dot(L, normalUniverseSpaceAdjusted)*0.8+0.2);
        totalColor+= diffuse * C1 * baseColorLinRGB * (1-fr) ;

        // transmission
        float transmission = pow(dot(L, viewDirNormalized) * 0.5 + 0.5, 4);
        totalColor+= transmission * C1 * baseColorLinRGB;
    }


    vec3 atmFilter = getAtmFilter(
        planetSkyHeight,
        playerHeight,
        localUpUniverseSpace,
        viewDir,
        LocalAtmDensity,
        LocalSunriseColor
    );

    totalColor *= atmFilter;

    vec4 normalColor = vec4(totalColor, alpha);

    fragColor = normalColor;

}