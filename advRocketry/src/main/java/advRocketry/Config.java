package advRocketry;

public class Config {
    public static Config INSTANCE = new Config();

    public double planet_Sky_Height = 5000;

    // choose the particle render mode
    // true: render particles delayed with custom renderer, not compatible with iris shaders
    // false: have minecraft builtin particle engine render, use simple dust particles
    public boolean use_Transparent_Particle_Engine = true;

    public double rocket_SpaceTravel_AU_Per_Second = 0.01;
    public double rocket_SpaceTravel_Distance_For_Max_Speed = 0.1;
    public double rocket_SpaceTravel_Min_Speed = 0.000002;
    public double rocket_SpaceTravel_Rotation_Rate = 0.02;
    public double rocket_Planet_Entry_Speed_Y = -5;
    public int rocket_Engine_Boot_Ticks = 100;

    public float rocket_Block_Weight = 3;
    public float rocket_ItemStack_Weight = 10;
    public float rocket_Fuel_Weight_Per_MB = 0.0005f;

    public double station_SpaceTravel_AU_Per_Second = 1;
    public double station_SpaceTravel_Distance_For_Max_Speed = 10;
    public double station_SpaceTravel_Min_Speed = 0.000002;
    public double station_SpaceTravel_Rotation_Rate = 0.005;
    public double station_Max_Orbit_R_Factor = 10;

    // true scale is way too small, for example earth would only cover 8px on a 1080p screen.
    // solution: artificially scale up planet size for rendering
    public double planet_Render_Scale_Multiplier = 10;


    // TODO: rework with tick probability, higher probability if in space staion and if has data
    //      observatory should only work when sky is not blocked
    public double observatory_Find_Planet_P_Per_Tick = (double) 1 / 1000;
    public double observatory_Find_Asteroid_P_Per_Tick = (double) 1 / 100;
    public int observatory_Energy_Per_Tick = 10;

    public int astrobody_Data_Processor_Energy_Per_Tick = 100;

    public double satellite_Radiation_Damage_Prob_Per_Second = (double) 1 / 10000;

    public int rocket_Assembler_Max_Size = 66;
    public int rocket_Assembler_Build_Time_Base = 12;
    public int rocket_Assembler_Energy_Per_Tick = 100;

    public int fueling_Station_Energy_Per_Tick = 200;
    public int fueling_Station_Fuel_Per_Tick = 50;

    public int item_Loader_Energy_Per_Tick = 100;



    // how much adding / removing 1000mb(1 bucket) of liquid should impact atmosphere composition
    // default: 10000 buckets of fluid modify the composition by 1% of earth atmosphere
    //          so you would require to remove 100 * 10k buckets to fully drain a gas
    //          if it ha sa presence of 1 in atmosphere
    public double fluid_Contribution_To_Composition_Per_1000MB = 0.01 / 10000;

    // how much gas should evaporate or freeze per tick when temperature falls / rises above the threshold
    // default: 100 seconds for a difference of 0.01
    public double gas_Atm_Ground_Transition_Speed = 0.01 / 20 / 100;

    // how fast a planet can change its temperature
    //  thermalMass = 1.0 + (oceanFraction * 10) + (getGravitationalMultiplier() * 100) * planet_Heat_Capacity_Multiplier;
    public double planet_Heat_Capacity_Multiplier = 1;

    // co2 is removed from atmosphere as diff * planet_Sea_Lvl_Co2_Reduction_Factor
    // every tick, a fraction of 1 / 50k to the target is closed
    public double planet_Sea_Lvl_Co2_Reduction_Factor = (double) 1 / 50000;

    // how fast co2 is consumed and turned into o2 during photosynthesis
    // in ideal conditions, it will transfer a total of 1 * planet_Photosynthesis_Factor every tick
    public double planet_Photosynthesis_Factor = (double) 1 / 2000000;

}
