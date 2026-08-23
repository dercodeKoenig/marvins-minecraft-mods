#version 150

uniform sampler2D Frame;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(Frame, texCoord);
}
