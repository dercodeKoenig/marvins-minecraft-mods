#version 150

uniform sampler2D Sampler0;
uniform vec3 Light0_Direction;

uniform float reflectivity;
uniform vec4 emissiveColor;


in vec2 texCoord0;
in vec3 rotatedNormal;
out vec4 fragColor;

void main() {
    // Calculate raw illumination factor from reflection:
    float reflectedFactor = max(0,dot(rotatedNormal, Light0_Direction) * reflectivity);
    // Get the base planet color (texture/vertex color):
    vec3 baseSurfaceColor = (texture(Sampler0, texCoord0)).rgb;
    // use alpha for intensity
    vec4 reflectedLight = vec4(baseSurfaceColor,  reflectedFactor);


    // Emitted light is constant across the surface (or based on a separate texture/map)
    // We can multiply the emissive value by the surface color to color the emission.
    vec4 emittedLight = vec4(emissiveColor.rgb * baseSurfaceColor.rgb, emissiveColor.a);




    fragColor = reflectedLight + emittedLight;
}