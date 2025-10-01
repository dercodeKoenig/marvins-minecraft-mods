#version 150

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec3 Light0_Direction;

out vec4 vertexColor;
out vec2 texCoord0;

out vec3 rotatedNormal;

vec4 mix_light(vec3 lightDir0, vec3 normal, vec4 color) {
    float light0 = max(0.0, dot(lightDir0, normal));
    return vec4(color.rgb * light0, color.a);
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // Compute the normal matrix (upper-left 3x3 inverse transpose)
    mat3 normalMatrix = transpose(inverse(mat3(ModelViewMat)));
    rotatedNormal = normalize(normalMatrix * Normal);

    vertexColor = mix_light(Light0_Direction, rotatedNormal, vec4(1,1,1,1));
    texCoord0 = UV0;
}
