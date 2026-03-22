#version 150

uniform sampler2D frame;
uniform float threshold;
in vec2 texCoord;
out vec4 fragColor;

uniform ivec2 resolution;

// Helper to prevent flickering on high-intensity HDR pixels
float KarisWeight(vec3 c) {
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return 1.0 / (1.0 + luma);
}

void main() {
    vec2 uv = texCoord;
    vec2 d = 1 / vec2(resolution);

    // 1. Get the 4 raw colors
    vec3 c1 = texture(frame, uv + vec2(-d.x, -d.y)).rgb;
    vec3 c2 = texture(frame, uv + vec2( d.x, -d.y)).rgb;
    vec3 c3 = texture(frame, uv + vec2(-d.x,  d.y)).rgb;
    vec3 c4 = texture(frame, uv + vec2( d.x,  d.y)).rgb;

    // 2. A small helper to do your extraction logic per-pixel
    // (Defining it as a function or just doing it 4 times)
    #define EXTRACT(col) col * (max(0.0, dot(col, vec3(0.2126, 0.7152, 0.0722)) - threshold) / max(dot(col, vec3(0.2126, 0.7152, 0.0722)), 0.0001))

    vec3 s1 = EXTRACT(c1);
    vec3 s2 = EXTRACT(c2);
    vec3 s3 = EXTRACT(c3);
    vec3 s4 = EXTRACT(c4);

    // 3. Apply Karis weights to the EXTRACTED values
    float w1 = KarisWeight(s1);
    float w2 = KarisWeight(s2);
    float w3 = KarisWeight(s3);
    float w4 = KarisWeight(s4);

    // 4. Final Average
    vec3 finalBloom = (s1*w1 + s2*w2 + s3*w3 + s4*w4) / (w1 + w2 + w3 + w4);

    fragColor = vec4(finalBloom, 1.0);
}