#version 150

uniform sampler2D image;      // The frame of extracted bright regions
uniform int horizontal;      // 1 for horizontal pass, 0 for vertical pass
uniform int resolution;     // Width or height of the texture (for calculating texel size)

in vec2 texCoord;
out vec4 fragColor;

void main()
{
    // The size of a single texel
    float texelSize = 1.0 / resolution;

    float weight[6];
    weight[0] = 0.2270;
    weight[1] = 0.1946;
    weight[2] = 0.1216;
    weight[3] = 0.0541;
    weight[4] = 0.0162;
    weight[5] = 0.0037;

    vec3 result = texture(image, texCoord).rgb * weight[0]; // Start with the center pixel

    // Loop through the 4 steps on either side of the center (9 total samples)
    for(int i = 1; i < 6; ++i)
    {
        if(horizontal == 1)
        {
            // Sample pixels to the left and right
            vec2 offset = vec2(float(i) * texelSize, 0.0);
            result += texture(image, texCoord + offset).rgb * weight[i];
            result += texture(image, texCoord - offset).rgb * weight[i];
        }
        else
        {
            // Sample pixels above and below
            vec2 offset = vec2(0.0, float(i) * texelSize);
            result += texture(image, texCoord + offset).rgb * weight[i];
            result += texture(image, texCoord - offset).rgb * weight[i];
        }
    }

    fragColor = vec4(result, 1.0);
}