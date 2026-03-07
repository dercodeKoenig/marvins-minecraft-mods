package advRocketry.Satellites;

public interface SatellitePrimaryFunction {
    // creates & returns a subclass of whatever specific satellite type
    // return null if invalid
    Satellite build(Satellite satellite);
}
