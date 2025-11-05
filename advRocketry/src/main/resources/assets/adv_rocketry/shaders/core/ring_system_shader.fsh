#version 150

uniform sampler2D Sampler0; // surface texture

#define MAX_LIGHTS 4
uniform vec4 LightColors[MAX_LIGHTS]; // r,g,b + intensity
uniform vec3 LightVectors[MAX_LIGHTS];
uniform int LightCount;


in vec2 texcoord;
in vec3 viewDir;
in vec3 normalUniverseSpace;

out vec4 fragColor;

//TODO: maybe render rings on planets use a sphere with dot(normal, ringnormal) and pow to render rings on local planet - but monitor fps and use low framebuffer size

void main() {

    float specularStrength = 1;
    float specularPower = 10;

    float alphaMultiplier = 0.1;


    //vec4 baseColor = pow(texture(Sampler0, texcoord), vec4(2.2));
    vec4 baseColor = texture(Sampler0, texcoord);

    vec3 baseColorLinRGB = pow(baseColor.rgb, vec3(2.2));

    vec3 normalUniverseSpaceAdjusted = gl_FrontFacing ? normalUniverseSpace : -normalUniverseSpace;

    vec3 totalColor = vec3(0,0,0);

    for (int i = 0; i < LightCount; i++) {
        vec3 L = normalize(LightVectors[i]);
        vec3 C = LightColors[i].rgb * LightColors[i].a;
        float distance = length(LightVectors[i]);
        vec3 C1 = C  / (distance*distance);

        // specular - bright when starlight reflects into my view
        vec3 reflected = reflect(viewDir, normalUniverseSpaceAdjusted); // the reflection vector
        float reflectionMultiplier = pow(max(0,dot(reflected, L)), specularPower);
        totalColor += specularStrength * reflectionMultiplier * C1;

        // diffuse - brignt when face is facing the star
        float diffuse = max(0,dot(L, normalUniverseSpaceAdjusted))*0.9+0.1;
        totalColor+= diffuse * C1 * baseColorLinRGB;

    }

    vec4 normalColor = vec4(totalColor, baseColor.a * alphaMultiplier);
    //vec4 normalColor = vec4(totalColor, 1);

    fragColor = normalColor;

}