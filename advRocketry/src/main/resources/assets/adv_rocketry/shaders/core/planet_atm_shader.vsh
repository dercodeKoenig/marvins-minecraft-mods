#version 150

in vec3 Position;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ViewMat;
uniform mat4 WorldMat;  // rotate universe space (relative to player position) to world space
uniform mat4 ModelMat; // Model space to universe space (planets are transformed in universe space, relative to the player position)
uniform mat4 ProjMat;

out vec3 normalUniverseSpace;

void main() {
    gl_Position = ProjMat * ViewMat * WorldMat * ModelMat * vec4(Position, 1.0);

    // Get the rotation matrices
    mat3 rotModel = mat3(ModelMat);
    // Normal is transformed from Model -> Universe (rotModel)
    normalUniverseSpace = normalize(rotModel * Normal);

}
