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
    if(texcoord.x < 0)
        // too close to planet
        discard;

    float alphaMultiplier = 1;
    // TODO: tint color?, specular color?

    vec4 baseColor = texture(Sampler0, texcoord);
    vec3 baseColorLinRGB = pow(baseColor.rgb, vec3(2.2));

    float alpha = baseColor.a * alphaMultiplier;

    vec3 N0 = normalize(normalUniverseSpace);
    vec3 N = gl_FrontFacing ? N0 : -N0;

    vec3 V = normalize(viewDir);

    vec3 totalColor = vec3(0,0,0);

    // see why this exists in planet shader
    float totalBrightness = 0;

    for (int i = 0; i < LightCount; i++) {

        // Reconstruct light position and fragment->light direction
        vec3 planetToLight = LightVectors[i];

        float distance = length(planetToLight);

        float brightness = LightColors[i].a / (distance * distance);
        totalBrightness += brightness;

        vec3 L = normalize(LightVectors[i]);

        vec3 C1 = LightColors[i].rgb * brightness;

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

        float NdotV = clamp(dot(N, V), -1, 1);
        float NdotL = clamp(dot(L, N), -1, 1);
        float LdotV = clamp(dot(L, V), -1, 1);

        vec3 F0 = vec3(0.04);
        vec3 fr = fresnelSchlick(abs(NdotV), F0);

        // specular - bright when starlight reflects into my view
        vec3 halfway = L - V;
        if(length(halfway) > 0.0001){
            halfway = normalize(halfway);
            float reflectionMultiplier = pow(max(0, dot(halfway, N)), 10);
            totalColor += reflectionMultiplier * C1 * fr ;
        }

        // diffuse - bright when face is facing the star
        float diffuse = max(0, NdotL * 0.95 + 0.05);
        totalColor+= diffuse * C1 * baseColorLinRGB * (1 - fr) ;

        // transmission
        // ok, LdotV should always be -1 to 1 and *0.5 +0.5 should always make it 0-1
        // BUT float precision can cause it to go negative and it blacks out entire regions of the screen from NAN
        // so ALWAYS CLAMP RESULTS!!!!
        float transmission = pow(max(0, LdotV * 0.5 + 0.5), 50) * (1 - 0.9 * abs(NdotL));
        totalColor+= transmission * C1 * baseColorLinRGB;

        // Ambient light reflected from planet
        // (but not in the shadow)
        // Apply a small constant 'ambient' that doesn't care about the normal
        // This ensures the rings never go pitch black
        // I want it more significant in front of the planet and less significant behind
        // (optional) i could multiply it with planet reflective texture tint but i am lazy
        float distanceToPlanet = 1 + texcoord.x;
        vec3 planetShine = baseColorLinRGB * C1 / (distanceToPlanet * distanceToPlanet);
        float shineFactor = dot(normalize(position - planetCenter), L) * 0.5 + 0.7;
        shineFactor = pow(shineFactor, 2);
        totalColor += planetShine * shineFactor * 0.2;
    }

    if(totalBrightness < 1){
        totalColor *= 1 / pow(totalBrightness, 0.5);
    }


    vec3 atmFilter = getAtmFilter(
        planetSkyHeight,
        playerHeight,
        normalize(localUpUniverseSpace),
        V,
        LocalAtmDensity,
        LocalSunriseColor
    ).rgb;

    totalColor *= BrightnessMultiplier;

    totalColor *= atmFilter;

    fragColor = vec4(totalColor, alpha);

}