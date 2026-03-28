#version 150

uniform sampler2D Frame1;
uniform sampler2D Frame2;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 textureColor = texture(Frame1, texCoord).rgb + texture(Frame2, texCoord).rgb;
    fragColor = vec4(textureColor,1);
}
