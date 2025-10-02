#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 rotatedNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    mat3 normalMatrix = transpose(inverse(mat3(ModelViewMat)));
    rotatedNormal = normalize(normalMatrix * Normal);

}