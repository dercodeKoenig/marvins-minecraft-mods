#version 150

in vec3 vertexDirUniverseSpace;
in vec3 localUpUniverseSpace;

uniform float time;
uniform float intensity;

out vec4 fragColor;

#moj_import "adv_rocketry:noise.glsl"

float getWarpNoise(vec3 p, float frequency, float time) {
// 1. Create two continuous flowing directions.
// Tweak these vectors to change how the energy washes over the ship.
vec3 offset1 = vec3(time * 0.3, time * 0.15, time * 0.2);
vec3 offset2 = vec3(-time * 0.2, time * 0.1, -time * 0.25);

// 2. Sample the continuously moving noise layers
float n1 = cnoise(p * frequency + offset1);
float n2 = cnoise(p * frequency + offset2);

// 3. Map the noise from [-1.0, 1.0] to [0.0, 1.0]
n1 = n1 * 0.5 + 0.5;
n2 = n2 * 0.5 + 0.5;

// 4. Interference Multiplication!
// Multiplying two decimals (< 1.0) makes the output darker, creating pockets.
// The * 2.0 just recovers some of that lost brightness.
return n1 * n2 * 2.0;
}

void main() {
vec3 V = normalize(vertexDirUniverseSpace);

float noise = 0.0;
float amp = 0.8;
float freq = 2.0;

for (int i = 0; i < 5; i++) {
    float noiseVal = getWarpNoise(V, freq, time);
    noise += noiseVal * amp;
    amp *= 0.5;
    freq *= 2.0;
}

// Your contrast curve - this is what makes it look like sharp energy!
noise = pow(max(0, noise), 5.0);
noise *= 0.5 * intensity;

// Optional: Tint it a warp color like blue/purple here instead of white
fragColor = vec4(noise * 0.2, noise * 0.5, noise * 1.5, 1.0);
}