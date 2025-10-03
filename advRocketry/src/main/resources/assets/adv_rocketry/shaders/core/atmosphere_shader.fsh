#version 150

uniform sampler2D planetTexture; // HDR planets/stars texture

uniform vec3 SkyColor;
uniform vec3 SunriseColor;
uniform vec3 FogColor;
uniform int screenWidth;
uniform int screenHeight;
uniform float playerHeight;
uniform float renderDistance;

in vec3 normalViewSpace;
in vec3 upViewSpace;

in vec3 StarDir0ViewSpace;

out vec4 fragColor;

void main() {

    /////////// toneMap the rendered planets / stars
    vec2 uv = gl_FragCoord.xy / vec2(screenWidth, screenHeight);
    vec4 planetColorHDR = texture(planetTexture, uv);
    vec4 planetColorLDR = planetColorHDR / (1.0 + planetColorHDR);
    planetColorLDR = pow(planetColorLDR, vec4(1.0 / 2.2));





    ///////////// sunrise / sunset calculations
    // how much is the fragment at the horizon
    float upDot = dot(normalViewSpace, upViewSpace);
    float horizonFactor = pow(1-abs(upDot), 3);

    // how much is the sun aligned with the fragments normal, note that the skybox normals point towards inside
    float sunDot = -dot(StarDir0ViewSpace, normalViewSpace);
    sunDot = (sunDot+1) / 2;

    vec3 targetSkyColor = SkyColor.xyz;
    targetSkyColor = mix(targetSkyColor, SunriseColor, sunDot * horizonFactor);





    //////////// blend with terrain fog
    // 1. Get the base vertical factor
    float verticalDot = dot(normalize(upViewSpace), -normalize(normalViewSpace));

    // 2. Calculate the horizon dip based on planet curvature
    // This is the more precise formula.
    float h = playerHeight;
    float R = renderDistance;
    float R_plus_h = R + h;

    float horizonDip = sqrt(2.0 * R * h + h * h) / R_plus_h;

    // 3. Remap the vertical factor to account for the new horizon
    // The logic remains the same: shift and rescale the range.
    float fogFactor = (verticalDot + horizonDip) / (1.0 + horizonDip);
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    // 4. Apply the artistic curve
    fogFactor = pow(fogFactor, 1.0); // Adjust exponent for feel

    // Use this final fogFactor to mix your colors
    targetSkyColor = mix(FogColor, targetSkyColor, fogFactor);


    // adjust for player height, black it out when player is very high
    targetSkyColor = targetSkyColor * max(0,(10000-playerHeight) / 10000);


    fragColor = vec4(targetSkyColor,1) + planetColorLDR;
    //fragColor = vec4(horizonTint, 1)*0.0001 + vec4(sunsetFactor) + planetColorLDR*0.0001;
}