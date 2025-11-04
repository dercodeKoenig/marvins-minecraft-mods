#version 150

uniform sampler2D Sampler0; // surface texture

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;

in vec3 normalUniverseSpace;
in vec2 texcoord;
in vec3 normalModelSpace;

out vec4 fragColor;

//TODO: to render rings on planets use a sphere with dot(normal, ringnormal) and pow to render rings on local planet - but monitor fps and use low framebuffer size

void main() {

    vec4 baseColor = pow(texture(Sampler0, texcoord), vec4(2.2));


    vec3 totalColor = vec3(0,0,0);

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        vec4 C = LightColors[i];

        float d = max(0,(dot(normalUniverseSpace, L)+1) / 2);
        totalColor+=d*C.rgb*baseColor.rgb;

    }

    vec4 normalColor = vec4(totalColor, baseColor.a);

    fragColor = normalColor;

}