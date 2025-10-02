#version 150

uniform sampler2D Sampler0; // texture

uniform vec4 Light0_Color; // r,g,b + intensity

uniform float reflectivity; // how much the planet reflects other stars light
uniform vec4 emissiveColor; // the light that this planet or star emits, rgb + intensity

in vec3 Light0_Vector_transformed; // vector star to planet

in vec2 texcoord; // texture
in vec3 rotatedNormal; // adjusted normal


out vec4 fragColor;

void main() {
    vec3 baseSurfaceColor = (texture(Sampler0, texcoord)).rgb;

    float Light0Dot =  max(0,dot(rotatedNormal, Light0_Vector_transformed));
    float Light0Distance = length(Light0_Vector_transformed);
    vec3 reflectedLight0 = Light0Dot * reflectivity * baseSurfaceColor * Light0_Color.rgb * Light0_Color.a / (Light0Distance * Light0Distance);
    vec4 reflectedLight = vec4(reflectedLight0, 1.0);


    // Emitted light is constant across the surface (or based on a separate texture/map)
    // We can multiply the emissive value by the surface color to color the emission.
    vec4 emittedLight = vec4(emissiveColor.rgb * baseSurfaceColor.rgb, emissiveColor.a);

    fragColor = reflectedLight + emittedLight;
}