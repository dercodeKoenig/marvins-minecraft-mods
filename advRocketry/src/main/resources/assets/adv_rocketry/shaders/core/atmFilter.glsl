vec3 getAtmFilter(
    float planetSkyHeight,
    float playerHeight,
    vec3 localUpUniverseSpace,
    vec3 viewDir,
    float LocalAtmDensity,
    vec3 LocalSunriseColor
) {

    // atmosphere modifies how the planet appears
    float altitudeAtmThicknessMod = clamp((planetSkyHeight - playerHeight) / planetSkyHeight, 0, 1);
    float planetUp = dot(localUpUniverseSpace, viewDir);
    float atmThicknessMod = LocalAtmDensity / (1.0 + LocalAtmDensity);
    atmThicknessMod *= pow(1.0 - max(0.0, planetUp), 2.0) * 0.9 + 0.1;
    atmThicknessMod *= altitudeAtmThicknessMod;


    // 1. Normalize the sunrise color so it acts as our base transmittance target
    float maxSunriseColor = max(LocalSunriseColor.r, max(LocalSunriseColor.g, LocalSunriseColor.b));
    vec3 sunRiseTintNormalized = LocalSunriseColor / max(maxSunriseColor, 0.0001);

    // 2. Calculate what colors the atmosphere is absorbing (inverting the sunrise color)
    // If the sunrise is red, the atmosphere is absorbing green and blue.
    vec3 absorptionCoefficients = vec3(1.0) - clamp(sunRiseTintNormalized, 0.0, 1.0);

    // 3. Apply Beer's Law for atmospheric transmittance.
    // You can increase the 'extinctionIntensity' to make the effect stronger
    float extinctionIntensity = 4.0;
    vec3 atmFilter = exp(-extinctionIntensity * atmThicknessMod * absorptionCoefficients);

    return atmFilter;
}