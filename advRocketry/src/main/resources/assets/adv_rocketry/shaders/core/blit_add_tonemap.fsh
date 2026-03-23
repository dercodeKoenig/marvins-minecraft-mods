#version 150

uniform sampler2D Atmosphere;
uniform sampler2D SpaceBackground;
uniform sampler2D SpaceBackgroundBloom;
uniform float bloomIntensity;

in vec2 texCoord;

out vec4 fragColor;

float interleavedGradientNoise(vec2 n) {
    return fract(52.9829189 * fract(dot(n, vec2(0.06711056, 0.00583715))));
}

void main() {
    vec3 textureColor = texture(SpaceBackground, texCoord).rgb + texture(Atmosphere, texCoord).rgb;

    vec3 bloomColor = texture(SpaceBackgroundBloom, texCoord).rgb * bloomIntensity;

    // add bloom & texture
    textureColor = textureColor + bloomColor;

    // tonemapping
    textureColor = textureColor / (vec3(1) + textureColor);

    // gamma correction
    textureColor = pow(textureColor, vec3(1.0 / 2.2));

    // Apply dithering (shift the noise to be between -0.5 and 0.5, then divide by 255)
    float noise = interleavedGradientNoise(gl_FragCoord.xy);
    textureColor += (noise - 0.5) / 255.0;

    fragColor = vec4(textureColor,1);
}
