#version 150

in vec3 Position;
in vec3 Normal;

uniform float scale;
uniform mat4 ViewMat;
uniform mat4 WorldMat;  // rotate universe space to world space
uniform mat4 ModelMat; // Model space to universe space (planets are transformed in universe space)
uniform mat4 ProjMat;
uniform vec3 playerEye;

out vec2 texcoord;
out vec3 normalUniverseSpace;
out vec3 viewDir; // the direction of the fragment relative to the camera for light calculations

out vec3 position; // the position of the fragment
out vec3 planetCenter; // the position of the planet for shadow approximation

out vec3 localUpUniverseSpace; // for shading with local atmosphere

void main() {

    vec3 scaledPosition = Position * scale;

    gl_Position = ProjMat * ViewMat * WorldMat * ModelMat * vec4(scaledPosition, 1.0);

    position = (ModelMat * vec4(scaledPosition, 1.0)).xyz;

    viewDir = normalize(position-playerEye);

    planetCenter = (ModelMat * vec4(0,0,0,1)).xyz;

    texcoord = vec2((length(Position.xz) - 0.5) * 2, 0.5);

    // Get the rotation matrices
    // Normal is transformed from Model -> Universe (rotModel)
    mat3 normalMatrix = transpose(inverse(mat3(ModelMat)));
    normalUniverseSpace = normalize(normalMatrix * Normal);

    mat3 rotWorldInv = transpose(mat3(WorldMat));
    localUpUniverseSpace = normalize(rotWorldInv * vec3(0,1,0));
}
