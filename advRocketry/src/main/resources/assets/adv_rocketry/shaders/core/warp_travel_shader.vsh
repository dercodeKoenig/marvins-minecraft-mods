#version 150

in vec3 Position;

uniform mat4 ViewMat;
uniform mat4 ModelMat; // Model space to World space
uniform mat4 WorldMat; // Universe space to World space
uniform mat4 ProjMat;

out vec3 vertexDirUniverseSpace;
out vec3 localUpUniverseSpace;

void main() {
    gl_Position = ProjMat * ViewMat * ModelMat * vec4(Position, 1.0);

    // Get the rotation matrices
    mat3 rotModel = mat3(ModelMat);
    // rotWorldInv transforms vectors from World space to Universe space
    mat3 rotWorldInv = transpose(mat3(WorldMat));

    // We use the local Position as the direction.
    // This works perfectly for a sphere centered at eye pos.
    vertexDirUniverseSpace = normalize(rotWorldInv * (rotModel * Position));

    // World up in universe space
    localUpUniverseSpace = normalize(rotWorldInv * vec3(0,1,0));
}