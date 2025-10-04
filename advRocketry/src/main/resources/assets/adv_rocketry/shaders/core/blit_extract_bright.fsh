#version 150

uniform sampler2D frame;
uniform float threshold;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(frame, texCoord); // Read as vec4 for alpha
    vec3 hdr_color = color.rgb;

    // 1. Calculate the luminance (perceived brightness) of the pixel
    float luminance = dot(hdr_color, vec3(0.2126, 0.7152, 0.0722));

    // 2. Apply the thresholding logic (using a threshold > 1.0)
    if (luminance > threshold)
    {
        // --- Option A: Simple Pass-Through (Bloom Extraction) ---
        // This passes the full HDR color through. The glow will be based
        // on the original color intensity.
        // fragColor = vec4(hdr_color, 1.0);


        // --- Option B: Saturate / Excess Brightness Extraction ---
        // This is often preferred. It subtracts the threshold from the
        // color channels, isolating ONLY the light that is 'excess'
        // to the scene. This can prevent over-blooming.

        // Calculate the ratio of excess luminance to total luminance
        float excess_luminance_ratio = (luminance - threshold) / luminance;

        // Scale the color by the excess ratio
        vec3 saturated_color = hdr_color * excess_luminance_ratio;

        fragColor = vec4(saturated_color, 1.0);
    }
    else
    {
        // The pixel is too dark, output black (no contribution to bloom)
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}