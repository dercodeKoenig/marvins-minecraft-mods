#version 150

in vec3 Position;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 skyViewMat;
uniform mat4 ProjMat;

// Light arrays
#define MAX_LIGHTS 4
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount; // how many lights are actually in use
uniform vec3 TargetVector;
uniform vec3 LocalUpUniverseSpace;

out vec2 texcoord;
out vec3 normalViewSpace;
out vec3 LightVectors_ViewSpace[MAX_LIGHTS];
out vec3 upViewSpace;
out vec3 TargetVectorViewSpace;

void main() {
    gl_Position = ProjMat * skyViewMat * ModelViewMat * vec4(Position, 1.0);

    // Normal matrix (for transforming normals into view space)
    mat3 normalMatrix = transpose(inverse(mat3(skyViewMat * ModelViewMat)));
    normalViewSpace = normalize(normalMatrix * Normal);

    // Transform each light vector into view space
    for (int i = 0; i < LightCount; i++) {
        LightVectors_ViewSpace[i] = (skyViewMat * vec4(LightVectors[i], 0.0)).xyz;
    }

    // transform the target vector to view space
    TargetVectorViewSpace = (skyViewMat * vec4(TargetVector, 0.0)).xyz;

    // up to view space
    upViewSpace = normalize((skyViewMat * vec4(LocalUpUniverseSpace,0)).xyz);

    texcoord = UV0;
}
