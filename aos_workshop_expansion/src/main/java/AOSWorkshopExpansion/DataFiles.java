package AOSWorkshopExpansion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class DataFiles {
    public static void copyDataFiles(String dataPath, Path targetDir) {
        var server = ServerLifecycleHooks.getCurrentServer();
        var resourceManager = server.getResourceManager();

        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    dataPath,
                    path -> true // copy everything
            );

            for (var entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                Resource resource = entry.getValue();

                // Create Path objects for the search path and the full resource path
                Path dataPathAsPath = Path.of(dataPath);
                Path fullResourcePath = Path.of(location.getPath());
                // Calculate the path of the resource relative to the search path
                Path subPath = dataPathAsPath.relativize(fullResourcePath);
                // Resolve this correct sub-path against the target directory
                Path targetPath = targetDir.resolve(subPath.toString());
                System.out.println("copy " + fullResourcePath);
                System.out.println("to " + targetPath);

                // Ensure subdirs exist
                Files.createDirectories(targetPath.getParent());

                try (InputStream in = resource.open()) {
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied: " + targetPath);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to copy data files from " + dataPath, e);
        }
    }

}
