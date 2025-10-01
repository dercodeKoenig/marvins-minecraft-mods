#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec3 Light0_Direction;

out float vertexHeight;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vertexHeight = Position.y;

}