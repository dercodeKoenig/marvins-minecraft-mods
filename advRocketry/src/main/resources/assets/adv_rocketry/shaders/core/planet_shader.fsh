#version 150

uniform sampler2D Sampler0;
uniform vec3 Light0_Direction;
uniform vec3 AtmColor;

in vec4 vertexColor;
in vec2 texCoord0;

in vec3 rotatedNormal;

out vec4 fragColor;

const float ATM_BLEED = 0.08;
const vec3 u_emissiveColor = vec3(0.2,0.2,0.2); // Light emitted by the planet (e.g., city lights, glow)


void main() {
    float lightIntensity = dot(rotatedNormal, Light0_Direction);

    // 1. Get the base planet color (reflectance)
    vec4 planetColor = texture(Sampler0, texCoord0) * vertexColor;

    // 2. Calculate the blend factor (t) for the reflected light
    // t controls the interpolation between AtmColor (dark side) and planetColor (lit side)
    float t = clamp(lightIntensity + ATM_BLEED, 0.0, 1.0);

    // 3. Calculate the REFLECTED Color (using MIX)
    // The amount of light reflected by the surface (planetColor) is blended with the sky color (AtmColor)
    vec3 reflectedColor = mix(AtmColor.rgb, planetColor.rgb, t);

    // --- Now incorporate Emissive Light ---

    // 4. Calculate the EMITTED Color
    // Emissive light is usually constant, but we can darken it slightly on the lit side
    // where the Sun's light dominates, although often it's just added directly.

    // For a constant glow (simplest method):
    vec3 emittedColor = u_emissiveColor * planetColor.rgb * 3;

    // For a glow visible only on the dark side (like city lights):
    // float emittanceFactor = 1.0 - t; // Emissive light fades out as 't' (sunlight) increases
    // vec3 emittedColor = u_emissiveColor * emittanceFactor;

    // 5. Combine using ADD
    // Add the self-emitted light to the reflected/blended color
    vec3 finalColorRGB = reflectedColor + emittedColor;

    // 6. Set the final fragment color
    // We clamp to 1.0 here to prevent colors from getting too saturated and losing detail,
    // although additive effects often allow values > 1.0 if using HDR rendering.
    fragColor = vec4(clamp(finalColorRGB, 0.0, 1.0), 1.0);

    // NOTE: If you are using High Dynamic Range (HDR) rendering, you would skip the clamp,
    // allowing values > 1.0 to give the emitted light an extra 'pop' and brightness.
}