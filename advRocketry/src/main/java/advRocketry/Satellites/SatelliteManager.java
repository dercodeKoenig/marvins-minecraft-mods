package advRocketry.Satellites;

import java.util.HashMap;
import java.util.UUID;

// unlike DimensionManager, this should only exist server side
public class SatelliteManager {
    public static SatelliteManager INSTANCE;

    public HashMap<UUID, Satellite> satellites = new HashMap<>();

    public void serverTick(){

    }

    public void onServerStart(){

    }

    public void onServerStop(){

    }
}
