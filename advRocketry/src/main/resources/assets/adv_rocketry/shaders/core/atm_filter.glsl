float getAtmThickness(float relativeHeight, vec3 U, vec3 V, float baseValue){
    float atmThickness = baseValue;
    atmThickness *= pow(1.0 - max(0.0, dot(U, V)), 4.0) * 0.8 + 0.2;
    atmThickness *= relativeHeight;
    return atmThickness;
}

vec3 getAtmFilter(
    float planetSkyHeight,
    float playerHeight,
    vec3 U,
    vec3 V,
    float LocalAtmDensity,
    vec3 LocalSunriseColor
) {

    float relativeHeight = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
    float atmThickness = getAtmThickness(relativeHeight, U, V, LocalAtmDensity);

    // 1. Normalize the sunrise color so it acts as our base transmittance target
    float maxSunriseColor = max(LocalSunriseColor.r, max(LocalSunriseColor.g, LocalSunriseColor.b));
    vec3 sunRiseTintNormalized = LocalSunriseColor / max(maxSunriseColor, 0.0001);

    // 2. Calculate what colors the atmosphere is absorbing (inverting the sunrise color)
    // If the sunrise is red, the atmosphere is absorbing green and blue.
    // 1.x makes some base extinction so even the less-extincted color will be a little extinct
    vec3 absorptionCoefficients = vec3(1.5) - clamp(sunRiseTintNormalized, 0.0, 1.0);

    // 3. Apply Beer's Law for atmospheric transmittance.
    // You can increase the 'extinctionIntensity' to make the effect stronger
    float extinctionIntensity = 1.5;
    vec3 atmFilter = exp(-extinctionIntensity * atmThickness * absorptionCoefficients);

    return vec3(atmFilter);
}