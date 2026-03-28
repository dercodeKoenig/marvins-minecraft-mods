#version 150

in vec3 Position;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ViewMat;
uniform mat4 WorldMat;  // rotate universe space to world space
uniform mat4 ModelMat; // Model space to universe space (planets are transformed in universe space, translated relative to the player position in universe space)
uniform mat4 ProjMat;
uniform vec3 playerEye;

out vec2 texcoord;
out vec3 normalUniverseSpace;
out vec3 localUpUniverseSpace;
out vec3 viewDir;
out vec3 normalModelSpace;

void main() {

    vec3 posUniverseSpace = (ModelMat * vec4(Position, 1.0)).xyz;

    gl_Position = ProjMat * ViewMat * WorldMat * vec4(posUniverseSpace, 1.0);

    viewDir = normalize(posUniverseSpace - playerEye);

    texcoord = UV0;

    normalModelSpace = Normal;

    // Get the rotation matrices
    // Normal is transformed from Model -> Universe (rotModel)
    mat3 normalMatrix = transpose(inverse(mat3(ModelMat)));
    normalUniverseSpace = normalize(normalMatrix * Normal);

    // up to Universe space
    // rotWorldInv transforms vectors from World space to Universe space
    mat3 rotWorldInv = transpose(mat3(WorldMat));
    localUpUniverseSpace = normalize(rotWorldInv * vec3(0,1,0));

}
