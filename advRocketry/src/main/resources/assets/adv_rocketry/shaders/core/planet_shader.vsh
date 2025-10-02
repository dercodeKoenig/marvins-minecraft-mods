#version 150

in vec3 Position;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 skyViewMat;
uniform mat4 ProjMat;

uniform vec3 Light0_Vector;

out vec2 texcoord;

out vec3 normalViewSpace;
out vec3 Light0_Vector_ViewSpace;

vec4 mix_light(vec3 lightDir0, vec3 normal, vec4 color) {
    float light0 = max(0.0, dot(lightDir0, normal));
    return vec4(color.rgb * light0, color.a);
}

void main() {
    gl_Position = ProjMat * skyViewMat * ModelViewMat * vec4(Position, 1.0);

    // Compute the normal matrix (upper-left 3x3 inverse transpose)
    mat3 normalMatrix = transpose(inverse(mat3(skyViewMat * ModelViewMat)));
    normalViewSpace = normalize(normalMatrix * Normal);

    Light0_Vector_ViewSpace = (skyViewMat * vec4(Light0_Vector, 0.0)).xyz;

    texcoord = UV0;
}
