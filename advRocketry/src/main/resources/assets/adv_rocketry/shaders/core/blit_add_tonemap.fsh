#version 150

uniform sampler2D Atmosphere;
uniform sampler2D SpaceBackground;
uniform sampler2D SpaceBackgroundBloom;
uniform float bloomIntensity;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 textureColor = texture(SpaceBackground, texCoord).rgb + texture(Atmosphere, texCoord).rgb;

    vec3 bloomColor = texture(SpaceBackgroundBloom, texCoord).rgb * bloomIntensity;

    // add bloom & texture
    textureColor = textureColor + bloomColor;

    // tonemapping
    textureColor = textureColor / (vec3(1) + textureColor);

    // gamma correction
    textureColor = pow(textureColor, vec3(1.0 / 2.2));

    fragColor = vec4(textureColor,1);
}
