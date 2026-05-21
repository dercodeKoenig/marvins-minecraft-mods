#version 150

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform float TargetAtmDensity;  // target planet atmosphere (affects rim)
uniform vec3 TargetSkyColor;       // target planets sky color

in vec2 texcoord;
in vec3 normalUniverseSpace;
in vec3 viewDir;
in vec3 normalModelSpace;
in vec3 localUpUniverseSpace;

out vec4 fragColor;

// TODO: maybe use same brightness rescale like in the planet / ring shader?

void main() {

    vec3 U = normalize(localUpUniverseSpace);
    vec3 V = normalize(viewDir);
    vec3 N = normalize(normalUniverseSpace);
    vec3 N_model = normalize(normalModelSpace);

    // find a normalization for the target atmosphere density to keep the math stable
    float normalizedTargetAtmDensity = TargetAtmDensity / (1 + TargetAtmDensity);

    // Invert the incoming view vector to point straight from the fragment to the camera
    vec3 V_to_eye = -V;

    // --- 1. SCREEN-SPACE RADIUS CALCULATION ---
    // NdotV drops from 1.0 (dead center of the planet disk) down to 0.0 (the absolute outer edge).
    // Using the geometric identity sin^2(x) + cos^2(x) = 1, we convert this angular falloff into
    // a linear screen-space radius 'r' that goes from 0.0 (center) to 1.0 (space silhouette edge).
    float NdotV = max(0.0, dot(N, V_to_eye));
    float r2 = max(0.0, 1.0 - NdotV * NdotV);
    float r = sqrt(r2);

    // --- 2. VOLUMETRIC PROFILE (ALPHA GRADIENT) ---
    float atmosphereAlpha = 0.0;
    float horizonRadius = 0.925926; // 1.0 / 1.08: The screen-space boundary where the solid ground ends

    if (r < horizonRadius) {
        // FRONT FACE: Keep the center of the planet highly transparent using a steep pow() curve.
        // This ensures thick atmospheric fog doesn't wash out terrain or cloud details on the ground.
        atmosphereAlpha = pow(r / horizonRadius, 16.0);
    } else {
        // OUTER HALO: Calculate a soft polynomial fade-out for the air bleeding into open space.
        // Polynomial shapes survive Reinhard HDR filters much better than raw exponential decay.
        float haloT = (1.0 - r) / (1.0 - horizonRadius);
        atmosphereAlpha = pow(max(0.0, haloT), 4);
    }

    vec3 targetColor = vec3(0.0);

    // --- 3. DYNAMIC LIGHTING LOOP ---
    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        float dist = length(LightVectors[i]);
        float brightness = LightColors[i].a / (dist * dist); // Inverse-square light attenuation

        float NdotL = dot(N, L);
        float VdotL = dot(V, L);

        // --- LAYER A: ENHANCED DAY/TWILIGHT SIDE ---
        // Wrap the diffuse term slightly (* 0.8 + 0.2) to let the atmosphere hold its color softly
        // over the curvature of the terminator line.
        float atmLightFactor = max(0.0, NdotL * 0.8 + 0.2);
        float baseDiffuse = pow(atmLightFactor, 2);
        vec3 dayLight = LightColors[i].rgb * brightness * TargetSkyColor * baseDiffuse * 1.5;

        // --- LAYER B: TWILIGHT BACKLIT RIM (Forward Scattering) ---
        // By wrapping the view-light vector alignment (VdotL * 0.5 + 0.5), we smooth out the extreme
        // mathematical peak of forward scattering. This keeps its values small enough to prevent
        // the Reinhard tonemapper from crushing the gradient into a flat neon line.
        float forwardScattering = pow(max(0.0, VdotL * 0.5 + 0.5), 16.0) * 0.5;

        // --- LAYER C: THE INTERNAL COREMASK ---
        // To prevent the background scattering from rendering uniformly over the center of the night side,
        // we isolate the inner rim. From r = 0.90 to r = 0.925926, the backlight ramps up from 0 to 1.
        // This creates a tight, realistic twilight bleed over the dark crust without spilling into the continents.
        float backlightMask = 1.0;
        if (r < horizonRadius) {
            backlightMask = smoothstep(0.90, horizonRadius, r);
        }

        // Calculate final backlit rim light color contribution
        vec3 backlitLight = LightColors[i].rgb * brightness * TargetSkyColor * forwardScattering * backlightMask;

        // Accumulate both daylight and backlit layers into the scene buffer
        targetColor += dayLight + backlitLight;
    }

    // --- 4. RENDER OUTPUT ---
    // Scale the geometric profile alpha by the target atmospheric density factor.
    float finalAlpha = atmosphereAlpha * normalizedTargetAtmDensity * 0.7;

    fragColor = vec4(targetColor, finalAlpha);
}
