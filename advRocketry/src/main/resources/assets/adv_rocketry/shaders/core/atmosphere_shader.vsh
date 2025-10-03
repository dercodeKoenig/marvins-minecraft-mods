#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 skyViewMat;
uniform vec3 StarDir0;

out vec3 normalViewSpace;
out vec3 upViewSpace;

out vec3 StarDir0ViewSpace;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    mat3 normalMatrix = transpose(inverse(mat3(ModelViewMat)));
    normalViewSpace = normalize(normalMatrix * Normal);

    upViewSpace = normalize((ModelViewMat * vec4(0,1,0,0)).xyz);

    StarDir0ViewSpace = normalize((skyViewMat * vec4(StarDir0.xyz,0)).xyz);

}