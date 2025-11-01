#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ViewMat;
uniform mat4 ModelMat; // Model space to World space
uniform mat4 WorldMat; // Universe space to World space
uniform mat4 ProjMat;


out vec3 normalUniverseSpace;
out vec3 upUniverseSpace;

void main() {
    gl_Position = ProjMat * ViewMat * ModelMat * vec4(Position, 1.0);

    // Get the rotation matrices
    mat3 rotModel = mat3(ModelMat);
    // rotWorldInv transforms vectors from World space to Universe space
    mat3 rotWorldInv = transpose(mat3(WorldMat));
    // Normal is transformed from Model -> World (rotModel) -> Universe (rotWorldInv)
    normalUniverseSpace = normalize(rotWorldInv * (rotModel * Normal));

    // World up in universe space
    upUniverseSpace = normalize(rotWorldInv * vec3(0,1,0));
}