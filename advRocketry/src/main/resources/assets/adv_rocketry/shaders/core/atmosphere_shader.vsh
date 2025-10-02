#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 normalViewSpace;
out vec3 upViewSpace;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    mat3 normalMatrix = transpose(inverse(mat3(ModelViewMat)));
    normalViewSpace = normalize(normalMatrix * Normal);

    upViewSpace = (ModelViewMat * vec4(0,1,0,0)).xyz;

}