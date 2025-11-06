#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

uniform mat4 WorldMat; // rotate universe space to world space


uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + ChunkOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);

    vec4 lightMapColor = 2*pow(minecraft_sample_lightmap(Sampler2, UV2), vec4(3)) * pow(Color, vec4(2.2));


    //   rotWorldInv transforms vectors from World space to Universe space
    // terrain model is not rotated so model normal is already world space
    mat3 rotWorldInv = transpose(mat3(WorldMat));
    vec3 normalUniverseSpace = normalize(rotWorldInv * Normal).xyz;
    vec3 upUniverseSpace = normalize(rotWorldInv * vec3(1, 1, 1)).xyz;

    vec3 vertexColor3 = vec3(0,0, 0);

    for (int i = 0; i < LightCount; i++) {
        float distance = length(LightVectors[i]);
        vec3 L = normalize(LightVectors[i]);
        vec3 C = LightColors[i].rgb;

        vertexColor3 += lightMapColor.xyz
                        * C
                        * LightColors[i].a / ( distance*distance)
                        * pow(max(0, 0.6 + 0.5* dot(L, normalUniverseSpace)), 1)
                        * max(0,dot(L, upUniverseSpace))
;
        }

    vertexColor = vec4(vertexColor3, lightMapColor.a);


    texCoord0 = UV0;
}
