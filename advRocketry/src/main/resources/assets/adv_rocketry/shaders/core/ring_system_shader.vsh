#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ViewMat;
uniform mat4 WorldMat;  // rotate universe space (relative to player position) to world space
uniform mat4 ModelMat; // Model space to universe space (planets are transformed in universe space, relative to the player position)
uniform mat4 ProjMat;

out vec2 texcoord;
out vec3 normalUniverseSpace;
out vec3 viewDir; // the direction of the fragment relative to the camera for specular light

void main() {

    gl_Position = ProjMat * ViewMat * WorldMat * ModelMat * vec4(Position, 1.0);

    viewDir = normalize((ModelMat * vec4(Position, 1.0)).xyz); // the model is already translated relative to camera

    texcoord = vec2((length(Position.xz) - 0.5) * 2, 0.5);

    // Get the rotation matrices
    mat3 rotModel = mat3(ModelMat);
    // Normal is transformed from Model -> Universe (rotModel)
    normalUniverseSpace = normalize(rotModel * Normal);
}
