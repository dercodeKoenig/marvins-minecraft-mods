package advRocketry;

import ARLib.network.SimpleNetworkPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static final String PACKET_ID_SYNC = "adv_rocketry_server_config_sync";

    public static Config INSTANCE = loadConfig();

    // ---- planet / atmosphere physics ----
    public double planet_Sky_Height = 5000;

    // how much adding / removing 1000mb(1 bucket) of liquid should impact atmosphere composition
    // default: 50000 buckets of fluid modify the composition by 1% of earth atmosphere
    //          so you would require to remove 100 * 50k buckets to fully drain a gas
    //          if it has a presence of 1 in atmosphere
    public double fluid_Contribution_To_Composition_Per_1000MB = 0.01 / 50000;

    // same as fluid contribution, but i give blocks more weight
    public double solid_Contribution_To_Composition_Per_Block = 0.01 / 10000;

    // how much gas should evaporate or freeze per tick when temperature falls / rises above the threshold
    // default: 100 seconds for a difference of 0.01
    public double gas_Atm_Transition_Speed = 0.01 / 20 / 100;

    // how fast a planet can change its temperature
    //  thermalMass = 1.0 + (oceanFraction * 10) + (getGravitationalMultiplier() * 100) * planet_Heat_Capacity_Multiplier;
    public double planet_Heat_Capacity_Multiplier = 1;

    // co2 is removed from atmosphere as diff * planet_Sea_Lvl_Co2_Reduction_Factor
    // every tick, a fraction of 1 / 50k to the target is closed
    public double planet_Sea_Lvl_Co2_Reduction_Factor = (double) 1 / 50000;

    // how fast co2 is consumed and turned into o2 during photosynthesis
    // in ideal conditions, it will transfer a total of 1 * planet_Photosynthesis_Factor every tick
    public double planet_Photosynthesis_Factor = (double) 1 / 100_000_000;

    // ---- rocket space travel ----
    public double rocket_SpaceTravel_AU_Per_Second = 0.01;
    public double rocket_SpaceTravel_Min_Speed = 0.000002;
    public double rocket_SpaceTravel_Rotation_Rate = 0.02;
    public double rocket_Planet_Entry_Speed_Y = -5;
    public int rocket_Engine_Boot_Ticks = 100;

    // ---- rocket mass / weight ----
    public float rocket_Block_Weight = 3;
    public float rocket_ItemStack_Weight = 10;
    public float rocket_Fluid_Weight_Per_MB = 0.0005f;

    // ---- space station travel ----
    public double station_SpaceTravel_AU_Per_Second = 10000;
    public double station_SpaceTravel_Min_Speed = 0.000001;
    public double station_SpaceTravel_Rotation_Rate = 0.01;
    public double station_Max_Orbit_R_Factor = 10;

    // ---- survival / equipment consumption ----
    public int jetpack_hydrogen_per_tick = 10;
    public int jetpack_oxygen_per_tick = 5;

    // how much oxygen / nitrogen the oxygen vent consumes per tick while it is running to keep the life support system active
    // the vent only runs while it has a redstone signal and enough of the matching fluid
    public int oxygen_vent_Oxygen_per_tick = 5;
    public int oxygen_vent_Nitrogen_per_tick = 5;

    // how much energy the co2 scrubber consumes per tick while it is running
    public int co2_scrubber_energy_per_tick = 30;

    // fraction (0..1) of a neighboring oxygen vent's gas consumption that an active co2 scrubber removes.
    // the scrubber applies these discounts to oxygen and nitrogen respectively (95% / 98% by default)
    public float co2_scrubber_oxygen_reduction = 0.95f;
    public float co2_scrubber_nitrogen_reduction = 0.98f;

    // ---- observatory ----
    // TODO: rework with tick probability, higher probability if in space staion and if has data
    //      observatory should only work when sky is not blocked
    public double observatory_Find_Planet_P_Per_Tick = (double) 1 / 20 / 500; // 500s average
    public double observatory_Find_Asteroid_P_Per_Data = (double) 1 / 300;  // after 300 data average
    public int observatory_Energy_Per_Tick = 10;

    public int astrobody_Data_Processor_Energy_Per_Tick = 100;

    // ---- satellites ----
    public double satellite_Radiation_Damage_Prob_Per_Second = (double) 1 / 10000;

    // ---- rocket / space station assembler ----
    public int rocket_Assembler_Max_Size = 98;
    public int rocket_Assembler_Build_Time_Base = 12;
    public int rocket_Assembler_Energy_Per_Tick = 100;

    // ---- fueling / loaders ----
    public int fueling_Station_Energy_Per_Tick = 200;
    public int fueling_Station_Fuel_Per_Tick = 50;
    public int item_Loader_Energy_Per_Tick = 100;
    public int fluid_Loader_Energy_Per_Tick = 100;

    // ---- laser drill ----
    // how much energy the laser drill consumes per tick while actively drilling
    // the drill will only run while it has a redstone signal and at least this much energy stored
    public int laserDrill_Energy_Per_Tick = 10;

    // ---- render scale ----
    // used both for rendering and by navigation calculations (e.g. entry distance), so it ships in
    // the server config (synced) rather than the client-only config
    // true scale is way too small, for example moon would only cover 8px on a 1080p screen.
    // solution: artificially scale up planet size for rendering
    public double planet_Render_Scale_Multiplier = 8;

    @Override
    public void readClient(String data) {
        INSTANCE = new Gson().fromJson(data, Config.class);
        System.out.println("AdvRocketry config was received by client");
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(PACKET_ID_SYNC, new Gson().toJson(INSTANCE)));
        }
    }

    public static Config loadConfig() {
        Path configDir = Path.of(FMLPaths.CONFIGDIR.get().toString(), Main.MODID);
        Path filePath = configDir.resolve("config.json");
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, new GsonBuilder().setPrettyPrinting().create().toJson(new Config()));
            }
            String jsonContent = Files.readString(filePath);
            return new Gson().fromJson(jsonContent, Config.class);
        } catch (JsonSyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
