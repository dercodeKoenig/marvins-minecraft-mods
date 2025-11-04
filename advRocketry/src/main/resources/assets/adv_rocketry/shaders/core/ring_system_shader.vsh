#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ViewMat;
uniform mat4 WorldMat;  // rotate universe space (relative to player position) to world space
uniform mat4 ModelMat; // Model space to universe space (planets are transformed in universe space, relative to the player position)
uniform mat4 ProjMat;

out vec2 texcoord;
out vec3 normalUniverseSpace;
out vec3 universePosition; // the position, note that the model is translated relative to player position so it is just modelMat * position

void main() {

    gl_Position = ProjMat * ViewMat * WorldMat * ModelMat * vec4(Position, 1.0);

    universePosition = (ModelMat * vec4(Position, 1.0)).xyz;

    texcoord = vec2((length(Position.xz) - 0.5) * 2, 0.5);

    // Get the rotation matrices
    mat3 rotModel = mat3(ModelMat);
    // Normal is transformed from Model -> Universe (rotModel)
    normalUniverseSpace = normalize(rotModel * Normal);
}
