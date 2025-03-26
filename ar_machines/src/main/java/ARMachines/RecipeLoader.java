package ARMachines;

import ARLib.utils.MachineRecipe;
import ARLib.utils.RecipePartWithProbability;
import ARMachines.lathe.LatheConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeLoader {
    public static List<MachineRecipe> loadRecipes(String recipesDirName) {

        Class<MachineRecipe> recipeClass = MachineRecipe.class;
        MachineRecipe recipe;
        System.out.println("load config: " + recipesDirName);
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), ARMachines.MODID);
        Path configRecipesDir = configDir.resolve(recipesDirName);

        try {
            // Create the config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            // Load recipes from the recipesDirName directory
            if (!Files.exists(configRecipesDir)) {
                Files.createDirectories(configRecipesDir);
                System.out.println("Recipes directory created: " + configRecipesDir);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<MachineRecipe> recipes = new ArrayList<>();
        // load recipes from config
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(configRecipesDir, "*.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Path recipeFile : stream) {
            try {
                String recipeContent = Files.readString(recipeFile);
                Gson gson = new Gson();
                recipe = gson.fromJson(recipeContent, recipeClass);
                for (RecipePartWithProbability i : recipe.outputs) {
                    // if no p set, i set it to 1
                    if (i.p == 0) {
                        i.p = 1;
                        System.out.println(recipeFile + " - output with id " + i.id + " has no probability set or it is set to 0. It will default to one.");
                    }
                }
                for (RecipePartWithProbability i : recipe.inputs) {
                    // if no p set, i set it to 1
                    if (i.p == 0) {
                        i.p = 1;
                        System.out.println(recipeFile + " - input with id " + i.id + " has no probability set or it is set to 0. It will default to one.");
                    }
                }
                recipes.add(recipe);
                System.out.println("Loaded recipe: " + recipeFile.getFileName());
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse JSON, skipping recipe file: " + recipeFile.getFileName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return recipes;
    }


    public static void saveRecipes(String recipesDirName, List<MachineRecipe> recipes) {
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), ARMachines.MODID);
        Path configRecipesDir = configDir.resolve(recipesDirName);

        try {

            Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.PROTECTED).create(); // Pretty print for readability
            int index = 1;

            for (MachineRecipe recipe : recipes) {
                Path recipeFile = configRecipesDir.resolve(index + ".json"); // Generate file name
                String jsonContent = gson.toJson(recipe);

                // Write JSON to file
                Files.writeString(recipeFile, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Saved recipe to: " + recipeFile.getFileName());

                index++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save recipes", e);
        }
    }

}
