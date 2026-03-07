package advRocketry.Satellites;

import com.mojang.datafixers.util.Pair;

public interface SatellitePrimaryFunction {
    // creates & returns a subclass of whatever specific satellite type
    // return null if invalid
    // second value used for success / error message
    Pair<Satellite,  Pair<Boolean, String>> build(Satellite satellite);
}
