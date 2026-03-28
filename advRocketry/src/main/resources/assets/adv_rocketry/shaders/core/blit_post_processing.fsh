#version 150

uniform sampler2D Frame;
uniform sampler2D Bloom;
uniform float bloomIntensity;

#moj_import "adv_rocketry:utils.glsl"

in vec2 texCoord;

out vec4 fragColor;

float interleavedGradientNoise(vec2 n) {
    return fract(52.9829189 * fract(dot(n, vec2(0.06711056, 0.00583715))));
}

void main() {
    vec3 textureColor = texture(Frame, texCoord).rgb;

    vec3 bloomColor = texture(Bloom, texCoord).rgb * bloomIntensity;

    // add bloom & texture
    textureColor = textureColor + bloomColor;

    // tonemapping
    textureColor = textureColor / (vec3(1) + textureColor);

    // gamma correction
    textureColor = pow(textureColor, vec3(1.0 / 2.2));

    // Apply dithering (shift the noise to be between -0.5 and 0.5, then divide by 255)
    // (without it there will be strange artifacts because colors are limited to 8bit)
    float noise = interleavedGradientNoise(gl_FragCoord.xy);
    textureColor += (noise - 0.5) / 255.0;

    // TODO: apply gamma setting for increase or decrease brightness
    //textureColor += (1 - textureColor) * 0.25; // TODO: multiply by the part over 50%

    textureColor = clamp(textureColor, 0, 1);

    fragColor = vec4(textureColor,1);
}
