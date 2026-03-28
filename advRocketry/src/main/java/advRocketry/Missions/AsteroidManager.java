package advRocketry.Missions;

import ARLib.utils.RecipePartWithProbability;
import advRocketry.GlobalTime;
import advRocketry.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AsteroidManager {

    public static final int timeout = 20 * 60 * 60 * 48; // discovered asteroids are no longer valid after this many ticks game time

    public static final String saveFile = "AsteroidConfig.json";
    public static final String saveFileDiscoveredAsteroids = "adv_rocketry_discovered_asteroids.json";

    // holds general asteroid definitions
    private static HashMap<String, Asteroid> asteroids;
    // discovered asteroids will be saved here so they can be mined
    private static HashMap<String, DiscoveredAsteroid> discoveredAsteroids;

    public static Asteroid getAsteroid(DiscoveredAsteroid discoveredAsteroid) {
        if (discoveredAsteroid != null)
            return asteroids.get(discoveredAsteroid.asteroidId);
        else return null;
    }

    public static DiscoveredAsteroid getDiscoveredAsteroid(String key) {
        if (key != null) {
            return discoveredAsteroids.get(key);
        } else return null;
    }

    public static ArrayList<Asteroid> makeDefaultDefinition() {
        ArrayList<Asteroid> list = new ArrayList<>();
        Asteroid a = new Asteroid();
        a.id = "iron_asteroid";
        a.description = "iron enriched asteroid\norigin: somewhere far away";
        a.spawnProbability = 50.0; // Higher weight = more common
        a.loot.add(new RecipePartWithProbability("minecraft:iron_ore", 100, 0.5f));
        list.add(a);

        Asteroid b = new Asteroid();
        b.id = "diamond_asteroid";
        b.description = "rare diamond asteroid";
        b.spawnProbability = 5.0; // Lower weight = rarer
        b.loot.add(new RecipePartWithProbability("minecraft:diamond_ore", 100, 0.1f));
        list.add(b);

        return list;
    }

    public static void onServerStart() {
        // load asteroid definitions
        asteroids = new HashMap<>();
        Path savePath = Path.of(String.valueOf(Main.myConfigDir), saveFile);
        try {
            if (!Files.exists(Main.myConfigDir))
                Files.createDirectories(Main.myConfigDir);
            if (!Files.exists(savePath)) {
                // make a default definition
                String defaultDefinition = new GsonBuilder().setPrettyPrinting().create().toJson(makeDefaultDefinition());
                Files.writeString(savePath, defaultDefinition);
                System.out.println("[AsteroidSystem] created default asteroid definition");
            }
            String definition = Files.readString(savePath);
            Type type = new TypeToken<ArrayList<Asteroid>>() {
            }.getType();
            ArrayList<Asteroid> asteroidsList = new Gson().fromJson(definition, type);
            for (Asteroid i : asteroidsList) {
                asteroids.put(i.id, i);
            }
            System.out.println("[AsteroidSystem] loaded " + asteroids.size() + " asteroids");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // load discovered asteroids
        discoveredAsteroids = new HashMap<>();
        savePath = Path.of(String.valueOf(Main.worldPath), saveFileDiscoveredAsteroids);
        try {
            if (Files.exists(savePath)) {
                String discoveredAsteroidsString = Files.readString(savePath);
                Type type = new TypeToken<ArrayList<DiscoveredAsteroid>>() {
                }.getType();
                ArrayList<DiscoveredAsteroid> discoveredAsteroidsList = new Gson().fromJson(discoveredAsteroidsString, type);
                for (DiscoveredAsteroid i : discoveredAsteroidsList) {
                    discoveredAsteroids.put(i.key, i);
                }
                System.out.println("[AsteroidSystem] restore " + discoveredAsteroids.size() + " discovered asteroids");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void onServerStop() {
        // save discovered asteroids
        Path savePath = Path.of(String.valueOf(Main.worldPath), saveFileDiscoveredAsteroids);
        System.out.println("[AsteroidSystem] saving discovered asteroids");
        try {
            List<DiscoveredAsteroid> discoveredAsteroidList = new ArrayList<>();
            for (DiscoveredAsteroid asteroid : discoveredAsteroids.values()) {
                if (!asteroid.isExpired()) {
                    discoveredAsteroidList.add(asteroid);
                } else {
                    System.out.println("[AsteroidSystem] skip saving for expired asteroid: " + asteroid.key);
                }
            }
            String saveString = new GsonBuilder().setPrettyPrinting().create().toJson(discoveredAsteroidList);
            Files.writeString(savePath, saveString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DiscoveredAsteroid discoverNewAsteroid() {
        Asteroid asteroid = getRandomAsteroid(new Random());
        if (asteroid == null) return null;
        String id = UUID.randomUUID().toString();
        DiscoveredAsteroid discoveredAsteroid = new DiscoveredAsteroid();
        discoveredAsteroid.asteroidId = asteroid.id;
        discoveredAsteroid.key = id;
        discoveredAsteroid.discoverTime = GlobalTime.getGlobalTime();
        discoveredAsteroids.put(id, discoveredAsteroid);
        System.out.println("[AsteroidSystem] discover a new asteroid: " + asteroid.id + " - saved as " + id);
        return discoveredAsteroid;
    }

    public static void invalidateDiscoveredAsteroid(DiscoveredAsteroid discoveredAsteroid) {
        if (discoveredAsteroid == null) return;
        if (discoveredAsteroids.remove(discoveredAsteroid.key) != null)
            System.out.println("[AsteroidSystem] removed discovered asteroid " + discoveredAsteroid.key);
        else
            System.out.println("[AsteroidSystem] could not remove invalid discovered asteroid (not found)" + discoveredAsteroid.key);
    }

    /**
     * Gets a random asteroid based on weighted spawn probabilities.
     *
     * @param random A random instance (can be java.util.Random or net.minecraft.util.RandomSource)
     * @return A randomly selected Asteroid, or null if none are loaded.
     */
    private static Asteroid getRandomAsteroid(Random random) {
        if (asteroids == null || asteroids.isEmpty()) {
            return null;
        }

        double totalSpawnWeight = 0.0;
        for (Asteroid i : asteroids.values()) {
            totalSpawnWeight += i.spawnProbability; // Tally up the total weight
        }

        // Generate a random number between 0.0 and totalSpawnWeight
        double randomValue = random.nextDouble() * totalSpawnWeight;

        // Iterate through all asteroids, subtracting their probability from the random value.
        // Once the value drops below 0, we've found our weighted pick.
        for (Asteroid asteroid : asteroids.values()) {
            randomValue -= asteroid.spawnProbability;
            if (randomValue <= 0.0) {
                return asteroid;
            }
        }

        // Fallback (this should rarely be hit unless there are floating-point precision issues)
        return asteroids.values().iterator().next();
    }

    public static class DiscoveredAsteroid {
        public String asteroidId = "";
        public String key = "";
        long discoverTime = 0;

        public DiscoveredAsteroid() {
        }

        public DiscoveredAsteroid(String key, String asteroidId) {
            this.key = key;
            this.asteroidId = asteroidId;
            this.discoverTime = GlobalTime.getGlobalTime();
        }

        public boolean isExpired() {
            return discoverTime + timeout < GlobalTime.getGlobalTime();
        }
    }

    public static class Asteroid {
        public String id = "";
        public String description = "";
        public double spawnProbability = 1.0; // The relative weight for spawning
        public List<RecipePartWithProbability> loot = new ArrayList<>();
    }
}
