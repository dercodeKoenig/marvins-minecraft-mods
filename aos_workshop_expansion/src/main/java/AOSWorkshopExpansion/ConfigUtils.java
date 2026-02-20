package AOSWorkshopExpansion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static AOSWorkshopExpansion.Main.configDir;

public class ConfigUtils {
    public static <T> T loadConfig(Class<T> configClass, String configFileName) {
        T config = null;
        if (ServerLifecycleHooks.getCurrentServer() == null) return config;
        Path filePath = configDir.resolve(configFileName);
        if (!Files.exists(filePath)) {
            System.out.println("missing config file: " + filePath + " - configuration will reset");
            DataFiles.copyDataFiles("config/aos_workshop_expansion", configDir);
        }
        try {
            String jsonContent = Files.readString(filePath);
            Gson gson = new Gson();
            config = gson.fromJson(jsonContent, configClass);
        } catch (JsonSyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    public static <R> List<R> loadRecipes(Class<R> recipeClass, String recipesDir) {
        List<R> recipes = new ArrayList<>();
        if (ServerLifecycleHooks.getCurrentServer() == null) return recipes;
        Path configRecipesDir = configDir.resolve(recipesDir);
        DirectoryStream<Path> stream = null;
        try {
            stream = Files.newDirectoryStream(configRecipesDir, "*");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Path recipeFile : stream) {
            try {
                String recipeContent = Files.readString(recipeFile);
                Gson gson = new Gson();
                R recipe = gson.fromJson(recipeContent, recipeClass);
                recipes.add(recipe);
            } catch (JsonSyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return recipes;
    }
}
