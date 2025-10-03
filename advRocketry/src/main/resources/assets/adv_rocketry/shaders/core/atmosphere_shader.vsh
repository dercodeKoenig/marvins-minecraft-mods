#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 skyViewMat;

out vec3 normalViewSpace;
out vec3 upViewSpace;

// Light arrays
#define MAX_LIGHTS 4
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

out vec3 LightVectors_ViewSpace[MAX_LIGHTS];


void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    mat3 normalMatrix = transpose(inverse(mat3(ModelViewMat)));
    normalViewSpace = normalize(normalMatrix * Normal);

    upViewSpace = normalize((ModelViewMat * vec4(0,1,0,0)).xyz);

    // Transform each light vector into view space
    for (int i = 0; i < LightCount; i++) {
        LightVectors_ViewSpace[i] = (skyViewMat * vec4(LightVectors[i], 0.0)).xyz;
    }


}