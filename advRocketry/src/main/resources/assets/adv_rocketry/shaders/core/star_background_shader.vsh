#version 150

in vec3 Position;
in vec4 Color;

out vec4 vColor;
uniform mat4 ViewMat;
uniform mat4 ModelMat; // Model space to Universe space
uniform mat4 WorldMat; // Universe space to World space
uniform mat4 ProjMat;
uniform float BrightnessModifier; // lower brightness with much atmosphere

void main() {
    gl_Position = ProjMat * ViewMat *WorldMat* ModelMat * vec4(Position, 1.0);
    vColor = Color * BrightnessModifier;
}