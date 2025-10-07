#version 150

in vec3 Position;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ViewMat;
uniform mat4 WorldMat;  // rotate universe space (relative to player position) to world space
uniform mat4 ModelMat; // Model space to universe space (planets are transformed in universe space, relative to the player position)
uniform mat4 ProjMat;


uniform vec3 LocalUpUniverseSpace;

out vec2 texcoord;
out vec3 normalUniverseSpace;
out vec3 upUniverseSpace;

void main() {
    gl_Position = ProjMat * ViewMat * WorldMat * ModelMat * vec4(Position, 1.0);

    // Get the rotation matrices
    mat3 rotModel = mat3(ModelMat);
    // Normal is transformed from Model -> Universe (rotModel)
    normalUniverseSpace = normalize(rotModel * Normal);

    // up to Universe space
    // rotWorldInv transforms vectors from World space to Universe space
    mat3 rotWorldInv = transpose(mat3(WorldMat));
    upUniverseSpace = normalize(rotWorldInv * vec3(0,1,0)).xyz;

    texcoord = UV0;
}
