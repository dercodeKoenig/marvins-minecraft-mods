#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec4 vColor;

uniform mat4 ViewMat;
uniform mat4 ModelMat;
uniform mat4 WorldMat;
uniform mat4 ProjMat;
uniform float BrightnessModifier;
uniform vec3 WarpMovement; // The velocity vector of your travel
uniform ivec2 ScreenSize;

void main() {
    float BoxSize = 50000;
    float scale = 5;
    float warpScale = 2;

    // 1. Get the star's static center
    vec3 staticCenter = Position - (Normal * scale);

    // 2. Move into Camera Relative Space
    vec3 relativeCenter = (ModelMat * vec4(staticCenter, 1.0)).xyz;

    // 3. Warp the center (Wrap-around logic)
    vec3 wrappedCenter = mod(relativeCenter + (BoxSize * 0.5), BoxSize) - (BoxSize * 0.5);

    float distToCamera = length(wrappedCenter);

    // 4. Calculate Stretch
    vec3 stretchOffset = vec3(0.0);
    float speed = length(WarpMovement);

    if (speed > 0.001) {
        // Normalize
        vec3 dir = WarpMovement / speed;
        // Determine how much this specific corner points toward the movement
        float alignment = dot(Normal, dir);

        // We stretch the vertex along the WarpMovement vector.
        // Vertices in "front" go forward, vertices in "back" stay put or move less.
        // This turns the cube into a long line/needle.
        stretchOffset = WarpMovement * alignment * warpScale;
    }

    // 5. Final Position: Wrapped Center + Original Cube Shape + Warp Stretch
    vec3 finalPos = wrappedCenter + (Normal * scale) + stretchOffset;


    // --- 6. Anti-Flicker (Minimum Size) Logic ---

    // Get the position in clip space to find out how big it is on screen
    vec4 clipPos = ProjMat * ViewMat * WorldMat * vec4(finalPos, 1.0);

    // Approximate the size of 1.2 pixels in clip space
    // clipPos.w is the distance, ScreenSize.y is the vertical resolution
    float minSize = (1.2 / ScreenSize.y) * clipPos.w;

    // Calculate the current "intended" size of the star corner
    float currentSize = scale;

    // If the star is smaller than our minimum, we calculate a scale-up factor
    float sizeFactor = 1.0;
    if (currentSize < minSize) {
        sizeFactor = minSize / currentSize;
    }

    // Scale up the offset from the center
    // We only scale the 'Normal * scale' part, NOT the wrappedCenter
    vec3 antiFlickerOffset = (Normal * scale + stretchOffset) * sizeFactor;
    vec4 finalClipPos = ProjMat * ViewMat * WorldMat * vec4(wrappedCenter + antiFlickerOffset, 1.0);
    float brightnessComp = 1.0 / (sizeFactor * sizeFactor);

    gl_Position = finalClipPos;



    // Color fade in / out / special effects -------------

    // Optional: Boost brightness during warp so the streaks look "energetic"
    vColor = Color * BrightnessModifier * (1.0 + speed * 2);


    // --- Proximity Fade Logic ---

    // Define your ranges:
    float fadeStart = BoxSize * 0.5 * 0.2; // Stars start appearing
    float fadeEnd = BoxSize * 0.5 * 0.25;   // Stars are fully opaque
    float outOfRangeFadeStart = BoxSize * 0.5 * 0.9;
    float outOfRangeFadeEnd = BoxSize * 0.5 * 1;

    // smoothstep returns 0.0 if dist < fadeStart, 1.0 if dist > fadeEnd
    float proximityFade = smoothstep(fadeStart, fadeEnd, distToCamera);
    proximityFade *= 1-smoothstep(outOfRangeFadeStart, outOfRangeFadeEnd, distToCamera);

    // Apply the fade to the alpha channel
    vColor = Color * BrightnessModifier * (1.0 + speed * 0.05) * 1.5;
    vColor *= brightnessComp;
    vColor *= proximityFade; // alpha is not used in this render pipeline, so scale rgb
}