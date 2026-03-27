// normalize color by max value if any value is > 1
vec3 maxNormC(vec3 color){
    float maxCol = max(color.r, max(color.g, color.b));
    return color / max(1, maxCol);
}

float smoothStep(float x){
    return x * x * x * (x * (x * 6 - 15) + 10);
}