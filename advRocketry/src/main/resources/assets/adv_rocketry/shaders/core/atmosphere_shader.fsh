#version 150

uniform vec4 Color;
uniform vec4 FogColor;
in float vertexHeight;
out vec4 fragColor;

vec4 linear_fog(vec4 inColor, float vertexHeight, vec4 fogColor) {
    float fogValue = 1-vertexHeight;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

void main() {
    fragColor = linear_fog(Color, clamp(vertexHeight, 0,1), FogColor);
    //fragColor = vec4(vertexPosition.x,vertexPosition.y,vertexPosition.z,1);
}
