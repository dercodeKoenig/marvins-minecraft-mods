package advRocketry;

public class Config {
    public static Config INSTANCE= new Config();
    public double planetSkyHeight = 5000;
    public double rocketSpaceTravelAcceleration = 0.000001;
    public double rocketSpaceTravelRotationRate = 0.02;
    public double rocketPlanetEntrySpeedY = -10;

    // true scale is way too small, for example earth would only cover 8px on a 1080p screen.
    // solution: artificially scale up planet size for rendering
    public double planetRenderScaleMultiplier = 10;
}
