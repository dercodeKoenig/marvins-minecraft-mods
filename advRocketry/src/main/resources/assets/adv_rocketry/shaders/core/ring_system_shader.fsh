#version 150

uniform sampler2D Sampler0; // surface texture

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;


in vec2 texcoord;
in vec3 universePosition;
in vec3 normalUniverseSpace;

out vec4 fragColor;

//TODO: maybe render rings on planets use a sphere with dot(normal, ringnormal) and pow to render rings on local planet - but monitor fps and use low framebuffer size

void main() {

    //vec4 baseColor = pow(texture(Sampler0, texcoord), vec4(2.2));
    vec4 baseColor = texture(Sampler0, texcoord);

    vec3 baseColorLinRGB = pow(baseColor.rgb, vec3(2.2));

    vec3 normalUniverseSpaceAdjusted = gl_FrontFacing ? normalUniverseSpace : -normalUniverseSpace;

    vec3 totalColor = vec3(0,0,0);

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        vec3 C = LightColors[i].rgb * LightColors[i].a;
        float distance = length(LightVectors[i]);

        // specular
        vec3 reflected = reflect(normalize(universePosition), normalUniverseSpaceAdjusted); // the reflection vector
        float reflectionMultiplier = pow(max(0,dot(reflected, L)), 5);
        totalColor += reflectionMultiplier * C;

        // diffuse
        totalColor+= 0.5* (dot(L, normalUniverseSpaceAdjusted)+1)/2 * C * baseColorLinRGB / (distance*distance);

    }

    vec4 normalColor = vec4(totalColor, baseColor.a);
    //vec4 normalColor = vec4(totalColor, 1);

    fragColor = normalColor;

}