package advRocketry.Items;

import ARLib.utils.RecipePartWithProbability;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.GlobalTime;
import advRocketry.Main;
import advRocketry.Utils.ItemUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class ItemAsteroidIdChip extends Item {

    public static final String saveFile = "AsteroidConfig.json";

    public static HashMap<String, Asteroid> asteroids;

    public ItemAsteroidIdChip() {
        super(new Properties());
    }

    public static void setSelectedType(String id, ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        if(id != null)
            tag.putString("id", id);
        ItemUtils.setTag(stack, tag);
    }

    public static String getSelectedType(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains("id"))
            return tag.getString("id");
        else return null;
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String selected = getSelectedType(stack);
        if (selected != null) {
            if(asteroids.containsKey(selected)) {
                for (String i : asteroids.get(selected).description.split("\n")) {
                    tooltipComponents.add(
                            Component.literal(i).withStyle(ChatFormatting.GRAY)
                    );
                }
            }
        }
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
            Type type = new TypeToken<ArrayList<Asteroid>>() {}.getType();
            ArrayList<Asteroid> asteroidsList = new Gson().fromJson(definition, type);
            for (Asteroid i : asteroidsList) {
                asteroids.put(i.id, i);
            }
            System.out.println("[AsteroidSystem] loaded " + asteroids.size() + " asteroids");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a random asteroid based on weighted spawn probabilities.
     * @param random A random instance (can be java.util.Random or net.minecraft.util.RandomSource)
     * @return A randomly selected Asteroid, or null if none are loaded.
     */
    public static Asteroid getRandomAsteroid(Random random) {
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

    public static class Asteroid {
        public String id = "";
        public String description = "";
        public double spawnProbability = 1.0; // The relative weight for spawning
        public List<RecipePartWithProbability> loot = new ArrayList<>();
    }
}