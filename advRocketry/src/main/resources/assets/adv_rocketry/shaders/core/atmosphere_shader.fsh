#version 150

uniform sampler2D planetTexture; // HDR planets/stars texture

uniform vec4 Color;      // LDR sky color from the game
uniform vec4 FogColor;   // LDR fog color from the game
uniform int screenWidth;
uniform int screenHeight;
uniform float playerHeight;
uniform float renderDistance;

in vec3 normalViewSpace;
in vec3 upViewSpace;
out vec4 fragColor;

void main() {
    // 1. Get texture coordinates and the HDR color of the planet/star
    vec2 uv = gl_FragCoord.xy / vec2(screenWidth, screenHeight);
    vec4 planetColorHDR = texture(planetTexture, uv);

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
    fogFactor = pow(fogFactor, 0.5); // Adjust exponent for feel

    vec4 colorHeightAdjusted = Color * max(0,(10000-playerHeight) / 1000);

    // Use this final fogFactor to mix your colors
    vec4 backgroundColor = mix(FogColor, colorHeightAdjusted, fogFactor);

    // 4. Tonemap and gamma correct the HDR planet color separately
    // This brings the bright planet values down into the visible [0, 1] LDR range.
    vec4 planetColorLDR = planetColorHDR / (1.0 + planetColorHDR);
    planetColorLDR = pow(planetColorLDR, vec4(1.0 / 2.2));

    // 5. Add the LDR planet color to the LDR background color
    // Additive blending makes the planets appear luminous against the sky. Because you're adding
    // it to the final fogged background, the planet will still be visible at the horizon.
    fragColor = backgroundColor + planetColorLDR;
}