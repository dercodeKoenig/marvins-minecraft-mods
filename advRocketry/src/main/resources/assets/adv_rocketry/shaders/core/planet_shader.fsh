#version 150

// Existing Uniforms
uniform sampler2D Sampler0;
uniform vec3 Light0_Direction;
uniform vec3 AtmColor; // The background sky color at this pixel

uniform float reflectivity;
uniform vec3 emissiveColor;


in vec2 texCoord0;
in vec3 rotatedNormal;
out vec4 fragColor;

void main() {
    // Calculate raw illumination factor:
    float lightIntensity = dot(rotatedNormal, Light0_Direction);

    // 1. Get the base planet color (texture/vertex color):
    vec3 baseSurfaceColor = (texture(Sampler0, texCoord0)).rgb;

    // 2. Calculate the REFLECTED LIGHT contribution:
    // Only the positive part of lightIntensity matters for reflection.
    float reflectedFactor = max(0.0, lightIntensity);

    // Scale the surface color by the actual reflected light amount
    //vec3 reflectedLight = baseSurfaceColor * reflectedFactor* reflectivity;
    vec3 reflectedLight = baseSurfaceColor * reflectedFactor* 1;

    // 3. Apply Atmospheric Blending (The Mix)
    // The blend factor 't' determines where the atmosphere takes over.
    // It is clamped to [0, 1] and uses the bleed factor for softness.
    //float t = clamp(lightIntensity - AtmColor.length() + 1, 0.0, 1.0);
    float t = clamp(lightIntensity, 0.0, 1.0);

    // The final color for the reflected light component:
    // On the dark side (t=0), this is AtmColor.
    // On the lit side (t=1), this is the fully lit reflected light.
    vec3 blendedReflectedColor = mix(AtmColor.rgb, reflectedLight, t);

    // 4. Calculate the EMITTED LIGHT contribution (The Add)
    // Emitted light is constant across the surface (or based on a separate texture/map)
    // We can multiply the emissive value by the surface color to color the emission.
    vec3 emittedLight = emissiveColor * baseSurfaceColor.rgb;

    // 5. Final Combination
    // Add the emitted light to the blended reflected light.
    vec3 finalColorRGB = blendedReflectedColor + emittedLight;

    // 6. Output Color
    // Clamp to [0, 1] for LDR display. Remove clamp if using HDR.
    fragColor = vec4(clamp(finalColorRGB, 0.0, 1.0), 1.0);
}